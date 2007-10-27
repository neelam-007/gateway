package com.l7tech.server;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.LicenseManager;

/**
 * Abstract base class for components with life cycles.
 *
 * @author Steve Jones
 */
public abstract class LifecycleBean implements InitializingBean, ApplicationContextAware, ApplicationListener, ServerComponentLifecycle, DisposableBean {

    //- PUBLIC

    public void setServerConfig(ServerConfig config) throws LifecycleException {
    }

    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext already initialized!");
        this.applicationContext = applicationContext;
    }

    public boolean isLicensed() {
        boolean licensed = false;

        if ( licenseFeature == null ||
             licenseManager.isFeatureEnabled(licenseFeature)) {
            licensed = true;
        }

        return licensed;
    }

    public final void start() throws LifecycleException {
        if ( isLicensed() ) {
            doStart();
            synchronized (startLock) {
                this.started = true;
            }
        }
    }

    public void stop() throws LifecycleException {
        synchronized (startLock) {
            this.started = false;
        }
        doStop();
    }

    public void close() throws LifecycleException {
        doClose();
    }

    public void destroy() throws Exception {
        stop();
        close();
    }

    public boolean isStarted() {
        synchronized (startLock) {
            return this.started;
        }
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if ( licenseFeature != null ) {
            if (applicationEvent instanceof LicenseEvent) {
                // If the subsystem becomes licensed after bootup, start it now
                // We do not, however, support de-licensing an already-active subsystem without a reboot
                synchronized (startLock) {
                    if (started)
                        return;  //avoid cost of scheduling oneshot timertask if we have already started
                }

                if (isLicensed()) {
                    Background.scheduleOneShot(new TimerTask() {
                        public void run() {
                            try {
                                doStart();
                            } catch (LifecycleException e) {
                                logger.log(Level.SEVERE, "Unable to start subsystem: " + ExceptionUtils.getMessage(e), e);
                            }
                        }
                    }, 250);
                }
            }
        }
    }

    public String toString() {
        return componentName;
    }

    //- PROTECTED

    protected LifecycleBean(final String name,
                            final Logger logger,
                            final String licenseFeature,
                            final LicenseManager licenseManager) {
        this.componentName = name;
        this.logger = logger;
        this.licenseFeature = licenseFeature;
        this.licenseManager = licenseManager;
        synchronized (startLock) {
            this.started = false;
        }
    }

    protected void init(){}
    protected void doStart() throws LifecycleException {}
    protected void doStop() throws LifecycleException {}
    protected void doClose() throws LifecycleException {}

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    //- PRIVATE

    private final Logger logger;
    private final String componentName;
    private final String licenseFeature;
    private final LicenseManager licenseManager;
    private final Object startLock = new Object();
    private boolean started;
    private ApplicationContext applicationContext;
}
