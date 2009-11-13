package com.l7tech.gateway.common.service;

import com.l7tech.wsdl.Wsdl;
import com.l7tech.wsdl.ResourceTrackingWSDLLocator;

import javax.wsdl.WSDLException;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ServiceDocumentWsdlStrategy implements PublishedService.WsdlStrategy {

    //- PUBLIC

    public ServiceDocumentWsdlStrategy( final Collection<ServiceDocument> serviceDocuments ) {
        this.serviceDocuments = serviceDocuments==null ?
                Collections.<ServiceDocument>emptyList() :
                new ArrayList<ServiceDocument>(serviceDocuments);
    }

    @Override
    public final Wsdl parseWsdl( final PublishedService service,
                                 final String uri,
                                 final String wsdl ) throws WSDLException {
        return Wsdl.newInstance(Wsdl.getWSDLLocator(uri, buildContent(uri, wsdl, service), getLogger()));
    }

    public static Wsdl parseWsdl( final PublishedService service,
                                  final Collection<ServiceDocument> serviceDocuments ) throws WSDLException {
        return parseWsdl( service.getWsdlUrl(), service.getWsdlXml(), serviceDocuments );            
    }

    public static Wsdl parseWsdl( final String uri,
                                  final String wsdl,
                                  final Collection<ServiceDocument> serviceDocuments ) throws WSDLException {
        return new ServiceDocumentWsdlStrategy( serviceDocuments ).parseWsdl( null, uri, wsdl );
    }

    public static List<ServiceDocument> fromWsdlResources( final Collection<ResourceTrackingWSDLLocator.WSDLResource> sourceDocs ) {
        List<ServiceDocument> svcDocs = new ArrayList<ServiceDocument>();

        for (ResourceTrackingWSDLLocator.WSDLResource sourceDoc : sourceDocs) {
            ServiceDocument doc = new ServiceDocument();
            doc.setUri(sourceDoc.getUri());
            doc.setType("WSDL-IMPORT");
            doc.setContents(sourceDoc.getWsdl());
            doc.setContentType("text/xml");
            svcDocs.add(doc);
        }
        return svcDocs;
    }

    //- PROTECTED

    protected Collection<ServiceDocument> loadServiceDocuments( final PublishedService service ) throws WSDLException {
        return serviceDocuments;
    }

    protected Logger getLogger() {
        return logger;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceDocumentWsdlStrategy.class.getName() );

    private final Collection<ServiceDocument> serviceDocuments;

    private Map<String,String> buildContent( final String baseUri,
                                             final String baseContent,
                                             final PublishedService service ) throws WSDLException {
        Map<String,String> content = new HashMap<String,String>();

        for ( ServiceDocument doc : loadServiceDocuments( service ) ) {
            content.put( doc.getUri(), doc.getContents() );
        }

        content.put( baseUri, baseContent );

        return content;
    }
}