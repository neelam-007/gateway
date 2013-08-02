package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy.ServiceDocumentResources;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Option;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.memoize;
import static com.l7tech.util.Option.*;

/**
 * Upgrade task that supports WSDL updates for internal services.
 */
abstract class InternalServiceWsdlUpgradeTask implements UpgradeTask {

    //- PUBLIC

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws NonfatalUpgradeException {
        final Config config = applicationContext.getBean( "serverConfig", Config.class );
        final SessionFactory sessionFactory = (SessionFactory)applicationContext.getBean("sessionFactory");
        final Nullary<Option<ServiceDocumentResources>> loader = resourceLoader( config );
        final Option<String> error = new HibernateTemplate(sessionFactory).execute( new HibernateCallback<Option<String>>(){
            @Override
            public Option<String> doInHibernate( final Session session ) throws HibernateException, SQLException {
                final Criteria serviceCriteria = session.createCriteria( PublishedService.class );
                serviceCriteria.add( Restrictions.eq( "internal", true ) );
                serviceCriteria.add( Restrictions.eq( "wsdlUrl", originalWsdlUrl ) );
                for ( final Object serviceObj : serviceCriteria.list() ) {
                    if ( serviceObj instanceof PublishedService ) {
                        final PublishedService publishedService = (PublishedService) serviceObj;
                        logger.info( "Updating WSDL for "+internalServiceDescription+" internal service '"+publishedService.getName()+"' (#"+publishedService.getGoid()+")." );

                        final Option<ServiceDocumentResources> resources = loader.call();
                        if (!resources.isSome())
                            return some("Unable to load service resources, WSDL resources for "+internalServiceDescription+" services not upgraded.");

                        try {
                            publishedService.setWsdlUrl( resources.some().getUri() );
                            publishedService.setWsdlXml( resources.some().getContent() );
                        } catch ( MalformedURLException e ) {
                            return some("Error updating WSDL URL for "+internalServiceDescription+" service: " + ExceptionUtils.getMessage( e ));
                        }

                        final Criteria serviceDocumentCriteria = session.createCriteria( ServiceDocument.class );
                        serviceDocumentCriteria.add( Restrictions.eq( "serviceId", publishedService.getGoid() ) );
                        for ( final Object serviceDocumentObject : serviceDocumentCriteria.list() ) {
                            session.delete( serviceDocumentObject );
                        }

                        for( final ServiceDocument document : resources.some().getDependencies() ) {
                            final ServiceDocument dependency = new ServiceDocument( document, false );
                            dependency.setServiceId( publishedService.getGoid() );
                            session.save( dependency );
                        }
                    }
                }
                return none();
            }
        } );
        if ( error.isSome() ) {
            throw new NonfatalUpgradeException( error.some() );
        }
    }

    //- PACKAGE

    /**
     * Create a new internal service WSDL upgrade task.
     *
     * @param modularAssertionFileName The name of the AAR file (up to the first "-")
     * @param internalServiceDescription A user friendly description of the internal service type.
     * @param urlPrefix The URL prefix for service document WSDL urls
     * @param resourceUrl The relative or absolute URL for the main WSDL resource.
     * @param originalWsdlUrl The absolute URL to match for services being upgraded.
     * @param resourcePrefix The path prefix for new WSDL resources in the classpath.
     */
    InternalServiceWsdlUpgradeTask( final String modularAssertionFileName,
                                    final String internalServiceDescription,
                                    final String urlPrefix,
                                    final String resourceUrl,
                                    final String originalWsdlUrl,
                                    final String resourcePrefix ) {
        this.modularAssertionFileName = modularAssertionFileName;
        this.internalServiceDescription = internalServiceDescription;
        this.urlPrefix = urlPrefix;
        this.originalWsdlUrl = originalWsdlUrl;
        this.resourcePrefix = resourcePrefix;
        this.resourceUrl = resourceUrl;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( InternalServiceWsdlUpgradeTask.class.getName() );

    private final String modularAssertionFileName;
    private final String internalServiceDescription;
    private final String urlPrefix;
    private final String originalWsdlUrl;
    private final String resourcePrefix;
    private final String resourceUrl;

    private Nullary<Option<ServiceDocumentResources>> resourceLoader( final Config config ) {
        return memoize(new Nullary<Option<ServiceDocumentResources>>(){
            @Override
            public Option<ServiceDocumentResources> call() {
                Option<ServiceDocumentResources> resources = none();
                final File modularAssertionDirectory = new File(
                        config.getProperty( "modularAssertionsDirectory", "/opt/SecureSpan/Gateway/runtime/modules/assertions" ) );
                if ( modularAssertionDirectory.isDirectory() ) {
                    final Option<File[]> moduleFiles = optional( modularAssertionDirectory.listFiles( new FilenameFilter() {
                        @Override
                        public boolean accept( final File dir, final String name ) {
                            return name.startsWith( modularAssertionFileName + "-" ) && name.endsWith( ".aar" );
                        }
                    } ) );
                    if ( moduleFiles.isSome() && moduleFiles.some().length == 1 ) {
                        try {
                            URLClassLoader loader = new URLClassLoader( new URL[]{moduleFiles.some()[0].toURI().toURL()}, null );

                            resources = some( ServiceDocumentWsdlStrategy.loadResources(
                                    resourcePrefix,
                                    urlPrefix,
                                    resourceUrl,
                                    loader ) );
                        } catch ( MalformedURLException e ) {
                            logger.log( Level.WARNING, "Error loading updated WSDL resource for upgrading "+internalServiceDescription+" internal services.", e );
                        } catch ( URISyntaxException e ) {
                            logger.log( Level.WARNING, "Error loading updated WSDL resource for upgrading "+internalServiceDescription+" internal services.", e );
                        } catch ( IOException e ) {
                            logger.log( Level.WARNING, "Error loading updated WSDL resource for upgrading "+internalServiceDescription+" internal services.", e );
                        }
                    } else {
                        logger.warning( "Error loading updated WSDL resource for upgrading "+internalServiceDescription+" internal services, found " +moduleFiles.some().length+ " modules." );
                    }
                } else {
                    logger.warning( "Error loading updated WSDL resource for upgrading "+internalServiceDescription+" internal services, modular assertions not found." );
                }

                return resources;
            }
        });
    }
}
