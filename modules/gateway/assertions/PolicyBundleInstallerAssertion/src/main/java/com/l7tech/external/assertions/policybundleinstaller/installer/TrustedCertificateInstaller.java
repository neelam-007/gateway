package com.l7tech.external.assertions.policybundleinstaller.installer;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.GatewayManagementInvoker;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.CERTIFICATE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;

/**
 * Install trusted certificate.
 */
public class TrustedCertificateInstaller extends BaseInstaller {
    public static final String CERTIFICATE_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/trustedCertificates";

    public TrustedCertificateInstaller(@NotNull final PolicyBundleInstallerContext context,
                                       @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                                       @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document certificateEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), CERTIFICATE, true);
        final Map<Element, Element> certificateMap = findCertificateSerialNumbersAndNamesFromEnumeration(certificateEnumDoc);
        for (Element certSerialNumElm : certificateMap.keySet()) {
            checkInterrupted();
            final String serialNumber = XmlUtil.getTextValue(certSerialNumElm);
            try {
                final List<Goid> matchingPolicies = findMatchingCertificateBySerialNumber(serialNumber);
                if (!matchingPolicies.isEmpty()) {
                    dryRunEvent.addCertificateNameWithConflict(XmlUtil.getTextValue(certificateMap.get(certSerialNumElm)));
                }
            } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for certificate serial number  '" + serialNumber + "'", e);
            }
        }
    }

    public void install() throws InterruptedException, BundleResolver.UnknownBundleException, BundleResolver.BundleResolverException, BundleResolver.InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        checkInterrupted();
        final Document certificateBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), CERTIFICATE, true);
        if (certificateBundle == null) {
            logger.info("No certificate to install for bundle " + context.getBundleInfo());
        } else {
            installTrustedCertificate(certificateBundle);
        }
    }

    /**
     * Install all trusted certificates defined by a given certificate enumeration document.
     * Before installing a certificate, check if the certificate already exists.  If so, skip it, otherwise create it.
     *
     * @param trustedCertEnumeration: the Document object defines all trusted certificates.
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void installTrustedCertificate(final Document trustedCertEnumeration) throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final List<Element> certificateElms = GatewayManagementDocumentUtilities.getEntityElements(trustedCertEnumeration.getDocumentElement(), "TrustedCertificate");
        for (Element certificateElm : certificateElms) {
            checkInterrupted();

            final Element certSerialNumElm = XmlUtil.findFirstDescendantElement(certificateElm, MGMT_VERSION_NAMESPACE, "SerialNumber");
            final Element certificateNameElm = XmlUtil.findFirstDescendantElement(certificateElm, MGMT_VERSION_NAMESPACE, "Name");

            final String serialNumber = XmlUtil.getTextValue(certSerialNumElm);
            final List<Goid> matchingPolicies = findMatchingCertificateBySerialNumber(serialNumber);

            // If a certificate already exists, skip certificate creation and check the next candidate
            if (! matchingPolicies.isEmpty()) {
                logger.info("Not installing a trusted certificate with name '" + DomUtils.getTextValue(certificateNameElm) + "', due to existing certificate with conflicting Thumbprint SHA1");
                continue;
            }

            // If no conflict occurs, then create the certificate
            final String certificateXmlTemplate = XmlUtil.nodeToStringQuiet(certificateElm);
            final String createCertificateXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), CERTIFICATE_MGMT_NS, certificateXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createCertificateXml);

            final Goid createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
            if (createdId == null) {
                throw new GatewayManagementDocumentUtilities.UnexpectedManagementResponse("Could not create the certificate from the bundle: " + GatewayManagementDocumentUtilities.getErrorDetails(pair.right));
            }
        }
    }

    @NotNull
    private List<Goid> findMatchingCertificateBySerialNumber(String certificateSerialNumber) throws InterruptedException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final String certificateFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                CERTIFICATE_MGMT_NS, 10, "/l7:TrustedCertificate/l7:CertificateData/l7:SerialNumber[text()='" + certificateSerialNumber + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(certificateFilter);
        return GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
    }

    /**
     * Retrieve serial number and name for each trusted certificates defined in a certificate enumeration document.
     *
     * @param certificateEnumeration: a Document object defines all certificates.
     * @return a map in which each pair is (SerialNumber element, Name element).  The map could be empty, but not null.
     */
    @NotNull
    protected static Map<Element, Element> findCertificateSerialNumbersAndNamesFromEnumeration(final Document certificateEnumeration) {
        Map<Element, Element> certificatesMap = new HashMap<>();
        if (certificateEnumeration == null) return certificatesMap;

        for (Element trustedCertElmt: XpathUtil.findElements(certificateEnumeration.getDocumentElement(), ".//l7:TrustedCertificate", GatewayManagementDocumentUtilities.getNamespaceMap())) {
            List<Element> nameElmts = XpathUtil.findElements(trustedCertElmt, ".//l7:Name", GatewayManagementDocumentUtilities.getNamespaceMap());
            List<Element> serialNumElmts = XpathUtil.findElements(trustedCertElmt, ".//l7:SerialNumber", GatewayManagementDocumentUtilities.getNamespaceMap());

            if (nameElmts.size() != 1) {
                throw new IllegalArgumentException("Certificate xml does not contain valid Name element in TrustedCertificate element.");
            }
            if (serialNumElmts.size() != 1) {
                throw new IllegalArgumentException("Certificate xml does not contain valid SerialNumber element in TrustedCertificate element.");
            }

            certificatesMap.put(serialNumElmts.get(0), nameElmts.get(0));
        }
        return certificatesMap;
    }
}
