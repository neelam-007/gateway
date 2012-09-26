/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

import static com.l7tech.util.Functions.map;

/**
 * Resolver specific to the OAuth Toolkit Installer modular assertion. Loads bundles from jar resources.
 */
public class OAuthToolkitBundleResolver implements BundleResolver {

    public OAuthToolkitBundleResolver(final String bundleBaseName) throws InvalidBundleException, OAuthInstallerAdmin.OAuthToolkitInstallationException {
        final Map<String, String> guidMap = new HashMap<String, String>();

        final List<Pair<BundleInfo, String>> bundleInfosFromJar = BundleUtils.getBundleInfos(getClass(), bundleBaseName);
        for (Pair<BundleInfo, String> pair : bundleInfosFromJar) {
            guidMap.put(pair.left.getId(), pair.right);
        }

        resultList.clear();
        resultList.addAll(map(bundleInfosFromJar, new Functions.Unary<BundleInfo, Pair<BundleInfo, String>>() {
            @Override
            public BundleInfo call(Pair<BundleInfo, String> bundleInfoStringPair) {
                return bundleInfoStringPair.left;
            }
        }));
        guidToResourceDirectory.clear();
        guidToResourceDirectory.putAll(guidMap);
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

}
