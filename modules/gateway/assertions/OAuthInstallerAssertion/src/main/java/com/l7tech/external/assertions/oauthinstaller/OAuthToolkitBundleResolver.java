/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
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
 * Resolver specific to the OAuth Toolkit Installer modular assertion. Loads bundles from jar resources.
 */
public class OAuthToolkitBundleResolver implements BundleResolver {

    public OAuthToolkitBundleResolver(final List<Pair<BundleInfo, String>> bundleInfosFromJar) {
        final Map<String, String> guidMap = new HashMap<String, String>();

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
    }

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

    protected Document getItemFromBundle(String bundleId, BundleItem itemName) throws BundleResolver.UnknownBundleException,
            InvalidBundleException {

        if (!guidToResourceDirectory.containsKey(bundleId)) {
            throw new BundleResolver.UnknownBundleException("Unknown bundle id: " + bundleId);
        }

        final String resourceBase = guidToResourceDirectory.get(bundleId);
//        logger.info("Getting bundle: " + resourceBase);
        final URL itemUrl = this.getClass().getResource(resourceBase + itemName.getFileName());
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
            // rewrite the name of the OAuth folder
            final XpathResult xpathResult =
                    XpathUtil.getXpathResultQuietly(
                            new DomElementCursor(itemDocument), GatewayManagementDocumentUtilities.getNamespaceMap(), ".//l7:Name");

            final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
            final String newOAuthFolderName = "OAuth " + installationPrefix;
            boolean oauthNameFound = false;
            while (iterator.hasNext()) {
                final Element nameElement = iterator.nextElementAsCursor().asDomElement();
                final String nameValue = DomUtils.getTextValue(nameElement);
                if ("OAuth".equals(nameValue)) {

                    DomUtils.setTextContent(nameElement, newOAuthFolderName);
                    oauthNameFound = true;
                }
            }

            if (!oauthNameFound) {
                throw new InvalidBundleException("OAuth folder could not be found for update with installation prefix.");
            }
            logger.fine("Updated OAuth folder for installation prefix: '" + newOAuthFolderName + "'");
        }

        return itemDocument;
    }

    @NotNull
    @Override
    public List<BundleInfo> getResultList() {
        return Collections.unmodifiableList(resultList);
    }

    // - PRIVATE

    private final List<BundleInfo> resultList = new ArrayList<BundleInfo>();
    private final Map<String, String> guidToResourceDirectory = new HashMap<String, String>();
    private String installationPrefix;
    private static final Logger logger = Logger.getLogger(OAuthToolkitBundleResolver.class.getName());

}
