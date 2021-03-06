package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

/**
 * The XML security recipient context is for attaching recipient/security header actor information to
 * xml security type assertions. Those assertions are the ones in com.l7tech.policy.assertion.xmlsec.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 14, 2005<br/>
 * $Id$
 */
public class XmlSecurityRecipientContext implements Serializable {
    private String actor;
    private String base64edX509Certificate;
    private X509Certificate x509Certificate;
    public static final String LOCALRECIPIENT_ACTOR_VALUE = "";

    public XmlSecurityRecipientContext() {}

    public XmlSecurityRecipientContext(String actor, String base64edX509Certificate) {
        this.actor = actor;
        this.base64edX509Certificate = base64edX509Certificate;
        processCertificate();
    }

    /**
     * @return a XmlSecurityRecipientContext that represent the current system as a recipient
     */
    public static XmlSecurityRecipientContext getLocalRecipient() {
        return new XmlSecurityRecipientContext(LOCALRECIPIENT_ACTOR_VALUE, null);
    }

    /**
     * @return true if this instance represents the local system as a recipient, false otherwise.
     */
    public boolean localRecipient() {
        if (!actor.equals(LOCALRECIPIENT_ACTOR_VALUE)) {
            return false;
        } else if (base64edX509Certificate != null) {
            return false;
        }
        return true;
    }

    public String getActor() {
        return actor;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getBase64edX509Certificate() {
        return base64edX509Certificate;
    }

    /**
     * @deprecated For serialization only
     */
    @Deprecated
    public void setBase64edX509Certificate(String base64edX509Certificate) {
        this.base64edX509Certificate = base64edX509Certificate;
        processCertificate();
    }

    public X509Certificate getX509Certificate() {
        return this.x509Certificate;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof XmlSecurityRecipientContext)) return false;

        final XmlSecurityRecipientContext xmlSecurityRecipientContext = (XmlSecurityRecipientContext) o;

        if (actor != null ? !actor.equals(xmlSecurityRecipientContext.actor) : xmlSecurityRecipientContext.actor != null) return false;
        if (base64edX509Certificate != null ? !base64edX509Certificate.equals(xmlSecurityRecipientContext.base64edX509Certificate) : xmlSecurityRecipientContext.base64edX509Certificate != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (actor != null ? actor.hashCode() : 0);
        result = 29 * result + (base64edX509Certificate != null ? base64edX509Certificate.hashCode() : 0);
        return result;
    }

    private void processCertificate() {
        try {
            this.x509Certificate = base64edX509Certificate == null
                                       ? null
                                       : CertUtils.decodeCert(HexUtils.decodeBase64(base64edX509Certificate, true));
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Bad certificate", e);
        }
    }
}
