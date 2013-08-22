package com.l7tech.policy.exporter;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.math.BigInteger;
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
    public static final String OLD_OID_EL_NAME = "OID";
    public static final String GOID_EL_NAME = "GOID";
    public static final String CERTNAME_EL_NAME = "CertificateName";
    public static final String ISSUERDN_EL_NAME = "CertificateIssuerDn";
    public static final String CERTSERIAL_EL_NAME = "CertificateSerialNum";

    private Goid goid;
    private String certName;
    private String  certIssuerDn;
    private BigInteger certSerial;
    private LocalizeAction localizeType = null;
    private Goid localCertId;

    public TrustedCertReference( final ExternalReferenceFinder finder ) {
        super(finder);
    }

    public TrustedCertReference( final ExternalReferenceFinder finder,
                                 final Goid certId ) {
        this( finder );
        TrustedCert cert;
        try {
            cert = finder.findCertByPrimaryKey(certId);
            if (cert != null) {
                goid = cert.getGoid();
                certName = cert.getName();
                certIssuerDn = cert.getCertificate().getIssuerDN().getName();
                certSerial = cert.getCertificate().getSerialNumber();
            }
        } catch (FindException e){
            logger.log(Level.SEVERE, "error retrieving trusted cert information. ", e);
        }
    }

    @Override
    public String getRefId() {
        String id = null;

        if ( !goid.equals(TrustedCert.DEFAULT_GOID)) {
            id = goid.toString();
        }

        return id;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute(refEl);
        referencesParentElement.appendChild(refEl);

        addElement( refEl, GOID_EL_NAME, goid == null ? null : goid.toString() );
        addElement( refEl, CERTNAME_EL_NAME, certName );
        addElement( refEl, ISSUERDN_EL_NAME, certIssuerDn );
        addElement( refEl, CERTSERIAL_EL_NAME, certSerial == null ? null : certSerial.toString() );
    }

    private void addElement( final Element parent,
                             final String childElementName,
                             final String text ) {
        Element childElement = parent.getOwnerDocument().createElement( childElementName );
        parent.appendChild(childElement);

        if ( text != null ) {
            Text textNode = DomUtils.createTextNode(parent, text);
            childElement.appendChild( textNode );
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        Collection<TrustedCert> tempMatches = new ArrayList<>();
        Collection<TrustedCert> allCerts;
        try {
            allCerts = getFinder().findAllCerts();
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
        } else {
            // Try to discriminate using name property
            for (TrustedCert aMatch : tempMatches) {
                if (aMatch.getName().equals(certName) && permitMapping( goid, aMatch.getGoid() )) {
                    // WE HAVE A PERFECT MATCH!
                    logger.fine("The local trusted certificate was resolved from goid " + goid + " to " + aMatch.getGoid());
                    localCertId = aMatch.getGoid();
                    localizeType = LocalizeAction.REPLACE;
                    return true;
                }
            }
            // Otherwise, use a partial match
            for ( TrustedCert aMatch : tempMatches ) {
                if ( permitMapping( goid, aMatch.getGoid() ) ) {
                    logger.fine("The local trusted cert was resolved from goid " + goid + " to " +  aMatch.getGoid());
                    localCertId = aMatch.getGoid();
                    localizeType = LocalizeAction.REPLACE;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean localizeAssertion(final @Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof BridgeRoutingAssertion) {
                BridgeRoutingAssertion bra = (BridgeRoutingAssertion) assertionToLocalize;
                if ( goid != null && Goid.equals(goid, bra.getServerCertificateGoid()) ) {
                    if (localizeType == LocalizeAction.REPLACE) {
                        // replace server cert oid
                        bra.setServerCertificateGoid(localCertId);
                        logger.info("The server certificate goid of the imported bridge routing assertion has been changed " +
                                    "from " + goid + " to " + localCertId);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            } else if ( assertionToLocalize instanceof UsesEntities ) {
                UsesEntities entitiesUser = (UsesEntities)assertionToLocalize;
                for(EntityHeader entityHeader : entitiesUser.getEntitiesUsed()) {
                    if ( entityHeader.getType().equals(EntityType.TRUSTED_CERT) && entityHeader.equalsId(goid) ) {
                        if(localizeType == LocalizeAction.REPLACE) {
                            if ( !localCertId.equals(goid)) {
                                EntityHeader newEntityHeader = new EntityHeader(localCertId, EntityType.TRUSTED_CERT, null, null);
                                entitiesUser.replaceEntity(entityHeader, newEntityHeader);
                                logger.info("The server certificate goid of the imported assertion has been changed " +
                                            "from " + goid + " to " + localCertId);
                                break;
                            }
                        } else if(localizeType == LocalizeAction.DELETE) {
                            logger.info("Deleted this assertion from the tree.");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static TrustedCertReference parseFromElement( final ExternalReferenceFinder context,
                                                         final Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        TrustedCertReference output = new TrustedCertReference( context );
        String val = getParamFromEl(el, OLD_OID_EL_NAME);
        if (val != null) {
            try {
                output.goid = GoidUpgradeMapper.mapOid(EntityType.TRUSTED_CERT, Long.parseLong(val));
            } catch (NumberFormatException nfe) {
                output.goid = PersistentEntity.DEFAULT_GOID;
            }
        }

        val = getParamFromEl(el, GOID_EL_NAME);
        if (val != null) {
            try {
                output.goid = new Goid(val);
            } catch (IllegalArgumentException e) {
                throw new InvalidDocumentFormatException("Invalid trusted certificate goid: " + ExceptionUtils.getMessage(e), e);
            }
        }

        output.certName = getParamFromEl(el, CERTNAME_EL_NAME);
        output.certIssuerDn = getParamFromEl(el, ISSUERDN_EL_NAME);
        String serialValue = getParamFromEl(el, CERTSERIAL_EL_NAME);
        if ( serialValue != null ) {
            output.certSerial = new BigInteger( serialValue );
        }
        return output;
    }

    public Goid getGoid() {
        return goid;
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

    @Override
    public boolean setLocalizeReplace(Goid identifier) {
        localizeType = LocalizeAction.REPLACE;
        localCertId = identifier;
        return true;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final TrustedCertReference that = (TrustedCertReference) o;

        if (goid != null ? !goid.equals(that.goid) : that.goid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return goid != null ? goid.hashCode() : 0;
    }
}