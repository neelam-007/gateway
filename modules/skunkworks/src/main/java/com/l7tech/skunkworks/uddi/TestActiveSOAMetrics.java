package com.l7tech.skunkworks.uddi;

import com.l7tech.common.uddi.guddiv3.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 *
 */
public class TestActiveSOAMetrics extends UDDISupport {

    private static final Logger logger = Logger.getLogger( TestActiveSOAMetrics.class.getName() );

    private String serviceKey = ""; //TODO add service key here
    private String targetName = "Layer7";

    //TODO update settings for test UDDI registry
    private String login = "administrator";
    private String password = "";
    private String inquiryUrl = "http://donalwinxp.l7tech.com:53307/UddiRegistry/inquiry";
    private String publishingUrl = "http://donalwinxp.l7tech.com:53307/UddiRegistry/publish";
    private String securityUrl = "http://donalwinxp.l7tech.com:53307/UddiRegistry/publish";
    private String subscriptionUrl = "http://donalwinxp.l7tech.com:53307/UddiRegistry/publish";

    public TestActiveSOAMetrics() {
        super(logger);
    }

    public static void main( String[] args ) throws Exception {
        TestActiveSOAMetrics metrics = new TestActiveSOAMetrics();
        metrics.addServiceMetrics();
        metrics.deleteServiceMetrics();
    }

    private void addServiceMetrics() throws Exception {
        String serviceKey = this.serviceKey;
        System.out.println( "Service key is: " + serviceKey );

        try {
            TModel tModel = new TModel();
            Name name = new Name();
            name.setValue( "Metrics");
            tModel.setName( name );
            Description desc = new Description();
            desc.setValue( "Metrics for some service" );
            tModel.getDescription().add( desc );
            CategoryBag cbag = new CategoryBag();
            tModel.setCategoryBag( cbag );

            KeyedReference mainKR = new KeyedReference();
            mainKR.setTModelKey( "uddi:474e1904-69ad-9a6b-11c2-fc8e41c64350" ); //TODO is this a constant?
            mainKR.setKeyName( "Metrics" );
            mainKR.setKeyValue( "Metrics" );
            cbag.getKeyedReference().add( mainKR );

            KeyedReference serviceKR = new KeyedReference();
            serviceKR.setTModelKey( "uddi:centrasite.com:management:service.key" );
            serviceKR.setKeyName( "Service Key" );
            serviceKR.setKeyValue( serviceKey );
            cbag.getKeyedReference().add( serviceKR );

            KeyedReference targetKR = new KeyedReference();
            targetKR.setTModelKey( "uddi:centrasite.com:management:target" );
            targetKR.setKeyName( "Target" );
            targetKR.setKeyValue( targetName );
            cbag.getKeyedReference().add( targetKR );

            KeyedReference metricsRequestCount = new KeyedReference();
            metricsRequestCount.setTModelKey( "uddi:centrasite.com:management:metrics:total.request.count" );
            metricsRequestCount.setKeyName( "Count of hits" );
            metricsRequestCount.setKeyValue( "2" );
            cbag.getKeyedReference().add( metricsRequestCount );

            KeyedReference metricsSuccessRequestCount = new KeyedReference();
            metricsSuccessRequestCount.setTModelKey( "uddi:centrasite.com:management:metrics:success.request.count" );
            metricsSuccessRequestCount.setKeyName( "Count of successful hits" );
            metricsSuccessRequestCount.setKeyValue( "1" );
            cbag.getKeyedReference().add( metricsSuccessRequestCount );

            {
                KeyedReference metricsKR = new KeyedReference();
                metricsKR.setTModelKey( "uddi:centrasite.com:management:metrics:fault.request.count" );
                metricsKR.setKeyName( "Count of fault hits" );
                metricsKR.setKeyValue( "1" );
                cbag.getKeyedReference().add( metricsKR );
            }
            {
                KeyedReference metricsKR = new KeyedReference();
                metricsKR.setTModelKey( "uddi:centrasite.com:management:metrics:average.response.time" );
                metricsKR.setKeyName( "Average response time" );
                metricsKR.setKeyValue( "20" );
                cbag.getKeyedReference().add( metricsKR );
            }
            {
                KeyedReference metricsKR = new KeyedReference();
                metricsKR.setTModelKey( "uddi:centrasite.com:management:metrics:minimum.response.time" );
                metricsKR.setKeyName( "Minimum response time" );
                metricsKR.setKeyValue( "20" );
                cbag.getKeyedReference().add( metricsKR );
            }
            {
                KeyedReference metricsKR = new KeyedReference();
                metricsKR.setTModelKey( "uddi:centrasite.com:management:metrics:maximum.response.time" );
                metricsKR.setKeyName( "Maximum response time" );
                metricsKR.setKeyValue( "20" );
                cbag.getKeyedReference().add( metricsKR );
            }
            {
                KeyedReference metricsKR = new KeyedReference();
                metricsKR.setTModelKey( "uddi:centrasite.com:management:metrics:availability" );
                metricsKR.setKeyName( "Availability" );
                metricsKR.setKeyValue( "20.0" );
                cbag.getKeyedReference().add( metricsKR );
            }

            KeyedReference metricsStartTime = new KeyedReference();
            metricsStartTime.setTModelKey( "uddi:centrasite.com:management:start.datetime" );
            metricsStartTime.setKeyName( "Start Time" );
            metricsStartTime.setKeyValue( "2009-11-01T00:00:00" );
            cbag.getKeyedReference().add( metricsStartTime );

            KeyedReference metricsEndTime = new KeyedReference();
            metricsEndTime.setTModelKey( "uddi:centrasite.com:management:end.datetime" );
            metricsEndTime.setKeyName( "End Time" );
            metricsEndTime.setKeyValue( "2009-11-01T01:00:00" );
            cbag.getKeyedReference().add( metricsEndTime );

            String authToken = authToken();
            UDDIPublicationPortType publicationPort = getPublishPort();

            SaveTModel saveTModel = new SaveTModel();
            saveTModel.setAuthInfo( authToken );
            saveTModel.getTModel().add( tModel );
            TModelDetail tModelDetail = publicationPort.saveTModel( saveTModel );

//                        <tModel tModelKey="uddi:3d32ac10-5dd1-11da-88b8-51d47e6188b2" deleted="false" xmlns="urn:uddiorg:api_v3">
//                       <name>Metrics</name>
//                       <description>Metrics of EchoAccessPoint</description>
//                       <categoryBag>
//                       <keyedReference
//                         tModelKey="…(key of object type taxonomy)"
//                         keyName="Metrics"
//                         keyValue="Metrics"/>
//
//                         <keyedReference
//                           tModelKey="uddi:centrasite.com:management:metrics:total.request.count"
//                           keyName="Count of hits"
//                           keyValue="14"/>
//
//                       </categoryBag>
//                       </tModel>

            // find service detail
            GetServiceDetail getServiceDetail = new GetServiceDetail();
            getServiceDetail.setAuthInfo(authToken);
            UDDIInquiryPortType inquiryPort = getInquirePort();
            getServiceDetail.getServiceKey().add(serviceKey);

            ServiceDetail detail = inquiryPort.getServiceDetail(getServiceDetail);

            List<BusinessService> businessServices = detail.getBusinessService();
            for ( BusinessService businessService : businessServices ) {
                CategoryBag bscbag = businessService.getCategoryBag();

                if (bscbag==null) {
                    bscbag = new CategoryBag();
                    businessService.setCategoryBag( bscbag );
                }

                KeyedReference referenceKR = new KeyedReference();
                referenceKR.setTModelKey( "uddi:centrasite.com:management:metrics:reference" );
                referenceKR.setKeyName( "Metrics" );
                referenceKR.setKeyValue( tModelDetail.getTModel().get(0).getTModelKey() );
                bscbag.getKeyedReference().add( referenceKR );

                SaveService saveService = new SaveService();
                saveService.setAuthInfo( authToken );
                saveService.getBusinessService().add( businessService );
                publicationPort.saveService( saveService );
            }

//<keyedReference
//    tModelKey="uddi:centrasite.com:management:metrics:reference"
//    keyName="Metrics"
//    keyValue="uddi:3d32ac10-5dd1-11da-88b8-51d47e6188b2"/>

            // Simulate a bunch of metrics updates
//            saveTModel.getTModel().get( 0 ).setTModelKey( tModelDetail.getTModel().get(0).getTModelKey() );
//            metricsStartTime.setKeyValue( "2009-11-01T01:00:00" );
//            metricsEndTime.setKeyValue( "2009-11-01T02:00:00" );
//            metricsRequestCount.setKeyValue( "3" );
//            metricsSuccessRequestCount.setKeyValue( "2" );
//            publicationPort.saveTModel( saveTModel );
//
//            metricsStartTime.setKeyValue( "2009-11-01T02:00:00" );
//            metricsEndTime.setKeyValue( "2009-11-01T03:00:00" );
//            metricsRequestCount.setKeyValue( "4" );
//            metricsSuccessRequestCount.setKeyValue( "3" );
//            publicationPort.saveTModel( saveTModel );
//
//            metricsStartTime.setKeyValue( "2009-11-01T03:00:00" );
//            metricsEndTime.setKeyValue( "2009-11-01T04:00:00" );
//            metricsRequestCount.setKeyValue( "5" );
//            metricsSuccessRequestCount.setKeyValue( "4" );
//            publicationPort.saveTModel( saveTModel );
//
//            metricsStartTime.setKeyValue( "2009-11-01T04:00:00" );
//            metricsEndTime.setKeyValue( "2009-11-01T05:00:00" );
//            metricsRequestCount.setKeyValue( "6" );
//            metricsSuccessRequestCount.setKeyValue( "5" );
//            publicationPort.saveTModel( saveTModel );
//
//            metricsStartTime.setKeyValue( "2009-11-01T05:00:00" );
//            metricsEndTime.setKeyValue( "2009-11-01T06:00:00" );
//            metricsRequestCount.setKeyValue( "7" );
//            metricsSuccessRequestCount.setKeyValue( "6" );
//            publicationPort.saveTModel( saveTModel );

        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error adding metrics: ", drfm);
        }
    }

    private void deleteServiceMetrics() throws Exception {
        String serviceKey = this.serviceKey;
        System.out.println( "Service key is: " + serviceKey );

        try {
            String authToken = authToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find service detail
            GetServiceDetail getServiceDetail = new GetServiceDetail();
            getServiceDetail.setAuthInfo(authToken);
            getServiceDetail.getServiceKey().add(serviceKey);

            ServiceDetail detail = inquiryPort.getServiceDetail(getServiceDetail);
            if (detail != null) {
                List<String> metricsTModelKeys = new ArrayList<String>();

                UDDIPublicationPortType publicationPort = getPublishPort();

                List<BusinessService> businessServices = detail.getBusinessService();
                for ( BusinessService businessService : businessServices ) {
                    boolean updated = false;
                    CategoryBag cbag = businessService.getCategoryBag();
                    if ( cbag != null ) {

                        List<KeyedReference> references = cbag.getKeyedReference();
                        if ( references != null ) {
                            for ( Iterator<KeyedReference> referenceIterator = references.iterator(); referenceIterator.hasNext();  ) {
                                KeyedReference reference = referenceIterator.next();
                                if ( "uddi:centrasite.com:management:metrics:reference".equals( reference.getTModelKey() ) ) {
                                    metricsTModelKeys.add( reference.getKeyValue() );
                                    referenceIterator.remove();
                                    updated = true;
                                }
                            }
                        }
                    }

                    if ( updated ) {
                        SaveService saveService = new SaveService();
                        saveService.setAuthInfo( authToken );
                        saveService.getBusinessService().add( businessService );
                        publicationPort.saveService( saveService );
                    }
                }

                if ( !metricsTModelKeys.isEmpty() ) {
                    System.out.println( "Deleting metrics tModels : " + metricsTModelKeys );

                    DeleteTModel deleteTModel = new DeleteTModel();
                    deleteTModel.setAuthInfo( authToken );
                    deleteTModel.getTModelKey().addAll( metricsTModelKeys );

                    publicationPort.deleteTModel( deleteTModel );
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error deleting metrics: ", drfm);
        }
    }

    @Override
    protected String getUsername() {
        return login;
    }

    @Override
    protected String getPassword() {
        return password;
    }

    @Override
    protected String getInquiryUrl() {
        return inquiryUrl;
    }

    @Override
    protected String getPublishingUrl() {
        return publishingUrl;
    }

    @Override
    protected String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    @Override
    protected String getSecurityUrl() {
        return securityUrl;
    }
}
