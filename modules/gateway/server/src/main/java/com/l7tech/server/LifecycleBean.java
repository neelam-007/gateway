package com.l7tech.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

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
        if ( this.applicationContext == null ) {
            this.applicationContext = applicationContext;

            final ApplicationEventProxy eventProxy = applicationContext.getBean( "applicationEventProxy", ApplicationEventProxy.class );
            eventProxy.addApplicationListener( new ApplicationListener() {
                @Override
                public void onApplicationEvent( ApplicationEvent event ) {
                    LifecycleBean.this.onApplicationEvent( event );
                }
            } );
        }
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
            startedRwLock.writeLock().lock();
            try {
                if (isStarted())
                    return;

                doStart();

                this.started = true;
            } finally {
                startedRwLock.writeLock().unlock();
            }
        }
    }

    @Override
    public final void stop() throws LifecycleException {
        if (!isStarted())
            return;

        startedRwLock.writeLock().lock();
        try {
            if (!isStarted())
                return;

            doStop();

            this.started = false;
        } finally {
            startedRwLock.writeLock().unlock();
        }
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

    protected LifecycleBean( @NotNull  final String name,
                             @NotNull  final Logger logger,
                             @NotNull  final String licenseFeature,
                             @Nullable final LicenseManager licenseManager) {
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

    protected void setLicenseManager( final LicenseManager licenseManager ) {
        if ( this.licenseManager == null ) {
            this.licenseManager = licenseManager;
        }
    }

    protected void init(){}
    protected void doStart() throws LifecycleException {}
    protected void doStop() throws LifecycleException {}
    protected void doClose() throws LifecycleException {}

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected void onApplicationEvent(final ApplicationEvent applicationEvent) {
        if ( licenseFeature != null ) {
            if (applicationEvent instanceof LicenseChangeEvent) {
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
                    }, 250L);
                }
            } else {
                // Generally lifecycle beans will be explicitly stopped during
                // shutdown. This catches any lifecycle beans that are not in
                // the application context.
                if ( applicationEvent instanceof Stopped ) {
                    try {
                        stop();
                    } catch (LifecycleException e) {
                        logger.log(Level.SEVERE, "Unable to stop subsystem: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        }
    }

    //- PRIVATE

    private final Logger logger;
    private final String componentName;
    private final String licenseFeature;
    private boolean started;
    private final ReadWriteLock startedRwLock = new ReentrantReadWriteLock(false);
    private ApplicationContext applicationContext;
    private LicenseManager licenseManager;
}
