package com.l7tech.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for components with life cycles.
 *
 * @author Steve Jones
 */
public abstract class LifecycleBean implements Lifecycle, InitializingBean, ApplicationContextAware, ServerComponentLifecycle, DisposableBean {

    //- PUBLIC

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext!=null) throw new IllegalStateException("applicationContext already initialized!");
        this.applicationContext = applicationContext;

        ApplicationEventMulticaster eventMulticaster = applicationContext.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
        eventMulticaster.addApplicationListener( new ApplicationListener(){
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                LifecycleBean.this.onApplicationEvent( event );
            }
        } );
    }

    public boolean isLicensed() {
        boolean licensed = false;

        if ( licenseFeature == null ||
             licenseManager.isFeatureEnabled(licenseFeature)) {
            licensed = true;
        }

        return licensed;
    }

    @Override
    public final void start() throws LifecycleException {
        if (isStarted())
            return;

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

    @Override
    public void stop() throws LifecycleException {
        if (!isStarted())
            return;

        startedRwLock.writeLock().lock();
        try {
            this.started = false;
        } finally {
            startedRwLock.writeLock().unlock();
        }
        doStop();
    }

    @Override
    public void close() throws LifecycleException {
        doClose();
    }

    @Override
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

    @Override
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

    protected void onApplicationEvent(ApplicationEvent applicationEvent) {
        if ( licenseFeature != null ) {
            if (applicationEvent instanceof LicenseEvent) {
                // If the subsystem becomes licensed after bootup, start it now
                // We do not, however, support de-licensing an already-active subsystem without a reboot
                if (isStarted())
                    return;  //avoid cost of scheduling oneshot timertask if we have already started

                if (isLicensed()) {
                    Background.scheduleOneShot(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                start();
                            } catch (LifecycleException e) {
                                logger.log(Level.SEVERE, "Unable to start subsystem: " + ExceptionUtils.getMessage(e), e);
                            }
                        }
                    }, 250);
                }
            }
        }
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
