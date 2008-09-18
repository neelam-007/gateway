package com.l7tech.server;

import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.LicenseManager;

/**
 * Abstract base class for components with life cycles.
 *
 * @author Steve Jones
 */
public abstract class LifecycleBean implements InitializingBean, ApplicationContextAware, ApplicationListener, ServerComponentLifecycle, DisposableBean {

    //- PUBLIC

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
            startedRwLock.writeLock().lock();
            try {
                this.started = true;
            } finally {
                startedRwLock.writeLock().unlock();
            }
        }
    }

    public void stop() throws LifecycleException {
        startedRwLock.writeLock().lock();
        try {
            this.started = false;
        } finally {
            startedRwLock.writeLock().unlock();
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
        startedRwLock.readLock().lock();
        try {
            return this.started;
        } finally {
            startedRwLock.readLock().unlock();
        }
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if ( licenseFeature != null ) {
            if (applicationEvent instanceof LicenseEvent) {
                // If the subsystem becomes licensed after bootup, start it now
                // We do not, however, support de-licensing an already-active subsystem without a reboot
                startedRwLock.readLock().lock();
                try {
                    if (started)
                        return;  //avoid cost of scheduling oneshot timertask if we have already started
                } finally {
                    startedRwLock.readLock().unlock();
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

        startedRwLock.writeLock().lock();
        try {
            this.started = false;
        } finally {
            startedRwLock.writeLock().unlock();
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
    private boolean started;
    private final ReadWriteLock startedRwLock = new ReentrantReadWriteLock(false);
    private ApplicationContext applicationContext;
}
