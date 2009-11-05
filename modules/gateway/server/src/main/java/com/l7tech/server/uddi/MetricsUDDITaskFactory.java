package com.l7tech.server.uddi;

import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.server.wsdm.MetricsRequestContext;
import com.l7tech.util.ExceptionUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 *
 */
public class MetricsUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public MetricsUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                   final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                   final Aggregator aggregator ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
        this.aggregator = aggregator;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS_PUBLISH ) {
                task = new MetricsPublishUDDITask(
                        uddiRegistryManager,
                        uddiBusinessServiceStatusManager,
                        aggregator,
                        timerEvent.getRegistryOid() );
            } else if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS_CLEANUP ) {
                task = new MetricsCleanupUDDITask(
                        uddiRegistryManager,
                        uddiBusinessServiceStatusManager,
                        timerEvent.getRegistryOid() );
            }
        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
    private final Aggregator aggregator;

    /**
     * Task to publish metrics to UDDI.
     */
    private static final class MetricsPublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( MetricsPublishUDDITask.class.getName() );

        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
        private final Aggregator aggregator;
        private final long registryOid;

        MetricsPublishUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                final Aggregator aggregator,
                                final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
            this.aggregator = aggregator;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Publishing metrics to UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    final long endTime = System.currentTimeMillis();

                    final Collection<UDDIBusinessServiceStatus> toPublish =
                            uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISH );

                    final Collection<UDDIBusinessServiceStatus> toUpdate =
                            uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISHED );

                    final Map<Long,MetricsRequestContext> metricsMap = new HashMap<Long,MetricsRequestContext>();

                    final UDDIClient client = UDDIHelper.newUDDIClient( uddiRegistry );

                    for ( UDDIBusinessServiceStatus businessService : toPublish ) {
                        String metricsTModelKey = publishMetrics(
                                client,
                                businessService,
                                endTime,
                                metricsMap );

                        if ( metricsTModelKey != null ) {
                            if ( publishMetricsReference( client, businessService, metricsTModelKey ) ) {
                                businessService.setUddiMetricsTModelKey( metricsTModelKey );
                                businessService.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.PUBLISHED );
                                try {
                                    uddiBusinessServiceStatusManager.update( businessService );
                                } catch (UpdateException e) {
                                    logger.log( Level.WARNING, "Error updating UDDIBusinessServiceStatus", e );
                                }
                            }
                        }
                    }

                    for ( UDDIBusinessServiceStatus businessService : toUpdate ) {
                        publishMetrics(
                                client,
                                businessService,
                                endTime,
                                metricsMap );
                    }
                } else {
                    logger.info( "Ignoring metrics event for UDDI registry (#"+registryOid+"), registry not found or is disabled." );
                }
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing UDDIRegistry", e );
            }
        }

        private String publishMetrics( final UDDIClient client,
                                       final UDDIBusinessServiceStatus businessService,
                                       final long endTime,
                                       final Map<Long,MetricsRequestContext> metricsMap ) {
            final long serviceOid = businessService.getPublishedServiceOid();
            final String serviceKey = businessService.getUddiServiceKey();
            final String serviceName = businessService.getUddiServiceName();
            final String metricsTModelKey = businessService.getUddiMetricsTModelKey();

            final MetricsRequestContext metricsRequestContext = getMetricsRequestContext( serviceOid, endTime, metricsMap );
            if ( metricsRequestContext == null ) {
                return null;              
            }
            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            final String startDate = dateFormat.format( new Date(metricsRequestContext.getPeriodStart()) );
            final String endDate = dateFormat.format( new Date(endTime) );

            final Collection<UDDIClient.UDDIKeyedReference> references = new ArrayList<UDDIClient.UDDIKeyedReference>();

            //TODO [steve] CentraSite specific values should come from UDDI configuration

            // metadata
            references.add( new UDDIClient.UDDIKeyedReference("uddi:474e1904-69ad-9a6b-11c2-fc8e41c64350", "Metrics", "Metrics") ); //TODO [steve] is this a constant?
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:service.key", "Service Key", serviceKey) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:target", "Target", "Target") ); //TODO [steve] what is this??
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:start.datetime", "Start Time", startDate) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:end.datetime", "End Time", endDate) );

            // metrics
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:total.request.count", "Count of hits", Long.toString(metricsRequestContext.getNrRequests())) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:success.request.count", "Count of successful hits", Long.toString(metricsRequestContext.getNrSuccessRequests())) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:fault.request.count", "Count of fault hits", Long.toString(metricsRequestContext.getNrFailedRequests())) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:average.response.time", "Average response time", Long.toString(metricsRequestContext.getAvgResponseTime())) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:minimum.response.time", "Minimum response time", Long.toString(metricsRequestContext.getMinResponseTime())) );
            references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:maximum.response.time", "Maximum response time", Long.toString(metricsRequestContext.getMaxResponseTime())) );
            //references.add( new UDDIClient.UDDIKeyedReference("uddi:centrasite.com:management:metrics:availability", "Availability", "100") ); //TODO [steve] do we have an availability metric?

            try {
                return client.publishTModel( metricsTModelKey,
                                      "Metrics",
                                      "Metrics for " + serviceName,
                                      references );
            } catch ( UDDIException ue ) {
                logger.log( Level.WARNING, "Error publishing metrics TModel to UDDI '"+ ExceptionUtils.getMessage( ue )+"'.", ue );
            }

            return null;
        }

        private boolean publishMetricsReference( final UDDIClient client,
                                                 final UDDIBusinessServiceStatus businessService,
                                                 final String metricsTModelKey ) {
            final String serviceKey = businessService.getUddiServiceKey();

            boolean published = false;
            try {
                client.addKeyedReference( //TODO [steve] CentraSite specific values should come from UDDI configuration
                        serviceKey,
                        "uddi:centrasite.com:management:metrics:reference",
                        "Metrics",
                        metricsTModelKey  );
                published = true;
            } catch ( UDDIException ue ) {
                logger.log( Level.WARNING, "Error publishing metrics TModel reference to UDDI '"+ ExceptionUtils.getMessage( ue )+"'.", ue );
            }

            return published;
        }

        private MetricsRequestContext getMetricsRequestContext( final long serviceOid,
                                                                final long endTime,
                                                                final Map<Long,MetricsRequestContext> metricsMap ) {
            MetricsRequestContext context = metricsMap.get( serviceOid );

            if ( context == null ) {
                final MetricsSummaryBin bin = aggregator.getMetricsForService(serviceOid);
                if ( bin != null ) {
                    context = new MetricsRequestContext(bin, true, null, endTime - bin.getStartTime());
                    metricsMap.put( serviceOid, context );
                }
            }

            return context;
        }
    }

    /**
     * Task to cleanup metrics in UDDI.
     */
    private static final class MetricsCleanupUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( MetricsCleanupUDDITask.class.getName() );

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
        private final long registryOid;

        MetricsCleanupUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Cleanup metrics in UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    final Collection<UDDIBusinessServiceStatus> toDelete =
                            uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.DELETE );

                    final UDDIClient client = UDDIHelper.newUDDIClient( uddiRegistry );

                    for ( UDDIBusinessServiceStatus businessService : toDelete ) {
                        final String serviceKey = businessService.getUddiServiceKey();
                        final String tModelKey = businessService.getUddiMetricsTModelKey();

                        //TODO [steve] CentraSite specific values should come from UDDI configuration
                        client.removeKeyedReference( serviceKey, "uddi:centrasite.com:management:metrics:reference", null, tModelKey );
                        client.deleteTModel( tModelKey );

                        businessService.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
                        businessService.setUddiPolicyTModelKey( null );
                        uddiBusinessServiceStatusManager.update( businessService );
                    }
                } else {
                    logger.info( "Ignoring metrics event for UDDI registry (#"+registryOid+"), registry not found or is disabled." );
                }
            } catch (ObjectModelException e) {
                throw new UDDIException("Error in metrics cleanup task.", e );
            }
        }
    }
}
