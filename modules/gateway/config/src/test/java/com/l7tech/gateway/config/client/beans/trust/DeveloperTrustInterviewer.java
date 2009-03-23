package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.options.OptionType;
import com.l7tech.common.io.PermissiveSSLSocketFactory;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.gateway.config.client.beans.TypedConfigurableBean;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.net.URLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Developer configuration helper for process controller.
 */
public class DeveloperTrustInterviewer extends TrustInterviewer {

    private static final String PC_HOME = "build/sspc";
    private static final String HTTPS_URL = "https://localhost:8182/";

    public static void main( final String[] args ) {
        System.setProperty( "com.l7tech.server.controller.home", PC_HOME );
        new DeveloperTrustInterviewer().run();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    List<ConfigurationBean> doInterview(List<ConfigurationBean> inBeans, ResourceBundle bundle) throws IOException {
        boolean setIp = false;

        for ( ConfigurationBean bean : inBeans ) {
            if ( bean instanceof RemoteNodeManagementEnabled ) {
                bean.setConfigValue(Boolean.TRUE);
            } else if ( bean instanceof TypedConfigurableBean) {
                if ( bean.getId().equals(HOSTPROPERTIES_NODEMANAGEMENT_IPADDRESS) ) {
                    bean.setConfigValue("0.0.0.0");
                    setIp = true;
                }
            }

        }

        if ( !setIp ) {
            inBeans.add(new TypedConfigurableBean<String>( HOSTPROPERTIES_NODEMANAGEMENT_IPADDRESS, "Listener IP Address", "Valid inputs are any IP Address or * for all.", "localhost", "0.0.0.0", OptionType.IP_ADDRESS ) );
        }

        try {
            URLConnection conn = new URL(HTTPS_URL).openConnection();
            if (!(conn instanceof HttpsURLConnection)) throw new Exception("Provided URL did not result in an HTTPS connection");

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
            httpsURLConnection.setSSLSocketFactory(new PermissiveSSLSocketFactory());
            httpsURLConnection.setHostnameVerifier(new PermissiveHostnameVerifier());
            httpsURLConnection.connect();
            Certificate[] certs = httpsURLConnection.getServerCertificates();
            if (certs == null || certs.length < 1) throw new Exception("Server presented no certificates");
            if (!(certs[0] instanceof X509Certificate)) throw new Exception("Server certificate wasn't X.509");
            X509Certificate cert = (X509Certificate)certs[0];
            inBeans.add( new ConfiguredTrustedCert(cert, null) );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return inBeans;
    }
}
