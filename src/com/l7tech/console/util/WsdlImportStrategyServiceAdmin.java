package com.l7tech.console.util;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.uddi.UDDIRegistryInfo;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.CollectionUpdate;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.service.*;
import com.l7tech.admin.Administrative;
import org.xml.sax.InputSource;
import org.springframework.transaction.annotation.Transactional;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service Admin that adds a WSDL Strategy to PublishedServices.
 *
 * <p>This means that service documents can be retrieved via the Gateway.</p>
 *
 * @author steve
 */
public class WsdlImportStrategyServiceAdmin implements ServiceAdmin {

    //- PUBLIC

    public WsdlImportStrategyServiceAdmin( final ServiceAdmin serviceAdmin ) {
        this.delegate = serviceAdmin;    
    }

    public void deletePublishedService( String oid ) throws DeleteException {
        delegate.deletePublishedService( oid );
    }

    public void deleteSampleMessage( SampleMessage message ) throws DeleteException {
        delegate.deleteSampleMessage( message );
    }

    public ServiceHeader[] findAllPublishedServicesByOffset( int offset, int windowSize ) throws FindException {
        return delegate.findAllPublishedServicesByOffset( offset, windowSize );
    }

    public SampleMessage findSampleMessageById( long oid ) throws FindException {
        return delegate.findSampleMessageById( oid );
    }

    public EntityHeader[] findSampleMessageHeaders( long serviceOid, String operationName ) throws FindException {
        return delegate.findSampleMessageHeaders( serviceOid, operationName );
    }

    public Collection<ServiceDocument> findServiceDocumentsByServiceID( String serviceID ) throws FindException {
        return delegate.findServiceDocumentsByServiceID( serviceID );
    }

    public String[] findUDDIRegistryURLs() throws FindException {
        return delegate.findUDDIRegistryURLs();
    }

    public WsdlInfo[] findWsdlUrlsFromUDDIRegistry( String uddiURL, UDDIRegistryInfo info, String username, char[] password, String namePattern, boolean caseSensitive ) throws FindException {
        return delegate.findWsdlUrlsFromUDDIRegistry( uddiURL, info, username, password, namePattern, caseSensitive );
    }

    public String getConsumptionURL( String serviceoid ) throws FindException {
        return delegate.getConsumptionURL( serviceoid );
    }

    public String getPolicyURL( String serviceoid ) throws FindException {
        return delegate.getPolicyURL( serviceoid );
    }

    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo() {
        return delegate.getUDDIRegistryInfo();
    }

    public Set<ServiceTemplate> findAllTemplates() {
        return delegate.findAllTemplates();
    }

    public String[] listExistingCounterNames() throws FindException {
        return delegate.listExistingCounterNames();
    }

    public long savePublishedService( PublishedService service ) throws UpdateException, SaveException, VersionException, PolicyAssertionException {
        return delegate.savePublishedService( service );
    }

    public long savePublishedServiceWithDocuments( PublishedService service, Collection<ServiceDocument> serviceDocuments ) throws UpdateException, SaveException, VersionException, PolicyAssertionException {
        return delegate.savePublishedServiceWithDocuments( service, serviceDocuments );
    }

    public long saveSampleMessage( SampleMessage sm ) throws SaveException {
        return delegate.saveSampleMessage( sm );
    }

    public JobId<PolicyValidatorResult> validatePolicy( String policyXml, PolicyType policyType, boolean soap, Wsdl wsdl) {
        return delegate.validatePolicy( policyXml, policyType, soap, wsdl);
    }

    public JobId<PolicyValidatorResult> validatePolicy( String policyXml, PolicyType policyType, boolean soap, Wsdl wsdl, HashMap<String, Policy> fragments ) {
        return delegate.validatePolicy( policyXml, policyType, soap, wsdl, fragments);
    }

    public ServiceHeader[] findAllPublishedServices() throws FindException {
        return delegate.findAllPublishedServices();
    }

    public PublishedService findServiceByID( String oid ) throws FindException {
        return decorate(delegate.findServiceByID( oid ));
    }

    public String resolveWsdlTarget( String url ) throws IOException {
        return delegate.resolveWsdlTarget( url );
    }

    public <OUT extends Serializable> JobResult<OUT> getJobResult( JobId<OUT> jobId ) throws UnknownJobException, JobStillActiveException {
        return delegate.getJobResult( jobId );
    }

    public <OUT extends Serializable> String getJobStatus( JobId<OUT> jobId ) {
        return delegate.getJobStatus( jobId );
    }

    public CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(int oldVersionID) throws FindException {
        return delegate.getPublishedServicesUpdate(oldVersionID);
    }
    //- PRIVATE

    private final ServiceAdmin delegate;

    private PublishedService decorate( final PublishedService service ) {
        final String serviceId = service.getId();
        service.parseWsdlStrategy( new PublishedService.WsdlStrategy(){
            public Wsdl parseWsdl( String uri, String wsdl ) throws WSDLException {
                Wsdl parsedWsdl;

                try {
                    Collection<ServiceDocument> serviceDocuments = delegate.findServiceDocumentsByServiceID( serviceId );
                    parsedWsdl = Wsdl.newInstance(WsdlUtils.getWSDLFactory(), Wsdl.getWSDLLocator(uri, buildContent(uri, wsdl, serviceDocuments), logger));
                }
                catch (FindException fe) {
                    throw new WSDLException(WSDLException.OTHER_ERROR, "Error accessing WSDL import.");
                }
                catch (WSDLException we) {
                    // Fallback to direct acess to any imports
                    InputSource source = new InputSource();
                    source.setSystemId(uri);
                    source.setCharacterStream(new StringReader(wsdl));
                    parsedWsdl = Wsdl.newInstance(uri, source);
                }

                return parsedWsdl;
            }
        } );

        return service;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsdlImportStrategyServiceAdmin.class.getName());

     private Map<String,String> buildContent( String baseUri, String baseContent, Collection<ServiceDocument> docs ) {
        Map<String,String> content = new HashMap<String,String>();

        for ( ServiceDocument doc : docs ) {
            content.put( doc.getUri(), doc.getContents() );
        }

        content.put( baseUri, baseContent );

        return content;
    }
}
