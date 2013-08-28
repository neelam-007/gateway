package com.l7tech.server.util;

import com.l7tech.server.event.admin.AdminError;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.SystemAdminError;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import javax.inject.Inject;
import javax.inject.Provider;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Throws advice that audits exceptions from administrative APIs.
 */
public class AuditingThrowsAdvice implements ThrowsAdvice, PropertyChangeListener, PostStartupApplicationListener {

    //- PUBLIC

    public AuditingThrowsAdvice( final Config config ) {
        this.config = config;
    }

    /**
     * Load the properties after the gateway has started. This is done to avoid circular dependency issues (bug: SSG-7547)
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof Started) {
            reload();
        }
    }

    @SuppressWarnings({ "UnusedParameters" })
    public void afterThrowing( final Method method,
                               final Object[] args,
                               final Object target,
                               final Exception e ) {
        if ( shouldAudit( e ) ) {
            if ( JaasUtils.getCurrentUser() != null ) {
                // admin audit if a user is available
                publisher.get().publishEvent( new AdminError( this, buildMessage( e ) ) );
            } else {
                // system audit otherwise
                publisher.get().publishEvent( new SystemAdminError( this, buildMessage( e ) ) );
            }
        }
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        if ( evt.getPropertyName() != null &&
             evt.getPropertyName().startsWith( PROP_PREFIX ) ) {
            reload();
        }
    }

    //- PACKAGE

    static boolean isIncluded( final Exception e,
                               final boolean defaultInclude,
                               final Collection<Class> includes,
                               final Collection<Class> excludes ) {
        boolean include = defaultInclude;

        final Class mostSpecificInclude = getMostSpecificSuperclass( includes, e );
        final Class mostSpecificExclude = getMostSpecificSuperclass( excludes, e );

        if ( mostSpecificInclude != null && mostSpecificExclude != null ) {
            if ( !mostSpecificInclude.equals( mostSpecificExclude ) ) {
                include = mostSpecificExclude.isAssignableFrom( mostSpecificInclude );
            }
        } else if ( mostSpecificInclude != null ) {
            include = true;
        } else if ( mostSpecificExclude != null ) {
            include = false;
        }

        return include;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( AuditingThrowsAdvice.class.getName() );

    private static final boolean enabled = ConfigFactory.getBooleanProperty( "com.l7tech.server.admin.auditExceptions", true );

    private static final String PROP_PREFIX = "audit.adminExceptions.";
    private static final String PROP_DEFAULT_INCLUDE = PROP_PREFIX + "defaultIncludes";
    private static final String PROP_INCLUDES = PROP_PREFIX + "includes";
    private static final String PROP_EXCLUDES = PROP_PREFIX + "excludes";

    private static final String MESSAGE_PREFIX = "Administrative Error: ";
    private static final int MESSAGE_DETAIL_LENGTH = 255 - MESSAGE_PREFIX.length();

    private final AtomicBoolean defaultInclude = new AtomicBoolean( true );
    private final AtomicReference<Collection<Class>> exceptionIncludeClasses = new AtomicReference<Collection<Class>>( Collections.<Class>emptyList() );
    private final AtomicReference<Collection<Class>> exceptionExcludeClasses = new AtomicReference<Collection<Class>>( Collections.<Class>emptyList() );

    @Inject
    private Provider<ApplicationEventPublisher> publisher;

    private final Config config;

    private void reload() {
        defaultInclude.set( config.getBooleanProperty( PROP_DEFAULT_INCLUDE, true ) );
        buildClasses( exceptionIncludeClasses, config.getProperty( PROP_INCLUDES, "" ) );
        buildClasses( exceptionExcludeClasses, config.getProperty( PROP_EXCLUDES, "" ) );
    }

    private static void buildClasses( final AtomicReference<Collection<Class>> exceptionClasses,
                                      final String exceptionProp ) {
        final List<Class> exceptionList = new ArrayList<Class>();

        final String[] exceptions = exceptionProp.trim().split( "\\s+" );
        for ( final String exception : exceptions ) {
            if ( exception.isEmpty() ) continue;
            try {
                exceptionList.add( Class.forName( exception ) );
            } catch ( ClassNotFoundException e ) {
                logger.log( Level.WARNING, "Ignoring invalid exception class configuration '"+exception+"'." );
            }
        }

        exceptionClasses.set( Collections.unmodifiableCollection( exceptionList ) );
    }

    private boolean shouldAudit( final Exception e ) {
        return enabled &&
                !MySqlFailureThrowsAdvice.isMySqlFailure( e ) &&
                isIncluded( e, defaultInclude.get(), exceptionIncludeClasses.get(), exceptionExcludeClasses.get() );
    }

    private static Class getMostSpecificSuperclass( final Collection<Class> exceptionClasses,
                                                    final Exception e ) {
        Class result = null;

        for ( final Class clazz : exceptionClasses ) {
            if ( clazz.isInstance( e ) && (result==null || result.isAssignableFrom( clazz )) ) {
                result = clazz;
            }
        }

        return result;
    }

    private String buildMessage( final Exception e ) {
        return MESSAGE_PREFIX + TextUtils.truncateStringAtEnd( ExceptionUtils.getMessage( e ), MESSAGE_DETAIL_LENGTH );
    }

}
