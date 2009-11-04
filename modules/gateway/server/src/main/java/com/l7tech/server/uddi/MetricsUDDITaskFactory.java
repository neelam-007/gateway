package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.server.wsdm.MetricsRequestContext;
import com.l7tech.util.Pair;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 *
 */
public class MetricsUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public MetricsUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                   final UDDIServiceControlManager uddiServiceControlManager,
                                   final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                   final Aggregator aggregator ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiServiceControlManager = uddiServiceControlManager;
        this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
        this.aggregator = aggregator;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS ) {
                task = new MetricsPublishUDDITask(
                        uddiRegistryManager,
                        uddiServiceControlManager,
                        uddiProxiedServiceInfoManager,
                        aggregator,
                        timerEvent.getRegistryOid() );
            }
        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIServiceControlManager uddiServiceControlManager;
    private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
    private final Aggregator aggregator;

    private static final class MetricsPublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( MetricsPublishUDDITask.class.getName() );

        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIServiceControlManager uddiServiceControlManager;
        private final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager;
        private final Aggregator aggregator;
        private final long registryOid;

        MetricsPublishUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                final UDDIServiceControlManager uddiServiceControlManager,
                                final UDDIProxiedServiceInfoManager uddiProxiedServiceInfoManager,
                                final Aggregator aggregator,
                                final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiServiceControlManager = uddiServiceControlManager;
            this.uddiProxiedServiceInfoManager = uddiProxiedServiceInfoManager;
            this.aggregator = aggregator;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Publishing metrics to UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    final long end = System.currentTimeMillis();

                    final Collection<UDDIServiceControl> uddiServiceControls =
                            uddiServiceControlManager.findByUDDIRegistryAndMetricsState( registryOid, true );

                    final Collection<UDDIProxiedServiceInfo> uddiProxiedServiceInfos =
                            uddiProxiedServiceInfoManager.findByUDDIRegistryAndMetricsState( registryOid, true );

                    final Map<Long,Collection<Pair<String,String>>> serviceMap = buildServiceToServiceKeyMap( uddiServiceControls, uddiProxiedServiceInfos );

                    final UDDIClient client = UDDIHelper.newUDDIClient( uddiRegistry );

                    final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    final String endTime = dateFormat.format( new Date(end) );
                    for ( Map.Entry<Long,Collection<Pair<String,String>>> entry : serviceMap.entrySet()  ) {
                        final long serviceOid = entry.getKey();
                        final MetricsSummaryBin bin = aggregator.getMetricsForService(serviceOid);
                        if ( bin == null ) {
                            continue; //TODO [steve] publish empty metrics info?
                        }

                        final MetricsRequestContext metricsRequestContext = new MetricsRequestContext(bin, true, null, end - bin.getStartTime());
                        final String startTime = dateFormat.format( new Date(metricsRequestContext.getPeriodStart()) );

                        for ( Pair<String,String> businessServicePair : entry.getValue() ) {
                            Collection<UDDIClient.UDDIKeyedReference> references = new ArrayList<UDDIClient.UDDIKeyedReference>();

                            //TODO [steve] CentraSite specific values should come from UDDI configuration

                            // metadata
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:474e1904-69ad-9a6b-11c2-fc8e41c64350", "Metrics", "Metrics") ); //TODO [steve] is this a constant?
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:service.key", "Service Key", businessServicePair.getKey()) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:target", "Target", "Target") ); //TODO [steve] what is this??
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:start.datetime", "Start Time", startTime) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:end.datetime", "End Time", endTime) );

                            // metrics
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:total.request.count", "Count of hits", Long.toString(metricsRequestContext.getNrRequests())) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:success.request.count", "Count of successful hits", Long.toString(metricsRequestContext.getNrSuccessRequests())) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:fault.request.count", "Count of fault hits", Long.toString(metricsRequestContext.getNrFailedRequests())) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:average.response.time", "Average response time", Long.toString(metricsRequestContext.getAvgResponseTime())) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:minimum.response.time", "Minimum response time", Long.toString(metricsRequestContext.getMinResponseTime())) );
                            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:maximum.response.time", "Maximum response time", Long.toString(metricsRequestContext.getMaxResponseTime())) );
                            //references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:availability", "Availability", "100") ); //TODO [steve] do we have an availability metric?

                            //TODO [steve] ensure services reference metrics TModels

//                            client.publishTModel( null,   //TODO [steve] update existing TModel
//                                                  "Metrics",
//                                                  "Metrics for " + businessServicePair.getValue(),
//                                                  references );
                        }
                    }

                } else {
                    logger.info( "Ignoring metrics event for UDDI registry (#"+registryOid+"), registry not found or is disabled." );
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }
        }

        /**
         * Build map of serviceOids to pairs of business serviceKey/business service names 
         */
        private Map<Long, Collection<Pair<String,String>>> buildServiceToServiceKeyMap( final Collection<UDDIServiceControl> uddiServiceControls,
                                                                           final Collection<UDDIProxiedServiceInfo> uddiProxiedServiceInfos ) {
            Map<Long, Collection<Pair<String,String>>> serviceMap = new HashMap<Long, Collection<Pair<String,String>>>();

            for ( UDDIServiceControl control : uddiServiceControls ) {
                Collection<Pair<String,String>> serviceList = new ArrayList<Pair<String,String>>();
                serviceList.add( new Pair<String,String>(control.getUddiServiceKey(), control.getUddiServiceName()) );
                serviceMap.put( control.getPublishedServiceOid(), serviceList );
            }

            for ( UDDIProxiedServiceInfo info : uddiProxiedServiceInfos ) {
                Set<UDDIProxiedService> infoServices = info.getProxiedServices();
                if ( infoServices != null && !infoServices.isEmpty() ) {
                    Collection<Pair<String,String>> serviceList = serviceMap.get( info.getPublishedServiceOid() );
                    if ( serviceList == null ) {
                        serviceList = new ArrayList<Pair<String,String>>();
                        serviceMap.put( info.getPublishedServiceOid(), serviceList );
                    }

                    for ( UDDIProxiedService service : infoServices ) {
                        serviceList.add( new Pair<String,String>(service.getUddiServiceKey(),service.getUddiServiceName()) );
                    }
                }
            }

            return serviceMap;
        }
    }
}
