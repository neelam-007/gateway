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
import java.text.MessageFormat;

/**
 *
 */
public class MetricsUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public MetricsUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                   final UDDIHelper uddiHelper,
                                   final UDDITemplateManager uddiTemplateManager,
                                   final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                   final Aggregator aggregator ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiTemplateManager = uddiTemplateManager;
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
                        uddiHelper,
                        uddiTemplateManager,
                        uddiBusinessServiceStatusManager,
                        aggregator,
                        timerEvent.getRegistryOid() );
            } else if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS_CLEANUP ) {
                task = new MetricsCleanupUDDITask(
                        uddiRegistryManager,
                        uddiHelper,
                        uddiTemplateManager,
                        uddiBusinessServiceStatusManager,
                        timerEvent.getRegistryOid() );
            }
        }

        return task;
    }

    //- PACKAGE

    enum Metric { 
            SERVICE_KEY,
            START_TIME,
            END_TIME,
            TOTAL_REQUEST_COUNT,
            SUCCESS_REQUEST_COUNT,
            FAILURE_REQUEST_COUNT,
            AVERAGE_RESPONSE_TIME,
            MINIMUM_RESPONSE_TIME,
            MAXIMUM_RESPONSE_TIME }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDITemplateManager uddiTemplateManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
    private final Aggregator aggregator;

    /**
     * Task to publish metrics to UDDI.
     */
    private static final class MetricsPublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( MetricsPublishUDDITask.class.getName() );

        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

        private final UDDIRegistryManager uddiRegistryManager;
        private final UDDIHelper uddiHelper;
        private final UDDITemplateManager uddiTemplateManager;
        private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
        private final Aggregator aggregator;
        private final long registryOid;

        MetricsPublishUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                final UDDIHelper uddiHelper,
                                final UDDITemplateManager uddiTemplateManager,
                                final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                final Aggregator aggregator,
                                final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiHelper = uddiHelper;
            this.uddiTemplateManager = uddiTemplateManager;
            this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
            this.aggregator = aggregator;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Publishing metrics to UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() && uddiRegistry.isMetricsEnabled() ) {
                    final UDDITemplate template = uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
                    if ( template == null ) {
                        throw new UDDIException("Template not found for UDDI registry type '"+uddiRegistry.getUddiRegistryType()+"'.");
                    }

                    final long endTime = System.currentTimeMillis();

                    final Collection<UDDIBusinessServiceStatus> toPublish =
                            uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISH );

                    final Collection<UDDIBusinessServiceStatus> toUpdate =
                            uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISHED );

                    final Map<Long,MetricsRequestContext> metricsMap = new HashMap<Long,MetricsRequestContext>();

                    final UDDIClient client = uddiHelper.newUDDIClient( uddiRegistry );

                    for ( UDDIBusinessServiceStatus businessService : toPublish ) {
                        String metricsTModelKey = publishMetrics(
                                template,
                                client,
                                businessService,
                                endTime,
                                metricsMap );

                        if ( metricsTModelKey != null ) {
                            if ( publishMetricsReference( template, client, businessService, metricsTModelKey ) ) {
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
                                template,
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

        private String publishMetrics( final UDDITemplate template,
                                       final UDDIClient client,
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

            for ( UDDITemplate.KeyedReferenceTemplate keyedReferenceTemplate : template.getMetricsKeyedReferences() ) {
                String key = keyedReferenceTemplate.getKey();
                String name = keyedReferenceTemplate.getName();
                String value = keyedReferenceTemplate.getValue();

                if ( value == null && keyedReferenceTemplate.getValueProperty() != null ) {
                    value = getValue( keyedReferenceTemplate.getValueProperty(), serviceKey, startDate, endDate, metricsRequestContext );
                }

                if ( value != null ) {
                    references.add( new UDDIClient.UDDIKeyedReference( key, name, value) );
                }
            }

            String description = null;
            String descriptionFormat = template.getMetricsTModelDescription();
            if ( descriptionFormat != null ) {
                description = MessageFormat.format( descriptionFormat, serviceName );
            }
            try {
                return client.publishTModel( metricsTModelKey,
                                      template.getMetricsTModelName(),
                                      description,
                                      references );
            } catch ( UDDIException ue ) {
                logger.log( Level.WARNING, "Error publishing metrics TModel to UDDI '"+ ExceptionUtils.getMessage( ue )+"'.", ue );
            }

            return null;
        }

        //TODO Do we have an availability metric?
        private String getValue( final String valueProperty,
                                 final String serviceKey,
                                 final String startDate,
                                 final String endDate,
                                 final MetricsRequestContext metricsRequestContext ) {
            String value = null;

            try {
                Metric metric = Metric.valueOf( valueProperty );
                switch ( metric ) {
                    case AVERAGE_RESPONSE_TIME:
                        value = Long.toString(metricsRequestContext.getAvgResponseTime());
                        break;
                    case END_TIME:
                        value = endDate;
                        break;
                    case FAILURE_REQUEST_COUNT:
                        value = Long.toString(metricsRequestContext.getNrFailedRequests());
                        break;
                    case MAXIMUM_RESPONSE_TIME:
                        value = Long.toString(metricsRequestContext.getMaxResponseTime());
                        break;
                    case MINIMUM_RESPONSE_TIME:
                        value = Long.toString(metricsRequestContext.getMinResponseTime());
                        break;
                    case SERVICE_KEY:
                        value = serviceKey;
                        break;
                    case START_TIME:
                        value = startDate;
                        break;
                    case SUCCESS_REQUEST_COUNT:
                        value = Long.toString(metricsRequestContext.getNrSuccessRequests());
                        break;
                    case TOTAL_REQUEST_COUNT:
                        value = Long.toString(metricsRequestContext.getNrRequests());
                        break;
                }
            } catch ( IllegalArgumentException iae ) {
                logger.warning( "Invalid template value '"+valueProperty+"'." );
            }

            return value;
        }

        private boolean publishMetricsReference( final UDDITemplate template,
                                                 final UDDIClient client,
                                                 final UDDIBusinessServiceStatus businessService,
                                                 final String metricsTModelKey ) {
            final String serviceKey = businessService.getUddiServiceKey();

            boolean published = false;
            if ( template.getServiceMetricsKeyedReference() != null ) {
                try {
                    client.addKeyedReference(
                            serviceKey,
                            template.getServiceMetricsKeyedReference().getKey(),
                            template.getServiceMetricsKeyedReference().getName(),
                            metricsTModelKey  );
                    published = true;
                } catch ( UDDIException ue ) {
                    logger.log( Level.WARNING, "Error publishing metrics TModel reference to UDDI '"+ ExceptionUtils.getMessage( ue )+"'.", ue );
                }
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
        private final UDDIHelper uddiHelper;
        private final UDDITemplateManager uddiTemplateManager;
        private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
        private final long registryOid;

        MetricsCleanupUDDITask( final UDDIRegistryManager uddiRegistryManager,
                                final UDDIHelper uddiHelper,
                                final UDDITemplateManager uddiTemplateManager,
                                final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                final long registryOid ) {
            this.uddiRegistryManager = uddiRegistryManager;
            this.uddiHelper = uddiHelper;
            this.uddiTemplateManager = uddiTemplateManager;
            this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) throws UDDIException {
            logger.fine( "Cleanup metrics in UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {  // cleanup even if metrics is not enabled
                    final UDDITemplate template = uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
                    if ( template == null ) {
                        throw new UDDIException("Template not found for UDDI registry type '"+uddiRegistry.getUddiRegistryType()+"'.");
                    }

                    final String referenceKey = template.getServiceMetricsKeyedReference()==null ?
                            null : 
                            template.getServiceMetricsKeyedReference().getKey();
                    if ( referenceKey != null ) {
                        final Collection<UDDIBusinessServiceStatus> toDelete =
                                uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.DELETE );

                        final UDDIClient client = uddiHelper.newUDDIClient( uddiRegistry );

                        for ( UDDIBusinessServiceStatus businessService : toDelete ) {
                            final String serviceKey = businessService.getUddiServiceKey();
                            final String tModelKey = businessService.getUddiMetricsTModelKey();

                            client.removeKeyedReference( serviceKey, referenceKey, null, tModelKey );
                            client.deleteTModel( tModelKey );

                            businessService.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
                            businessService.setUddiPolicyTModelKey( null );
                            uddiBusinessServiceStatusManager.update( businessService );
                        }
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
