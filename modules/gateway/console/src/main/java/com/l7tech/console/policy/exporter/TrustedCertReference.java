package com.l7tech.console.policy.exporter;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: Mike
 */
public class TrustedCertReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(TrustedCertReference.class.getName());


    public static final String REF_EL_NAME = "TrustedCertificateReference";
    public static final String OID_EL_NAME = "OID";
    public static final String CERTNAME_EL_NAME = "CertificateName";
    public static final String ISSUERDN_EL_NAME = "CertificateIssuerDn";
    public static final String CERTSERIAL_EL_NAME = "CertificateSerialNum";

    private long oid;
    private String certName;
    private String  certIssuerDn;
    private BigInteger certSerial;
    private LocaliseAction localizeType = null;
    private long localCertOid;

    public TrustedCertReference() {
    }

    public TrustedCertReference(long certOid) {
        oid = certOid;
        TrustedCertAdmin admin = Registry.getDefault().getTrustedCertManager();
        TrustedCert cert = null;
        try {
            cert = admin.findCertByPrimaryKey(certOid);
            if (cert != null) {
                certName = cert.getName();
                certIssuerDn = cert.getCertificate().getIssuerDN().getName();
                certSerial = cert.getCertificate().getSerialNumber();
            }
        } catch (FindException e){
            logger.log(Level.SEVERE, "error retrieving trusted cert information. ", e);
            cert = null;
        }
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, TrustedCertReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = XmlUtil.createTextNode(referencesParentElement, Long.toString(oid));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        Element certNameEl = referencesParentElement.getOwnerDocument().createElement(CERTNAME_EL_NAME);
        refEl.appendChild(certNameEl);
        Element issuerDnEl = referencesParentElement.getOwnerDocument().createElement(ISSUERDN_EL_NAME);
        refEl.appendChild(issuerDnEl);
        Element certSerialEl = referencesParentElement.getOwnerDocument().createElement(CERTSERIAL_EL_NAME);
        refEl.appendChild(certSerialEl);
        if (certName != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, certName);
            certNameEl.appendChild(txt);
        }
        if (certIssuerDn != null) {
            txt = XmlUtil.createTextNode(referencesParentElement, certIssuerDn);
            issuerDnEl.appendChild(txt);
        }
        if (certSerial!= null) {
            txt = XmlUtil.createTextNode(referencesParentElement, certSerial.toString());
            certSerialEl.appendChild(txt);
        }
    }

    boolean verifyReference() throws InvalidPolicyStreamException {
        Collection<TrustedCert> tempMatches = new ArrayList<TrustedCert>();
        TrustedCertAdmin admin = Registry.getDefault().getTrustedCertManager();
        Collection<TrustedCert> allCerts = null;
        try {
            allCerts = admin.findAllCerts();
        } catch (FindException e) {
            throw new RuntimeException("Unable to look up trusted certs. ", e);
        }

        for (TrustedCert oneCert : allCerts) {
            if (oneCert.getCertificate().getIssuerDN().getName().equals(certIssuerDn) &&
                    oneCert.getCertificate().getSerialNumber().equals(certSerial)) {
                    tempMatches.add(oneCert);
            }
        }

        if (tempMatches.isEmpty()) {
            logger.warning("The trusted certificate cannot be resolved.");
            return false;
        } else {
            // Try to discriminate using name property
            for (TrustedCert aMatch : tempMatches) {
                if (aMatch.getName().equals(certName)) {
                    // WE HAVE A PERFECT MATCH!
                    logger.fine("The local trusted certificate was resolved from oid " + oid + " to " + aMatch.getOid());
                    localCertOid = aMatch.getOid();
                    localizeType = LocaliseAction.REPLACE;
                    return true;
                }
            }
            // Otherwise, use first partial match
            TrustedCert firstMatch = tempMatches.iterator().next();
            logger.fine("The local trusted cert was resolved from oid " + oid + " to " + firstMatch.getOid());
            localCertOid = firstMatch.getOid();
            localizeType = LocaliseAction.REPLACE;
            return true;
        }
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocaliseAction.IGNORE) {
            if (assertionToLocalize instanceof BridgeRoutingAssertion) {
                BridgeRoutingAssertion bra = (BridgeRoutingAssertion) assertionToLocalize;
                if (bra.getServerCertificateOid() != null &&
                    bra.getServerCertificateOid().longValue() == oid) {
                    if (localizeType == LocaliseAction.REPLACE) {
                        // replace server cert oid
                        bra.setServerCertificateOid(new Long(localCertOid));
                        logger.info("The server certificate oid of the imported bridge routing assertion has been changed " +
                                    "from " + oid + " to " + localCertOid);
                    } else if (localizeType == LocaliseAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            } else if ( assertionToLocalize instanceof UsesEntities ) {
                UsesEntities entitiesUser = (UsesEntities)assertionToLocalize;
                for(EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                    if(entityHeader.getType().equals(EntityType.TRUSTED_CERT) && entityHeader.getOid() == oid) {
                        if(localizeType == LocaliseAction.REPLACE) {
                            if(localCertOid != oid) {
                                EntityHeader newEntityHeader = new EntityHeader(localCertOid, EntityType.TRUSTED_CERT, null, null);
                                entitiesUser.replaceEntity(entityHeader, newEntityHeader);
                                logger.info("The server certificate oid of the imported assertion has been changed " +
                                            "from " + oid + " to " + localCertOid);
                                break;
                            }
                        } else if(localizeType == LocaliseAction.DELETE) {
                            logger.info("Deleted this assertion from the tree.");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static TrustedCertReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        TrustedCertReference output = new TrustedCertReference();
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.oid = Long.parseLong(val);
        }

        output.certName = getParamFromEl(el, CERTNAME_EL_NAME);
        output.certIssuerDn = getParamFromEl(el, ISSUERDN_EL_NAME);
        String serialValue = getParamFromEl(el, CERTSERIAL_EL_NAME);
        if ( serialValue != null ) {
            output.certSerial = new BigInteger( serialValue );
        }
        return output;
    }

    public long getOid() {
        return oid;
    }

    public String getCertName() {
        return certName;
    }

    public String getCertIssuerDn() {
        return certIssuerDn;
    }

    public BigInteger getCertSerial() {
        return certSerial;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public void setCertName(String certName) {
        this.certName = certName;
    }

    public void setCertIssuerDn(String certIssuerDn) {
        this.certIssuerDn = certIssuerDn;
    }

    public void setCertSerial(BigInteger certSerial) {
        this.certSerial = certSerial;
    }

    public void setLocalizeReplace(long newCertOid) {
        localizeType = LocaliseAction.REPLACE;
        localCertOid = newCertOid;
    }

    public void setLocalizeDelete() {
        localizeType = LocaliseAction.DELETE;
    }

    public void setLocalizeIgnore() {
        localizeType = LocaliseAction.IGNORE;
    }

    public static class LocaliseAction {
        public static final LocaliseAction IGNORE = new LocaliseAction(1);
        public static final LocaliseAction DELETE = new LocaliseAction(2);
        public static final LocaliseAction REPLACE = new LocaliseAction(3);
        private LocaliseAction(int val) {
            this.val = val;
        }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocaliseAction)) return false;

            final LocaliseAction localiseAction = (LocaliseAction) o;

            if (val != localiseAction.val) return false;

            return true;
        }

        public int hashCode() {
            return val;
        }

        private int val = 0;
    }
}
