package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.Goid;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIInvalidKeyException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import java.util.logging.Logger;
import java.util.Collection;
import java.text.MessageFormat;

/**
 * UDDI task factory for WS-Policy Attachment tasks.
 */
public class WsPolicyAttachmentTaskFactory extends UDDITaskFactory {

    //- PUBLIC

    public WsPolicyAttachmentTaskFactory( final UDDIRegistryManager uddiRegistryManager,
                                          final UDDIHelper uddiHelper,
                                          final UDDITemplateManager uddiTemplateManager,
                                          final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager ) {
        this.uddiRegistryManager = uddiRegistryManager;
        this.uddiHelper = uddiHelper;
        this.uddiTemplateManager = uddiTemplateManager;
        this.uddiBusinessServiceStatusManager = uddiBusinessServiceStatusManager;
    }

    @Override
    public UDDITask buildUDDITask( final UDDIEvent event ) {
        UDDITask task = null;

        if ( event instanceof WsPolicyUDDIEvent ) {
            WsPolicyUDDIEvent wsPolicyEvent = (WsPolicyUDDIEvent) event;
            task = new WsPolicyPublishUDDITask(
                    this,
                    wsPolicyEvent.getRegistryGoid() );
        }

        return task;
    }

    //- PRIVATE

    private final UDDIRegistryManager uddiRegistryManager;
    private final UDDIHelper uddiHelper;
    private final UDDITemplateManager uddiTemplateManager;
    private final UDDIBusinessServiceStatusManager uddiBusinessServiceStatusManager;

    /**
     * Task to publish ws-policy attachments to UDDI.
     */
    private static final class WsPolicyPublishUDDITask extends UDDITask {
        private static final Logger logger = Logger.getLogger( WsPolicyPublishUDDITask.class.getName() );
        private static final String PROP_WS_POLICY_NAME = "com.l7tech.server.uddi.defaultWsPolicyName";
        private static final String PROP_WS_POLICY_DESCRIPTION = "com.l7tech.server.uddi.defaultWsPolicyDescription";
        private static final String DEFAULT_NAME = "{0}";
        private static final String DEFAULT_DESCRIPTION = "Associated Policy";

        private final WsPolicyAttachmentTaskFactory factory;
        private final Goid registryGoid;

        WsPolicyPublishUDDITask( final WsPolicyAttachmentTaskFactory factory,
                                 final Goid registryGoid ) {
            this.factory = factory;
            this.registryGoid = registryGoid;
        }

        @Override
        public void apply( final UDDITaskContext context ) {
            logger.fine( "Updating ws-policy attachments in UDDI for registry (#"+registryGoid+")" );
            try {
                UDDIRegistry uddiRegistry = factory.uddiRegistryManager.findByPrimaryKey( registryGoid );
                if ( uddiRegistry != null && uddiRegistry.isEnabled() ) {
                    final UDDITemplate template = factory.uddiTemplateManager.getUDDITemplate( uddiRegistry.getUddiRegistryType() );
                    if ( template == null ) {
                        throw new UDDIException("Template not found for UDDI registry type '"+uddiRegistry.getUddiRegistryType()+"'.");
                    }

                    final Collection<UDDIBusinessServiceStatus> toPublish =
                            factory.uddiBusinessServiceStatusManager.findByRegistryAndWsPolicyPublishStatus( registryGoid, UDDIBusinessServiceStatus.Status.PUBLISH );

                    final Collection<UDDIBusinessServiceStatus> toDelete =
                            factory.uddiBusinessServiceStatusManager.findByRegistryAndWsPolicyPublishStatus( registryGoid, UDDIBusinessServiceStatus.Status.DELETE );

                    UDDIClient client = null;
                    try {
                        client = factory.uddiHelper.newUDDIClient( uddiRegistry );

                        if ( !toPublish.isEmpty() || !toDelete.isEmpty() ) {
                            // authenticate early to avoid error for every service
                            client.authenticate();
                        }

                        for ( UDDIBusinessServiceStatus businessService : toPublish ) {
                            final String policyUrl = businessService.getUddiPolicyPublishUrl();
                            final String name = getWsPolicyName(template, businessService.getUddiServiceName());
                            final String desc = getWsPolicyDescription(template, businessService.getUddiServiceName());
                            String tModelKey;

                            try {
                                tModelKey = client.publishPolicy(
                                        businessService.getUddiPolicyTModelKey(),
                                        name,
                                        desc,
                                        policyUrl );
                            } catch ( UDDIInvalidKeyException uike) {
                                // original policy was deleted, so publish a new one
                                tModelKey = client.publishPolicy(
                                        null,
                                        name,
                                        desc,
                                        policyUrl );
                            }

                            client.referencePolicy(
                                    businessService.getUddiServiceKey(),
                                    tModelKey,
                                    null,
                                    desc,
                                    Boolean.FALSE );

                            businessService.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.PUBLISHED );
                            businessService.setUddiPolicyUrl( policyUrl );
                            businessService.setUddiPolicyPublishUrl( null );
                            businessService.setUddiPolicyTModelKey( tModelKey );
                            factory.uddiBusinessServiceStatusManager.update( businessService );
                        }

                        for ( UDDIBusinessServiceStatus businessService : toDelete ) {
                            if ( businessService.getUddiPolicyTModelKey()==null ) {
                                try {
                                    // then it was a remote reference
                                    client.removePolicyReference(
                                            businessService.getUddiServiceKey(),
                                            null,
                                            businessService.getUddiPolicyUrl() );
                                } catch ( UDDIInvalidKeyException uike) {
                                    logger.fine( "Service not found when removing policy reference '"+businessService.getUddiServiceKey()+"'." );
                                }
                            } else {
                                // it was a local reference, delete reference and TModel
                                try {
                                    client.removePolicyReference(
                                            businessService.getUddiServiceKey(),
                                            businessService.getUddiPolicyTModelKey(),
                                            null );
                                } catch ( UDDIInvalidKeyException uike) {
                                    logger.fine( "Service not found when removing policy reference '"+businessService.getUddiServiceKey()+"'." );
                                }

                                try {
                                    client.deleteTModel( businessService.getUddiPolicyTModelKey() );
                                } catch ( UDDIInvalidKeyException uike) {
                                    logger.fine( "WS-Policy TModel not found for delete '"+businessService.getUddiPolicyTModelKey()+"'." );
                                }
                            }

                            businessService.setUddiPolicyStatus( UDDIBusinessServiceStatus.Status.NONE );
                            businessService.setUddiPolicyUrl( null );
                            businessService.setUddiPolicyTModelKey( null );
                            factory.uddiBusinessServiceStatusManager.update( businessService );
                        }
                    } finally {
                        ResourceUtils.closeQuietly( client );
                    }
                } else {
                    logger.info( "Ignoring ws-policy event for UDDI registry (#"+registryGoid+"), registry not found or is disabled." );
                }
            } catch (ObjectModelException e) {
                context.logAndAudit( SystemMessages.UDDI_WSPOLICY_PUBLISH_FAILED, e, "Database error when publishing ws-policy for registry #"+registryGoid+".");
            } catch (UDDIException ue) {
                context.logAndAudit(SystemMessages.UDDI_WSPOLICY_PUBLISH_FAILED, ExceptionUtils.getDebugException(ue), ExceptionUtils.getMessage(ue));
            }
        }
        
        private String getWsPolicyName( final UDDITemplate template,
                                        final String serviceName ) {
            String name = null;

            String nameFormat = template.getPolicyTModelName();
            if ( nameFormat == null ) {
                nameFormat = ConfigFactory.getProperty( PROP_WS_POLICY_NAME, DEFAULT_NAME );
            }
            if ( nameFormat != null ) {
                name = MessageFormat.format( nameFormat, serviceName );
            }

            return name;
        }

        private String getWsPolicyDescription( final UDDITemplate template,
                                               final String serviceName ) {
            String description = null;

            String descriptionFormat = template.getPolicyTModelDescription();
            if ( descriptionFormat == null ) {
                descriptionFormat = ConfigFactory.getProperty( PROP_WS_POLICY_DESCRIPTION, DEFAULT_DESCRIPTION );
            }
            if ( descriptionFormat != null ) {
                description = MessageFormat.format( descriptionFormat, serviceName );
            }

            return description;
        }

    }
}
