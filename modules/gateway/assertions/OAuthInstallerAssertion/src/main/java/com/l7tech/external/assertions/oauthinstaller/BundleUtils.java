package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Utility to work with BundleInfo.xml
 */
public class BundleUtils {

    public static final String NS_BUNDLE = "http://ns.l7tech.com/2012/09/policy-bundle";

    public static BundleInfo getBundleInfo(Document bundleInfoDoc) throws BundleInstaller.InvalidBundleException {
        final String namespaceURI = bundleInfoDoc.getDocumentElement().getNamespaceURI();
        if (!NS_BUNDLE.equals(namespaceURI)) {
            throw new BundleInstaller.InvalidBundleException("Unsupported BundleInfo.xml version: " + namespaceURI);
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
        } catch (TooManyChildElementsException e) {
            throw new BundleInstaller.InvalidBundleException(e);
        } catch (MissingRequiredElementException e) {
            throw new BundleInstaller.InvalidBundleException(e);
        }
        final String bundleId = DomUtils.getTextValue(bundleIdEl, true);
        final String version = DomUtils.getTextValue(versionEl, true);
        final String bundleName = DomUtils.getTextValue(nameEl, true);
        final String bundleDesc = DomUtils.getTextValue(descEl, true);
        if (bundleId.isEmpty() || bundleName.isEmpty() || bundleDesc.isEmpty()) {
            throw new BundleInstaller.InvalidBundleException("Invalid bundle declaration. Id, Name and Description must all be non empty");
        }

        return new BundleInfo(bundleId, version, bundleName, bundleDesc);
    }

}
