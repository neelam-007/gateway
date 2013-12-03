package com.l7tech.server.util;

import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Non-transactional bean that gathers and re-dispatches application events.
 * This is used to work around a massive performance problem when a transactional bean's onApplicationEvent()
 * handler gets wrapped in expensive Spring TX-checking plumbing, which was otherwise going to cause
 * a 30% performance decrease system-wide between Gateway versions 3.5 and 3.6.
 */
public class ApplicationEventProxy implements ApplicationListener, ApplicationEventPublisher, Ordered {
    final Set<ApplicationListener> subscribers = new CopyOnWriteArraySet<ApplicationListener>();
    final boolean primaryChannel;

    /**
     * Create an ApplicationEventProxy that assumes it is proxying the primary ApplicationEvent channel
     * for the ApplicationContext in which it is created.
     */
    public ApplicationEventProxy() {
        this(true);
    }

    /**
     * Create an ApplicationEventProxy that can proxy either the primary applicaiton event channel or
     * an alternative event channel.
     * <p/>
     * If proxying an alternate channel, the proxy will
     * not deliver events to subscribers if they arrive via {@link #onApplicationEvent};
     * it will only deliver events that are submitted directly to the proxy instance itself
     * via {@link #publishEvent}.
     *
     * @param primaryChannel true if this bean should proxy application events from the main Spring context.
     *                       false if it should only proxy events submitted directly to this bean via publishEvent.
     */
    public ApplicationEventProxy(boolean primaryChannel) {
        this.primaryChannel = primaryChannel;
    }

    /**
     * Add an application listener.  The subscription will persist until the application context is closed,
     * the module to which the subscriber belongs is unloaded, or the subscription is manually removed
     * by a call to {@link #removeApplicationListener(org.springframework.context.ApplicationListener)}.
     *
     * @param listener The ApplicationListener to subscribe to events.
     */
    public void addApplicationListener(ApplicationListener listener) {
        if (listener == null) throw new NullPointerException("listener");
        subscribers.add(listener);
    }

    /**
     * Unsubscribe from application events from this proxy.
     *
     * @param listener The ApplicationListener to unsubscribe from events.
     */
    public void removeApplicationListener(ApplicationListener listener) {
        subscribers.remove(listener);
    }

    /**
     * Check if this proxy is proxying the primary Spring application event channel.
     *
     * @return true iff. this proxy will deliver events to subscribers that arrive via {@link #onApplicationEvent}.
     */
    public boolean isPrimaryChannel() {
        return primaryChannel;
    }

    /**
     * Deliver an event from the primary Spring channel.  This event will only be passed on to subscribers
     * if {@link #isPrimaryChannel()} is true.
     * <p/>
     * <b>You should not normally invoke this method directly.</b> To submit an event to be delivered to all of this proxy's
     * subscribers, use {@link #publishEvent} instead.
     * <p/>
     * Regardless of whether this proxy is providing an alternate event channel, it will still monitor the primary event
     * channel to watch for modules that are unloaded so that it can remove subscribers from that module.
     * <p/>
     *
     * @param event the application event from the primary Spring application event channel. Required.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof AssertionModuleUnregistrationEvent)
            removeListenersFromClassLoader(((AssertionModuleUnregistrationEvent)event).getModule().getModuleClassLoader());

        if (isPrimaryChannel())
            deliverEventToSubscribers(event);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void deliverEventToSubscribers(ApplicationEvent event) {
        for (ApplicationListener applicationListener : subscribers)
            applicationListener.onApplicationEvent(event);
    }

    void removeListenersFromClassLoader(ClassLoader classLoader) {
        List<ApplicationListener> subs = new ArrayList<ApplicationListener>(subscribers);
        List<ApplicationListener> toRemove = new ArrayList<ApplicationListener>();
        for (ApplicationListener sub : subs) {
            if (sub.getClass().getClassLoader() == classLoader)
                toRemove.add(sub);
        }
        subscribers.removeAll(toRemove);
    }

    /**
     * Publish an application event to all subscribers of this proxy.
     *
     * @param event
     */
    @Override
    public void publishEvent(ApplicationEvent event) {
        deliverEventToSubscribers(event);
    }
}
