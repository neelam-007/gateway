package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.map;

/**
 * Default bundle resolver which loads bundles from jar resources.
 */
public class BundleResolverImpl implements BundleResolver {
    private static final String ROOT_FOLDER_NAME_LOWERCASE = "root";

    private final List<BundleInfo> resultList = new ArrayList<>();
    private final Map<String, String> guidToResourceDirectory = new HashMap<>();
    private String installationPrefix;
    private static final Logger logger = Logger.getLogger(BundleResolverImpl.class.getName());
    private final Class callingClass;

    public BundleResolverImpl(@NotNull final List<Pair<BundleInfo, String>> bundleInfosFromJar, @NotNull final Class callingClass) {
        final Map<String, String> guidMap = new HashMap<>();

        for (Pair<BundleInfo, String> pair : bundleInfosFromJar) {
            guidMap.put(pair.left.getId(), pair.right);
        }

        resultList.addAll(map(bundleInfosFromJar, new Functions.Unary<BundleInfo, Pair<BundleInfo, String>>() {
            @Override
            public BundleInfo call(Pair<BundleInfo, String> bundleInfoStringPair) {
                return bundleInfoStringPair.left;
            }
        }));
        guidToResourceDirectory.putAll(guidMap);

        this.callingClass = callingClass;
    }

    /**
     * Set the installation prefix.
     * @param installationPrefix installation prefix
     */
    public void setInstallationPrefix(@Nullable String installationPrefix) {
        this.installationPrefix = installationPrefix;
    }

    @Override
    public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
        final Document itemFromBundle;
        try {
            itemFromBundle = getItemFromBundle(bundleId, bundleItem);
        } catch (InvalidBundleException e) {
            throw new BundleResolverException(e);
        }
        if (itemFromBundle == null && !allowMissing) {
            throw new UnknownBundleItemException("Unknown bundle item '" + bundleItem + "' requested from bundle '" + bundleId + "'");
        }
        return itemFromBundle;
    }

    protected Document getItemFromBundle(@NotNull String bundleId, @NotNull BundleItem itemName) throws UnknownBundleException, InvalidBundleException {

        if (!guidToResourceDirectory.containsKey(bundleId)) {
            throw new UnknownBundleException("Unknown bundle id: " + bundleId);
        }

        final String resourceBase = guidToResourceDirectory.get(bundleId);
        final String fullResourceName;
        if (resourceBase.contains("\\") && !resourceBase.endsWith("\\")) {
            fullResourceName = resourceBase + "\\" + itemName.getFileName();
        } else if(resourceBase.contains("/") && !resourceBase.endsWith("/")){
            fullResourceName = resourceBase + "/" + itemName.getFileName();
        } else {
            // expected case e.g. /com/l7tech/external/assertions/oauthinstaller/bundles/OAuth_1_0/
            fullResourceName = resourceBase + itemName.getFileName();
        }

        final URL itemUrl = callingClass.getResource(fullResourceName);
        Document itemDocument = null;
        if (itemUrl != null) {
            try {
                final byte[] bytes = IOUtils.slurpUrl(itemUrl);
                itemDocument = XmlUtil.parse(new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                throw new InvalidBundleException(e);
            }
        }

        if (itemName == BundleItem.FOLDER && installationPrefix != null) {
            // rewrite the name of the folder
            final XpathResult xpathResult = XpathUtil.getXpathResultQuietly(
                    new DomElementCursor(itemDocument), GatewayManagementDocumentUtilities.getNamespaceMap(), ".//l7:Name");

            if (xpathResult.getType() != XpathResult.TYPE_NODESET) {
                throw new InvalidBundleException("Could not find folder name elements");
            }

            boolean atLeastOneFolderNameFound = false;
            final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
            while (iterator.hasNext()) {
                final Element nameElement = iterator.nextElementAsCursor().asDomElement();
                final String originalFolderName = DomUtils.getTextValue(nameElement);
                if (originalFolderName != null && !originalFolderName.toLowerCase().contains(ROOT_FOLDER_NAME_LOWERCASE)) {
                    String newFolderName = originalFolderName + " " + installationPrefix;
                    DomUtils.setTextContent(nameElement, newFolderName);
                    atLeastOneFolderNameFound = true;
                    logger.fine("Updated " + originalFolderName + " folder for installation prefix: '" + newFolderName + "'");
                }
            }

            if (!atLeastOneFolderNameFound) {
                throw new InvalidBundleException("No folder found for update with installation prefix.");
            }
        }

        return itemDocument;
    }

    @NotNull
    @Override
    public List<BundleInfo> getResultList() {
        return Collections.unmodifiableList(resultList);
    }
}
