package com.ca.siteminder;

import com.ca.siteminder.util.SiteMinderUtil;
import netegrity.siteminder.javaagent.UserCredentials;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;


/**
 * Copyright: CA Technologies, 2013
 * User: ymoiseyenko
 * Date: 7/8/13
 */
public final class SiteMinderCredentials {
    private final UserCredentials credentials;

    @Override
    public String toString() {
        return SiteMinderUtil.getCredentialsAsString(credentials);
    }

    public SiteMinderCredentials() {
        credentials = new UserCredentials();//create empty UserCredentials object
    }

    public SiteMinderCredentials(final String login, final String password){
        credentials = new UserCredentials(login, password);
        credentials.certUserDN = null;//set to null according to the SiteMinder api docs
        credentials.certIssuerDN = null;//set to null according to the SiteMinder api docs
    }

    public SiteMinderCredentials(X509Certificate[] certificates) throws CertificateEncodingException{
        credentials = new UserCredentials();
        if(certificates != null && certificates.length > 0) {
            SiteMinderUtil.handleCertificate(certificates[0], credentials);
        }

    }

    public SiteMinderCredentials(X509Certificate certificate) throws CertificateEncodingException{
        credentials = new UserCredentials();
        if(certificate != null ) {
            SiteMinderUtil.handleCertificate(certificate, credentials);
        }

    }

    public SiteMinderCredentials(final String login, final String password, final X509Certificate[] certificates) throws CertificateEncodingException {
        credentials = new UserCredentials(login, password);
        if(certificates != null && certificates.length > 0) {
            SiteMinderUtil.handleCertificate(certificates[0], credentials);
        }
    }

    /**
     * the reason it is not public because we don't want to expose internal UserCredentials object to  non-siteminder api classes
     * @return
     */
    UserCredentials getUserCredentials() {
        return credentials;
    }

    public void addClientCertificates(X509Certificate[] certificates) throws CertificateEncodingException {
        if(certificates != null && certificates.length > 0) {
            SiteMinderUtil.handleCertificate(certificates[0], credentials);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SiteMinderCredentials that = (SiteMinderCredentials) o;

        if (credentials != null ? !credentials.equals(that.credentials) : that.credentials != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return credentials != null ? credentials.hashCode() : 0;
    }
}
