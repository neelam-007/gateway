package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

import static com.l7tech.util.Functions.map;

/**
 * Default bundle resolver which loads bundles from jar resources.
 */
public class BundleResolverImpl implements BundleResolver {
    private final List<BundleInfo> resultList = new ArrayList<>();
    private final Map<String, String> guidToResourceDirectory = new HashMap<>();
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

    @Override
    public Document getBundleItem(@NotNull String bundleId, @NotNull String prerequisiteFolder, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
        final Document itemFromBundle;
        try {
            itemFromBundle = getItemFromBundle(bundleId, prerequisiteFolder, bundleItem);
        } catch (InvalidBundleException e) {
            throw new BundleResolverException(e);
        }
        if (itemFromBundle == null && !allowMissing) {
            throw new UnknownBundleItemException("Unknown bundle item '" + bundleItem + "' requested from bundle '" + bundleId + "'");
        }
        return itemFromBundle;
    }

    @Override
    public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException, InvalidBundleException {
        return getBundleItem(bundleId, "", bundleItem, allowMissing);
    }


    protected Document getItemFromBundle(@NotNull String bundleId, @NotNull String prerequisiteFolder, @NotNull BundleItem itemName) throws UnknownBundleException, InvalidBundleException {

        if (!guidToResourceDirectory.containsKey(bundleId)) {
            throw new UnknownBundleException("Unknown bundle id: " + bundleId);
        }

        String directorySeparator = "/";
        String resourceBase = guidToResourceDirectory.get(bundleId);
        if (resourceBase.contains("\\")) {
            directorySeparator = "\\";
        }

        if (resourceBase.endsWith("/") || resourceBase.endsWith("\\")) {
            resourceBase = resourceBase.substring(0, resourceBase.length() - 1);
        }

        final String prerequisiteFolderPath = StringUtils.isEmpty(prerequisiteFolder) ?  "" : directorySeparator + prerequisiteFolder;
        final String fullResourceName = resourceBase + prerequisiteFolderPath + directorySeparator + itemName.getFileName();

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

        return itemDocument;
    }

    @NotNull
    @Override
    public List<BundleInfo> getResultList() {
        return Collections.unmodifiableList(resultList);
    }
}
