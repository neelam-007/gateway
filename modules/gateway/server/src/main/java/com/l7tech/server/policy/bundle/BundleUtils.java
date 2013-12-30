package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.POLICY;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.SERVICE;
import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;
import static com.l7tech.server.policy.bundle.PolicyUtils.findJdbcReferences;

/**
 * Utilities related to processing Bundle related documents e.g. BundleInfo.xml
 */
public class BundleUtils {

    public static final String NS_BUNDLE = "http://ns.l7tech.com/2012/09/policy-bundle";
    public static final String L7_NS_POLICY = "http://www.layer7tech.com/ws/policy";
    public static final String L7_NS_GW_MGMT = "http://ns.l7tech.com/2010/04/gateway-management";

    /**
     * Get all bundles available at the base resource path.
     *
     * Note at runtime resources will be loaded via Jar URL Connections however at test time they are loaded from files.
     * This method is provided as a convenience to deal with both scenarios.
     *
     * @param callingClass Class whose classloader will be used to load resources.
     * @param bundleResourceBaseName the directory resource containing bundle folders e.g. /com/l7tech/server/policy/bundle/bundles/
     * @return list of bundle infos.
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    @NotNull
    public static List<Pair<BundleInfo, String>> getBundleInfos(@NotNull final Class callingClass, @NotNull final String bundleResourceBaseName) throws InvalidBundleException {
        final List<Pair<BundleInfo, String>> bundleInfos = new ArrayList<>();

        try {
            final JarFile jarFile = getJarFileForResource(callingClass, bundleResourceBaseName);
            bundleInfos.addAll(getBundleInfos(callingClass, bundleResourceBaseName, jarFile));
        } catch (IOException e) {
            throw new InvalidBundleException(e);
        }

        return bundleInfos;
    }

    /**
     * Get all bundles available at the base resource path.
     *
     * Note at runtime resources will be loaded via Jar URL Connections however at test time they are loaded from files.
     * This method is provided as a convenience to deal with both scenarios.
     *
     * @param callingClass Class whose classloader will be used to load resources.
     * @param bundleResourceBaseName the directory resource containing bundle folders e.g. /com/l7tech/server/policy/bundle/bundles/
     * @param jarFile the JAR file containing the bundle resources
     * @return list of bundle infos.
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    @NotNull
    public static List<Pair<BundleInfo, String>> getBundleInfos(@NotNull final Class callingClass, @NotNull final String bundleResourceBaseName, @Nullable final JarFile jarFile) throws InvalidBundleException {
        final List<Pair<BundleInfo, String>> bundleInfos = new ArrayList<>();
        try {
            if (jarFile != null) {
                final List<JarEntry> allBundleEntries = findAllBundleEntries(jarFile, bundleResourceBaseName);
                final List<Pair<BundleInfo, String>> bundleInfosFromJar = getBundleInfosFromJar(allBundleEntries, callingClass);
                bundleInfos.addAll(bundleInfosFromJar);
            } else {
                // work with files for test cases
                final URL resource = callingClass.getResource(bundleResourceBaseName);
                final File bundleFolderParent = new File(resource.getFile());
                if (bundleFolderParent.isDirectory()) {
                    final File[] files = bundleFolderParent.listFiles();
                    if (files != null) {
                        for (File bundleFolder : files) {
                            if (bundleFolder.isDirectory()) {
                                final File bundleInfo = new File(bundleFolder, "BundleInfo.xml");
                                final byte[] bytes = IOUtils.slurpFile(bundleInfo);
                                final Document parse;
                                try {
                                    parse = XmlUtil.parse(new ByteArrayInputStream(bytes));
                                } catch (SAXException e) {
                                    throw new InvalidBundleException(e);
                                }
                                final BundleInfo bundleInfo1 = getBundleInfo(parse);
                                // regardless of how the resource was found, only record a correct resource path to the resource
                                bundleInfos.add(new Pair<>(bundleInfo1, bundleResourceBaseName + "/" + bundleFolder.getName()));
                            }
                        }
                    }
                } else {
                    throw new InvalidBundleException("Could not access file resources for bundle");
                }
            }
        } catch (IOException e) {
            throw new InvalidBundleException(e);
        }

        return bundleInfos;
    }

    /**
     * Given a Document which represents a BundleInfo.xml, convert it into a BundleInfo object.
     *
     * @param bundleInfoDoc document to extract values from
     * @return BundleInfo with values from the document
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException if the document could not be processed
     */
    public static BundleInfo getBundleInfo(Document bundleInfoDoc) throws InvalidBundleException {
        final String namespaceURI = bundleInfoDoc.getDocumentElement().getNamespaceURI();
        if (!NS_BUNDLE.equals(namespaceURI)) {
            throw new InvalidBundleException("Unsupported BundleInfo.xml version: " + namespaceURI);
        }

        final Element bundleIdEl;
        final Element versionEl;
        final Element nameEl;
        final Element descEl;
        try {
            bundleIdEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Id");
            versionEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Version");
            nameEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Name");
            descEl = XmlUtil.findExactlyOneChildElementByName(bundleInfoDoc.getDocumentElement(), NS_BUNDLE, "Description");
        } catch (TooManyChildElementsException | MissingRequiredElementException e) {
            throw new InvalidBundleException(e);
        }
        final String bundleId = DomUtils.getTextValue(bundleIdEl, true);
        final String version = DomUtils.getTextValue(versionEl, true);
        final String bundleName = DomUtils.getTextValue(nameEl, true);
        final String bundleDesc = DomUtils.getTextValue(descEl, true);
        if (bundleId.isEmpty() || bundleName.isEmpty() || bundleDesc.isEmpty()) {
            throw new InvalidBundleException("Invalid bundle declaration. Id, Name and Description must all be non empty");
        }

        return new BundleInfo(bundleId, version, bundleName, bundleDesc);
    }

    /**
     * Get a jar file representing the resouces available at resourceBaseName
     *
     * @param callingClass class whose class loader should be used.
     * @param resourceBaseName directory resource
     * @return JarFile if found
     * @throws java.io.IOException if the JAR resource cannot be opened.
     */
    @Nullable
    public static JarFile getJarFileForResource(Class callingClass, String resourceBaseName) throws IOException {
        JarFile returnFile = null;
        final URL resource = callingClass.getResource(resourceBaseName);
        if (resource != null) {
            final URLConnection urlConnection = resource.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
                returnFile = jarURLConnection.getJarFile();
            }
        }

        return returnFile;
    }

    /**
     * Get all JarEntry objects representing directories under bundleResourceBaseName
     *
     * Each JarEntry returned is a bundle directory which contains the contents of a bundle.
     *
     * @param bundleResourceBaseName the base name of the resource directory to search for bundle directories within.
     *                               The value must beginw with a '/' and end with a '/'
     *                               e.g. /com/l7tech/external/assertions/oauthinstaller/bundles/
     * @return List of all JarEntry objects found.
     */
    @NotNull
    public static List<JarEntry> findAllBundleEntries(@NotNull final JarFile jarFile,
                                                      @NotNull final String bundleResourceBaseName) throws InvalidBundleException {

        final List<JarEntry> allBundleEntries = new ArrayList<>();
        final Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            final JarEntry jarEntry = entries.nextElement();

            final String jarEntryName = jarEntry.getName();
            final String resourceName = bundleResourceBaseName.substring(1);
            if (jarEntryName.startsWith(resourceName) && jarEntry.isDirectory()) {
                // must be one level down
                if (jarEntryName.indexOf("/", resourceName.length() + 1) == jarEntryName.length() - 1) {
                    // we have found a directory which is a direct child of the bundle folder
                    logger.fine("Found bundle folder: " + jarEntryName);
                    allBundleEntries.add(jarEntry);
                }
            }
        }

        return allBundleEntries;
    }

    /**
     * Given a list of JarEntries which represent directories, extract and parse BundleInfo.xml and return the list of
     * Paris of found BundleInfo objects and the resource directory they were loaded from..
     *
     * @param bundleJarDirectories directories which contain bundles
     * @param callingClass class loader to use
     * @return list of found Pair BundleInfo plus path of the resource folder.
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    public static List<Pair<BundleInfo, String>> getBundleInfosFromJar(@NotNull final List<JarEntry> bundleJarDirectories, @NotNull final Class callingClass)
            throws InvalidBundleException {
        final List<Pair<BundleInfo, String>> bundleInfos = new ArrayList<>();
        for (JarEntry bundleDir : bundleJarDirectories) {
            final String resourceBase = "/" + bundleDir.getName();
            final String resourceName = resourceBase + "BundleInfo.xml";
            final URL bundleInfoXml = callingClass.getResource(resourceName);
            final byte[] bytes;
            try {
                bytes = IOUtils.slurpUrl(bundleInfoXml);
            } catch (IOException e) {
                throw new InvalidBundleException("Unable to read BundleInfo.xml from bundle " + resourceBase, e);
            }

            final Document bundleInfoDoc;
            try {
                bundleInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bytes));
            } catch (IOException | SAXException e) {
                throw new InvalidBundleException("Unable to parse BundleInfo.xml from bundle " + resourceBase, e);
            }

            final BundleInfo bundleInfo = BundleUtils.getBundleInfo(bundleInfoDoc);
            bundleInfos.add(new Pair<>(bundleInfo, resourceBase));
        }

        return bundleInfos;
    }

    /**
     * Look for dependency references that are supported and update them in the BundleInfo
     *
     * Currently only JDBC references are supported.
     * @param bundleInfo the BundleInfo to update with any found references in that bundle
     * @param bundleResolver the resolver to use to find the bundle items to search within for references. It will look
     *                       for Policy.xml and Service.xml within the bundle.
     */
    public static void findReferences(final BundleInfo bundleInfo, final BundleResolver bundleResolver) throws BundleResolverException, UnknownBundleException, InvalidBundleException {
        final String id = bundleInfo.getId();
        final Document policyBundleItem = bundleResolver.getBundleItem(id, POLICY, true);
        if (policyBundleItem != null) {
            final List<Element> policyElms = getEntityElements(policyBundleItem.getDocumentElement(), "Policy");
            for (Element policyElm : policyElms) {
                final Element policyResourceElement = getPolicyResourceElement(policyElm, "Policy", getPolicyGuid(policyElm));
                if (policyResourceElement != null) {
                    final Set<String> jdbcConnsFound = searchForJdbcReferences(policyResourceElement, "Policy", getPolicyGuid(policyElm));
                    for (String conn : jdbcConnsFound) {
                        bundleInfo.addJdbcReference(conn);
                    }
                }
            }
        }

        final Document serviceBundleItem = bundleResolver.getBundleItem(id, SERVICE, true);
        if (serviceBundleItem != null) {
            final List<Element> serviceElms = getEntityElements(serviceBundleItem.getDocumentElement(), "Service");
            for (Element serviceElm : serviceElms) {
                final Element policyResourceElement = getPolicyResourceElement(serviceElm, "Service", getId(serviceElm));
                if (policyResourceElement != null) {
                    final Set<String> jdbcConnsFound = searchForJdbcReferences(policyResourceElement, "Service", getId(serviceElm));
                    for (String conn : jdbcConnsFound) {
                        bundleInfo.addJdbcReference(conn);
                    }
                }
            }
        }
    }

    /**
     * Find all JDBC references in a Layer7 policy document.
     * @param policyResourceElement Layer7 policy elemnet
     * @param entityType type for logging
     * @param identifier identifier for logging
     * @return set of found jdbc references
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException if searching for references using xpath threw any exceptions
     */
    public static Set<String> searchForJdbcReferences(final Element policyResourceElement, final String entityType, final String identifier) throws InvalidBundleException {
        final Set<String> returnList = new HashSet<>();
        if (policyResourceElement != null) {
            final Document layer7PolicyXml = getPolicyDocumentFromResource(policyResourceElement, entityType, identifier);
            final List<Element> jdbcReferences = findJdbcReferences(layer7PolicyXml.getDocumentElement());
            for (Element jdbcReference : jdbcReferences) {
                try {
                    final Element connNameElm = XmlUtil.findExactlyOneChildElementByName(jdbcReference, BundleUtils.L7_NS_POLICY, "ConnectionName");
                    final String connName = connNameElm.getAttribute("stringValue").trim();
                    returnList.add(connName);
                } catch (TooManyChildElementsException | MissingRequiredElementException e) {
                    throw new InvalidBundleException("Could not find jdbc references: " + ExceptionUtils.getMessage(e));
                }
            }
        }

        return returnList;
    }


    @NotNull
    public static String getPolicyGuid(Element policyElm) {
        return policyElm.getAttribute("guid");
    }

    // - PRIVATE
    private static final Logger logger = Logger.getLogger(BundleUtils.class.getName());

    private static String getId(Element serviceElm) {
        return serviceElm.getAttribute("id");
    }

}
