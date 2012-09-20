package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.event.AdminInfo.find;

public class OAuthInstallerAdminImpl extends AsyncAdminMethodsImpl implements OAuthInstallerAdmin {

    public static final String NS_INSTALLER_VERSION = "http://ns.l7tech.com/2012/09/oauth-toolkit";
    private String oAuthInstallerVersion;

    @NotNull
    @Override
    public String getOAuthToolkitVersion() throws OAuthToolkitInstallationException {
        configureBundleInstaller();
        return oAuthInstallerVersion;
    }

    /**
     * All bundles in bundleNames MUST USE the same GUIDS for all policies which have the same name. The name of a policy
     * is unique on a Gateway. If the bundles contain the same policy with different guids the bundles will not install.
     *
     * @param otkComponentId names of all bundles to install. Bundles may depend on each others items, but there is no
     *                       install dependency order.
     * @param folderOid      oid of the folder to install into.
     * @param installFolder  if not null or empty, this folder will be the install into folder. It may already exist.
     *                       If it does not exist it will be created.
     * @return Job ID, which will report on which bundles were installed.
     * @throws IOException for any problem installing. Installation is cancelled on the first error.
     */
    @NotNull
    @Override
    public JobId<ArrayList> installOAuthToolkit(@NotNull final Collection<String> otkComponentId,
                                                final long folderOid,
                                                @Nullable final String installFolder) throws OAuthToolkitInstallationException {

        configureBundleInstaller();

        final FutureTask<ArrayList> future = new FutureTask<ArrayList>(find(false).wrapCallable(new Callable<ArrayList>() {
            @Override
            public ArrayList call() throws Exception {
                //todo check version of bundle to ensure it's supported.

                // When installing more than one bundle, allow for optimization of not trying to recreate items already created.
                final Map<String, Object> contextMap = new HashMap<String, Object>();

                final ArrayList<String> installedBundles = new ArrayList<String>();
                if (isInstallInProgress.compareAndSet(false, true)) {
                    try {
                        //iterate through all the bundle names to install
                        for (String bundleId : otkComponentId) {
                            //todo search for and updated jdbc references as needed
                            try {
                                if (Thread.currentThread().isInterrupted()) {
                                    logger.info("Installation cancelled");
                                    break;
                                }

                                bundleInstaller.doInstall(bundleId, folderOid, installFolder, contextMap);
                            } catch (Exception e) {
                                if (e instanceof InterruptedException) {
                                    logger.info("Installation cancelled");
                                    break;
                                }
                                //todo log and audit with stack trace
                                if (!(e instanceof BundleResolver.UnknownBundleException)) {
                                    // catch everything including any runtime errors representing programming errors e.g. NPEs
                                    logger.warning("Unexpected error during installation: " + ExceptionUtils.getMessage(e));
                                    throw e;
                                } else {
                                    throw e;
                                }

                            }
                            installedBundles.add(bundleId);
                        }

                    } finally {
                        isInstallInProgress.set(false);
                    }
                } else {
                    throw new OAuthToolkitInstallationException("Install is already in progress");
                }

                return installedBundles;
            }

        }));

        // Lets schedule this to run in a second, not straight away...want the UI to wait just a little
        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                future.run();
            }
        }, 1000L);

        return registerJob(future, ArrayList.class);
    }

    @NotNull
    @Override
    public List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException {
        configureBundleInstaller();
        return Collections.unmodifiableList(new ArrayList<BundleInfo>(resultList));
    }

    public void setBundleInstaller(BundleInstaller bundleInstaller) {
        this.bundleInstaller = bundleInstaller;
    }

    /**
     * Wired via OAuthInstallerAssertion meta data.
     *
     * @param context spring application context
     */
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        if (applicationContext != null) {
            logger.log(Level.WARNING, "OAuthInstaller module is already initialized");
        } else {
            applicationContext = context;
        }
    }

    // - PROTECTED

    protected void loadBundles() throws OAuthToolkitInstallationException, BundleInstaller.InvalidBundleException {

        final String bundleBaseName = "/com/l7tech/external/assertions/oauthinstaller/bundles/";
        final URL resource = getClass().getResource(bundleBaseName);

        final JarFile jarFile;
        final List<JarEntry> allBundleEntries = new ArrayList<JarEntry>();
        try {
            if (resource.toURI().toString().startsWith("jar:")) {
                final JarURLConnection urlConnection;
                try {
                    urlConnection = (JarURLConnection) resource.openConnection();
                    jarFile = urlConnection.getJarFile();
                } catch (IOException e) {
                    throw new OAuthToolkitInstallationException("Could not access jar resource: " + resource, e);
                }

                final Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    final JarEntry jarEntry = entries.nextElement();

                    final String jarEntryName = jarEntry.getName();
                    final String resourceName = bundleBaseName.substring(1);
                    if (jarEntryName.startsWith(resourceName) && jarEntry.isDirectory()) {
                        // must be one level down
                        if (jarEntryName.indexOf("/", resourceName.length() + 1) == jarEntryName.length() - 1) {
                            // we have found a directory which is a direct child of the bundle folder
                            logger.fine("Found bundle folder: " + jarEntryName);
                            allBundleEntries.add(jarEntry);
                        }
                    }
                }
            } else {
                throw new OAuthToolkitInstallationException("Could not find jar resource to open: " + bundleBaseName);
            }

        } catch (URISyntaxException e) {
            throw new OAuthToolkitInstallationException(e);
        }

        final String oauthBundleInfo = bundleBaseName + "OAuthToolkitBundleInfo.xml";
        final URL oauthBundleInfoUrl = getClass().getResource(oauthBundleInfo);
        if (oauthBundleInfoUrl == null) {
            throw new OAuthToolkitInstallationException("Could not find OAuthToolkitBundleInfo.xml");
        }

        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(oauthBundleInfoUrl);
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException(e);
        }

        final Document oauthInfoDoc;
        try {
            oauthInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (IOException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        } catch (SAXException e) {
            throw new OAuthToolkitInstallationException("Unable to parse resource: " + oauthBundleInfo, e);
        }

        try {
            final Element versionElm = XmlUtil.findExactlyOneChildElementByName(oauthInfoDoc.getDocumentElement(), NS_INSTALLER_VERSION, "Version");
            oAuthInstallerVersion = DomUtils.getTextValue(versionElm, true);
            if (oAuthInstallerVersion.isEmpty()) {
                throw new OAuthToolkitInstallationException("Could not get version information for OAuth Toolkit");
            }
        } catch (Exception e) {
            throw new OAuthToolkitInstallationException("Could not find version information in: " + oauthBundleInfo, e);
        }

        final ArrayList<BundleInfo> bundleSummary = new ArrayList<BundleInfo>();
        final Map<String, String> guidMap = new HashMap<String, String>();
        if (jarFile != null) {
            for (JarEntry bundleDir : allBundleEntries) {
                final String resourceBase = "/" + bundleDir.getName();
                final String resourceName = resourceBase + "BundleInfo.xml";
                final URL bundleInfoXml = getClass().getResource(resourceName);
                final byte[] bytes;
                try {
                    bytes = IOUtils.slurpUrl(bundleInfoXml);
                } catch (IOException e) {
                    throw new OAuthToolkitInstallationException("Unable to read BundleInfo.xml from bundle " + resourceBase, e);
                }

                final Document bundleInfoDoc;
                try {
                    bundleInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bytes));
                } catch (IOException e) {
                    throw new OAuthToolkitInstallationException("Unable to parse BundleInfo.xml from bundle " + resourceBase, e);
                } catch (SAXException e) {
                    throw new OAuthToolkitInstallationException("Unable to parse BundleInfo.xml from bundle " + resourceBase, e);
                }

                final BundleInfo bundleInfo = BundleUtils.getBundleInfo(bundleInfoDoc);
                bundleSummary.add(bundleInfo);
                guidMap.put(bundleInfo.getId(), resourceBase);
            }
        }

        resultList.clear();
        resultList.addAll(bundleSummary);
        guidToResourceDirectory.clear();
        guidToResourceDirectory.putAll(guidMap);
    }

    // - PRIVATE

    private static ApplicationContext applicationContext;
    private final AtomicBoolean isInstallInProgress = new AtomicBoolean(false);
    private static final Logger logger = Logger.getLogger(OAuthInstallerAdminImpl.class.getName());

    /**
     * Loads all OTK bundle resources from jar.
     * <p/>
     * Double checked locked configuration method.
     *
     * @throws OAuthToolkitInstallationException
     *
     */
    private void configureBundleInstaller() throws OAuthToolkitInstallationException {
        if (bundleInstaller == null) {
            synchronized (this) {
                if (bundleInstaller == null) {
                    try {
                        loadBundles();
                    } catch (BundleInstaller.InvalidBundleException e) {
                        throw new OAuthToolkitInstallationException("Could not load OTK components: " + ExceptionUtils.getMessage(e));
                    }
                    bundleInstaller = new BundleInstaller(new BundleResolver() {
                        @Nullable
                        @Override
                        public Document getBundleItem(@NotNull String bundleId, @NotNull String bundleItem, boolean allowMissing)
                                throws UnknownBundleException, BundleResolverException {
                            final Document itemFromBundle;
                            try {
                                itemFromBundle = getItemFromBundle(bundleId, bundleItem);
                            } catch (IOException e) {
                                //todo log and audit with stack trace
                                throw new BundleResolverException(e);
                            } catch (SAXException e) {
                                //todo log and audit with stack trace
                                throw new BundleResolverException(e);
                            }
                            if (itemFromBundle == null && !allowMissing) {
                                throw new UnknownBundleItemException("Unknown bundle item '" + bundleItem + "' requested from bundle '" + bundleId + "'");
                            }
                            return itemFromBundle;
                        }
                    });
                    bundleInstaller.setApplicationContext(applicationContext);
                }
            }
        }
    }

    /**
     * A bundle item may not exist
     *
     * @param bundleId
     * @param itemName
     * @return
     * @throws IOException
     * @throws SAXException
     */
    @Nullable
    protected Document getItemFromBundle(String bundleId, String itemName) throws BundleResolver.UnknownBundleException, IOException, SAXException {

        if (!guidToResourceDirectory.containsKey(bundleId)) {
            throw new BundleResolver.UnknownBundleException("Unknown bundle id: " + bundleId);
        }

        final String resourceBase = guidToResourceDirectory.get(bundleId);
        logger.info("Getting bundle: " + resourceBase);
        final URL itemUrl = this.getClass().getResource(resourceBase + itemName);
        Document itemDocument = null;
        if (itemUrl != null) {
            final byte[] bytes = IOUtils.slurpUrl(itemUrl);
            itemDocument = XmlUtil.parse(new ByteArrayInputStream(bytes));
        }

        return itemDocument;
    }

    // - PRIVATE

    private final List<BundleInfo> resultList = new ArrayList<BundleInfo>();
    private final Map<String, String> guidToResourceDirectory = new HashMap<String, String>();
    private BundleInstaller bundleInstaller;

}
