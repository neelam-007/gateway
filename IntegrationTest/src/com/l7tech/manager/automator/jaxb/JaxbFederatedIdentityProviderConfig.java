package com.l7tech.manager.automator.jaxb;

import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 17, 2008
 * Time: 4:07:54 PM
 * A FederatedIdentityProviderConfig contains a list of trusted cert oids. When you move a federated identity provider
 * from one SSG to another these oid's will be invalid. As a result when marshalling a federated identity provider
 * we keep a string list of the providers trusted certs subjects.
 * When recreating the fed on another ssg, all dependant trusted certs must first be uploaded, then with the
 * information in trustedCertDns we can lookup the trusted certs and update the list of oid's needed by the
 * fed provider prior to saving in the new SSG.
 */
@XmlRootElement
public class JaxbFederatedIdentityProviderConfig {

    private FederatedIdentityProviderConfig fedProvider;

    private List<String> trustedCertDns;

    public JaxbFederatedIdentityProviderConfig(){
    }

    public JaxbFederatedIdentityProviderConfig(FederatedIdentityProviderConfig fedProvider){
        this.fedProvider = fedProvider;
    }
    
    public FederatedIdentityProviderConfig getFedProvider() {
        return fedProvider;
    }

    public void setFedProvider(FederatedIdentityProviderConfig fedProvider) {
        this.fedProvider = fedProvider;
    }

    public List<String> getTrustedCertDns() {
        return trustedCertDns;
    }

    public void setTrustedCertDns(List<String> trustedCertDns) {
        this.trustedCertDns = trustedCertDns;
    }
    
}
