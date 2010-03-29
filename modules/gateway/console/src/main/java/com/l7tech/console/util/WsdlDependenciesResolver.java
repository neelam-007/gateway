package com.l7tech.console.util;

import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.WSDLException;
import java.util.Collection;

/**
 * Resolving a WSDL with other dependencies and other related info.
 *
 * @author ghuang
 */
public class WsdlDependenciesResolver {
    private String wsdlUri;
    private String wsdlXml;
    private Collection<ServiceDocument> svcDocuments;

    public WsdlDependenciesResolver() {}

    public WsdlDependenciesResolver(String wsdlUri, String wsdlXml, Collection<ServiceDocument> svcDocuments) {
        this.wsdlUri = wsdlUri;
        this.wsdlXml = wsdlXml;
        this.svcDocuments = svcDocuments;
    }

    public Wsdl resolve() throws WSDLException {
        return ServiceDocumentWsdlStrategy.parseWsdl(wsdlUri, wsdlXml, svcDocuments);
    }

    public String getWsdlUri() {
        return wsdlUri;
    }

    public void setWsdlUri(String wsdlUri) {
        this.wsdlUri = wsdlUri;
    }

    public String getWsdlXml() {
        return wsdlXml;
    }

    public void setWsdlXml(String wsdlXml) {
        this.wsdlXml = wsdlXml;
    }

    public Collection<ServiceDocument> getSvcDocuments() {
        return svcDocuments;
    }

    public void setSvcDocuments(Collection<ServiceDocument> svcDocuments) {
        this.svcDocuments = svcDocuments;
    }
}
