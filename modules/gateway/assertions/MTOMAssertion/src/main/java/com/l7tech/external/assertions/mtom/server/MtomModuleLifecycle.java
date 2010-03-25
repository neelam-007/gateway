package com.l7tech.external.assertions.mtom.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.AssertionModuleRegistrationEvent;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class MtomModuleLifecycle implements ApplicationListener {

    //- PUBLIC

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded( final ApplicationContext context ) {
        if (instance != null) {
            logger.log( Level.WARNING, "MTOM module is already initialized");
        } else {
            instance = new MtomModuleLifecycle(context);
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        MtomModuleLifecycle instance = MtomModuleLifecycle.instance;
        if ( instance != null ) {
            logger.log(Level.INFO, "MTOM module is shutting down");
            try {
                instance.destroy();
            } catch (Exception e) {
                logger.log(Level.WARNING, "MTOM module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                MtomModuleLifecycle.instance = null;
            }
        }
    }

    public MtomModuleLifecycle( final ApplicationContext spring ) {
        this.policyCache = (PolicyCache) spring.getBean("policyCache", PolicyCache.class);
        this.applicationEventProxy = (ApplicationEventProxy) spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.applicationEventProxy.addApplicationListener(this);
        this.spring = spring;
        this.mtomDecodePolicy = loadResource(MTOM_POLICY_RESOURCE);
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof LicenseEvent ) {
            handleLicenceEvent();
        } else if ( event instanceof AssertionModuleRegistrationEvent ) {
            if (!initialized()) handleLicenceEvent();
        } else if ( event instanceof Started ) {
            if (!initialized()) handleLicenceEvent();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MtomModuleLifecycle.class.getName());

    private static final String MTOM_POLICY_RESOURCE = "mtom_decode.xml";

    private static MtomModuleLifecycle instance = null;

    private final String mtomDecodePolicy;
    private final PolicyCache policyCache;
    private final ApplicationEventProxy applicationEventProxy;
    private final ApplicationContext spring;
    private String policyGuid;

    private void handleLicenceEvent() {
        if ( isLicensed() ) {
            registerGlobalPolicy();
        } else {
            unregisterGlobalPolicy();
        }
    }

    private boolean initialized() {
        return policyGuid!=null;
    }

    private void destroy() throws Exception {
        instance.unregisterGlobalPolicy();

        if (applicationEventProxy != null)
            applicationEventProxy.removeApplicationListener(this);
    }

    private boolean isLicensed() {
        LicenseManager licMan = (LicenseManager) spring.getBean("licenseManager");
        return licMan.isFeatureEnabled("set:modularAssertions");
    }

    private void registerGlobalPolicy() {
        unregisterGlobalPolicy();

        if ( !isLicensed() ) {
            logger.warning("The MTOM module is not licensed. MTOM will not be available.");
        } else {
            policyGuid = policyCache.registerGlobalPolicy( "MTOM Global Policy", PolicyType.PRE_SECURITY_FRAGMENT, mtomDecodePolicy );
        }
    }

    private void unregisterGlobalPolicy() {
        if ( policyGuid != null ) {
            policyCache.unregisterGlobalPolicy( policyGuid );
            policyGuid = null;
        }
    }

    private String loadResource( final String resource ) {
        try {
            URL url = getClass().getResource(resource);
            if ( url == null ) {
                throw new IllegalStateException( "Missing resource '"+resource+"'." );                
            }
            byte[] bytes = IOUtils.slurpUrl( url );
            return new String(bytes, Charsets.UTF8);
        } catch ( IOException ioe ) {
            throw ExceptionUtils.wrap( ioe );
        }
    }
}
