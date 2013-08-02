package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ServerConfig;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIInvalidKeyException;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.wsdm.Aggregator;
import com.l7tech.server.wsdm.MetricsRequestContext;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.uddi.UDDIKeyedReference;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 *
 */
public class MetricsUDDITaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public MetricsUDDITaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                   final UDDIHelper uddiHelper,
                                   final UDDITemplateManager uddiTemplateManager,
                                   final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager,
                                   final Aggregator aggregator,
                                   final ClusterPropertyCache clusterPropertyCache ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiTemplateManager = uddiTemplateManager;
        this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
        this.aggregator = aggregator;
        this.clusterPropertyCache = clusterPropertyCache;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof TimerUDDIEvent ) {
            TimerUDDIEvent timerEvent = (TimerUDDIEvent) event;
            if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS_PUBLISH ) {
                task = new MetricsPublishUDDITask(
                        this,
                        timerEvent.getRegistryOid() );
            } else if ( timerEvent.getType()==TimerUDDIEvent.Type.METRICS_CLEANUP ) {
                task = new MetricsCleanupUDDITask(
                        this,
                        timerEvent.getRegistryOid() );
            }
        }

        return task;
    }

    //- PACKAGE

    enum Metric {
            CLUSTER,
            SERVICE_KEY,
            START_TIME,
            END_TIME,
            TOTAL_REQUEST_COUNT,
            SUCCESS_REQUEST_COUNT,
            FAILURE_REQUEST_COUNT,
            AVERAGE_RESPONSE_TIME,
            MINIMUM_RESPONSE_TIME,
            MAXIMUM_RESPONSE_TIME,
            AVAILABILITY }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDITemplateManager uddiTemplateManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;
    private final Aggregator aggregator;
    private final ClusterPropertyCache clusterPropertyCache;

    /**
     * Task to publish metrics to UDDI.
     */
    private static final class MetricsPublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( MetricsPublishUDDITask.class.getName() );

        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

        private final MetricsUDDITaskFactory factory;
        private final long registryOid;
        private final NumberFormat percentFormat = new DecimalFormat("0.0");

        MetricsPublishUDDITask( final MetricsUDDITaskFactory factory,
                                final long registryOid ) {
            this.factory = factory;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Publishing metrics to UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() && uddiRegistry.isMetricsEnabled() ) {
                    final UDDITemplate template = factory.uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
                    if ( template == null ) {
                        throw new UDDIException("Template not found for UDDI registry type '"+uddiRegistry.getUddiRegistryType()+"'.");
                    }

                    final long endTime = System.currentTimeMillis();

                    final Collection<UDDIBusinessServiceStatus> toPublish =
                            factory.uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISH );

                    final Collection<UDDIBusinessServiceStatus> toUpdate =
                            factory.uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.PUBLISHED );

                    final Map<Goid,MetricsRequestContext> metricsMap = new HashMap<Goid,MetricsRequestContext>();

                    UDDIClient client = null;
                    try {
                        client = factory.uddiHelper.newUDDIClient( uddiRegistry );

                        if ( !toPublish.isEmpty() || !toUpdate.isEmpty() ) {
                            // authenticate early to avoid error for every service
                            client.authenticate();
                        }

                        for ( UDDIBusinessServiceStatus businessService : toPublish ) {
                            String metricsTModelKey = publishMetrics(
                                    context,
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
                                        factory.uddiBusinessServiceStatusManager.update( businessService );
                                    } catch (UpdateException e) {
                                        logger.log( Level.WARNING, "Error updating UDDIBusinessServiceStatus", e );
                                    }
                                }
                            }
                        }

                        for ( UDDIBusinessServiceStatus businessService : toUpdate ) {
                            publishMetrics(
                                    context,
                                    template,
                                    client,
                                    businessService,
                                    endTime,
                                    metricsMap );
                        }
                    } finally {
                        ResourceUtils.closeQuietly( client );
                    }
                } else {
                    logger.info( "Ignoring metrics event for UDDI registry (#"+registryOid+"), registry not found or is disabled." );
                }
            } catch (ValueException ve) {
                context.logAndAudit( SystemMessages.UDDI_METRICS_PUBLISH_FAILED, "Error when publishing metrics for registry #"+registryOid+"; "+ExceptionUtils.getMessage(ve));
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_METRICS_PUBLISH_FAILED, e, "Database error when publishing metrics for registry #"+registryOid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_METRICS_PUBLISH_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }

        private String publishMetrics( final UDDITaskContext context,
                                       final UDDITemplate template,
                                       final UDDIClient client,
                                       final UDDIBusinessServiceStatus businessService,
                                       final long endTime,
                                       final Map<Goid,MetricsRequestContext> metricsMap ) throws ValueException {
            final Goid serviceGoid = businessService.getPublishedServiceGoid();
            final String serviceKey = businessService.getUddiServiceKey();
            final String serviceName = businessService.getUddiServiceName();
            final String metricsTModelKey = businessService.getUddiMetricsTModelKey();

            final MetricsRequestContext metricsRequestContext = getMetricsRequestContext( serviceGoid, endTime, metricsMap );
            if ( metricsRequestContext == null ) {
                return null;              
            }
            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            final String startDate = dateFormat.format( new Date(metricsRequestContext.getPeriodStart()) );
            final String endDate = dateFormat.format( new Date(endTime) );

            final Collection<UDDIKeyedReference> references = new ArrayList<UDDIKeyedReference>();

            for ( UDDITemplate.KeyedReferenceTemplate keyedReferenceTemplate : template.getMetricsKeyedReferences() ) {
                String key = keyedReferenceTemplate.getKey();
                String name = keyedReferenceTemplate.getName();
                String value = keyedReferenceTemplate.getValue();

                if ( value == null && keyedReferenceTemplate.getValueProperty() != null ) {
                    value = getValue( keyedReferenceTemplate.getValueProperty(), serviceKey, startDate, endDate, metricsRequestContext );
                }

                if ( value != null ) {
                    references.add( new UDDIKeyedReference( key, name, value) );
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
                context.logAndAudit(SystemMessages.UDDI_METRICS_PUBLISH_TMODEL_ERROR, ExceptionUtils.getDebugException(ue), serviceName, ExceptionUtils.getMessage(ue));
            }

            return null;
        }

        private String getValue( final String valueProperty,
                                 final String serviceKey,
                                 final String startDate,
                                 final String endDate,
                                 final MetricsRequestContext metricsRequestContext ) throws ValueException {
            String value = null;

            String metricsKey = valueProperty;
            String metricsSub = null;
            int sepIndex = valueProperty.indexOf( ':' );
            if ( sepIndex > -1 ) {
                metricsKey = valueProperty.substring( 0, sepIndex );
                metricsSub = valueProperty.substring( sepIndex+1 );
            }

            try {
                Metric metric = Metric.valueOf( metricsKey );
                switch ( metric ) {
                    case AVAILABILITY:
                        value = percentFormat.format(metricsRequestContext.getAvailability());
                        break;
                    case AVERAGE_RESPONSE_TIME:
                        value = Long.toString(metricsRequestContext.getAvgResponseTime());
                        break;
                    case CLUSTER:
                        if ( metricsSub != null && !metricsSub.isEmpty() ) {
                            final ServerConfig serverConfig = ServerConfig.getInstance();
                            final String configName = serverConfig.getNameFromClusterName( metricsSub );
                            if ( configName != null ) {
                                value = serverConfig.getProperty( configName );
                            } else {
                                final ClusterProperty property = factory.clusterPropertyCache.getCachedEntityByName( metricsSub, 30000 );
                                if ( property != null && !property.isHiddenProperty() ) {
                                    value = property.getValue();
                                }
                            }
                        }
                        if ( value == null || value.trim().isEmpty() ) {
                            throw new ValueException("Cluster property value missing or empty ["+metricsSub+"]");
                        }
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
                throw new ValueException( "Invalid value requested ["+valueProperty+"]." );
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
                    logger.log( Level.WARNING, "Error publishing metrics TModel reference to UDDI '"+ ExceptionUtils.getMessage( ue )+"'.", ExceptionUtils.getDebugException(ue) );
                }
            }

            return published;
        }

        private MetricsRequestContext getMetricsRequestContext( final Goid serviceGoid,
                                                                final long endTime,
                                                                final Map<Goid,MetricsRequestContext> metricsMap ) {
            MetricsRequestContext context = metricsMap.get( serviceGoid );

            if ( context == null ) {
                final MetricsSummaryBin bin = factory.aggregator.getMetricsForService(serviceGoid);
                if ( bin != null ) {
                    context = new MetricsRequestContext(bin, true, null, endTime - bin.getStartTime());
                    metricsMap.put( serviceGoid, context );
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

        private final MetricsUDDITaskFactory factory;
        private final long registryOid;

        MetricsCleanupUDDITask( final MetricsUDDITaskFactory factory,
                                final long registryOid ) {
            this.factory = factory;
            this.registryOid = registryOid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Cleanup metrics in UDDI for registry (#"+registryOid+")" );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryOid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {  // cleanup even if metrics is not enabled
                    final UDDITemplate template = factory.uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
                    if ( template == null ) {
                        throw new UDDIException("Template not found for UDDI registry type '"+uddiRegistry.getUddiRegistryType()+"'.");
                    }

                    final String referenceKey = template.getServiceMetricsKeyedReference()==null ?
                            null : 
                            template.getServiceMetricsKeyedReference().getKey();
                    if ( referenceKey != null ) {
                        final Collection<UDDIBusinessServiceStatus> toDelete =
                                factory.uddiBusinessServiceStatusManager.findByRegistryAndMetricsStatus( registryOid, UDDIBusinessServiceStatus.Status.DELETE );

                        UDDIClient client = null;
                        try {
                            client = factory.uddiHelper.newUDDIClient( uddiRegistry );

                            for ( UDDIBusinessServiceStatus businessService : toDelete ) {
                                final String serviceKey = businessService.getUddiServiceKey();
                                final String tModelKey = businessService.getUddiMetricsTModelKey();

                                try {
                                    client.removeKeyedReference( serviceKey, referenceKey, null, tModelKey );
                                } catch ( UDDIInvalidKeyException uike) {
                                    logger.fine( "Service not found when removing metrics reference '"+serviceKey+"'." );
                                }

                                try {
                                    client.deleteTModel( tModelKey );
                                } catch ( UDDIInvalidKeyException uike) {
                                    logger.fine( "Metrics TModel not found for delete '"+tModelKey+"'." );
                                }

                                businessService.setUddiMetricsReferenceStatus( UDDIBusinessServiceStatus.Status.NONE );
                                businessService.setUddiMetricsTModelKey( null );
                                factory.uddiBusinessServiceStatusManager.update( businessService );
                            }
                        } finally {
                            ResourceUtils.closeQuietly( client );
                        }
                    }
                } else {
                    logger.fine( "Ignoring metrics event for UDDI registry (#"+registryOid+"), registry not found or is disabled." );
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_METRICS_CLEANUP_FAILED, e, "Database error when removing metrics for registry #"+registryOid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_METRICS_CLEANUP_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }
    }

    private static final class ValueException extends Exception {
        public ValueException( final String message ) {
            super( message );
        }
    }

}
