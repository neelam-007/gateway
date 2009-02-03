/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.server.management.api.monitoring.MonitorableEvent;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.monitoring.Monitorable;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.Pair;

import javax.annotation.Resource;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;

public class MonitoringKernelImpl implements MonitoringKernel {
    private static final Logger logger = Logger.getLogger(MonitoringKernelImpl.class.getName());

    private final Timer samplerTimer = new Timer("Monitoring System / Property Sampler", true);
    private final Timer triggerCheckTimer = new Timer("Monitoring System / Trigger Condition Checker", true);
    private final TimerTask triggerCheckTask = new TimerTask() {
        @Override
        public void run() {
            final long now = System.currentTimeMillis();

            final MonitoringState<MonitorableProperty, PropertyState<?>> pstates = currentPropertyState;
            final TriggerState tstates = currentTriggerState;
            if (tstates == null || pstates == null) {
                logger.fine("No configuration; skipping trigger condition check");
                return;
            }

            for (Map.Entry<Long, Trigger> entry : tstates.triggers.entrySet()) {
                final Long oid = entry.getKey();
                final Trigger trigger = entry.getValue();
                if (trigger instanceof PropertyTrigger) {
                    final PropertyTrigger<?> ptrigger = (PropertyTrigger) trigger;
                    final MonitorableProperty property = ptrigger.getMonitorable();

                    final PropertyState<?> state = pstates.states.get(property);
                    if (state == null) {
                        logger.warning("Couldn't find PropertyState for " + property + " (required by trigger #" + oid + "); skipping");
                        continue;
                    }

                    Pair<Long, ? extends Serializable> sample = state.getLastSample();
                    ptrigger.getOperator();

                }
            }
        }
    };
    private volatile MonitoringState<MonitorableProperty, PropertyState<?>> currentPropertyState;
    private volatile MonitoringState<MonitorableEvent, EventState> eventState;

    private volatile MonitoringConfiguration currentConfig = null;
    private volatile TriggerState currentTriggerState = null;


    private static class MonitoringState<MT extends Monitorable, ST extends MonitorState> {
        private final Map<MT, ST> states;

        private MonitoringState(Map<MT, ST> states) {
            this.states = states;
        }
    }

    private static class TriggerState {
        private final Map<Long, Trigger> triggers;

        public TriggerState(Map<Long, Trigger> triggers) {
            this.triggers = triggers;
        }
    }

    @Resource
    private PropertySamplerFactory samplerFactory;

    @Resource
    private ConfigService configService;

    void checkConfig() {
        MonitoringConfiguration newConfiguration = configService.getCurrentMonitoringConfiguration();
        final MonitoringConfiguration oldConfiguration;
        final MonitoringConfiguration currentConfig = this.currentConfig;

        // TODO simplfy
        if (newConfiguration != null) {
            if (currentConfig != null) {
                if (currentConfig.equals(newConfiguration)) {
                    logger.fine("Configuration is unchanged");
                    return;
                }

                oldConfiguration = currentConfig;
            } else {
                oldConfiguration = null;
            }
        } else if (currentConfig != null) {
            logger.info("Monitoring configuration has been unset; all monitoring activities will now stop.");
            for (PropertyState<?> entry : currentPropertyState.states.values()) {
                entry.getTask().cancel();
            }
            kickTheSampler();

            // TODO kill the notification tasks
            kickTheNotifier();
            return;
        } else {
            logger.fine("No monitoring configuration is available yet; will check again later");
            return;
        }

        final Map<Long, Trigger> newTriggers = EntityUtil.buildEntityMap(newConfiguration.getTriggers());
        final Map<Long, NotificationRule> newNotificationRules = EntityUtil.buildEntityMap(newConfiguration.getNotificationRules());
        if (oldConfiguration == null) {
            logger.info("Accepted initial configuration");
        } else {
            logger.info("Monitoring Configuration has been updated");
        }

        final TriggerState oldTstate = currentTriggerState;
        final Map<Long, Trigger> liveTriggers = new HashMap<Long, Trigger>();

        // Check for properties that are starting to be monitored or are no longer being monitored
        final Map<MonitorableProperty, PropertyState<?>> pstates = new HashMap<MonitorableProperty, PropertyState<?>>();
        final MonitoringState<MonitorableProperty, PropertyState<?>> mstate = currentPropertyState; // getfield once to ensure consistency
        if (mstate != null)
            pstates.putAll(mstate.states);

        final MonitoringState estate = eventState;
        final Map<MonitorableEvent, EventState> estates = new HashMap<MonitorableEvent, EventState>();
        if (mstate != null) estates.putAll(estate.states);

        final Set<MonitorableEvent> liveEvents = new HashSet<MonitorableEvent>();
        final Set<MonitorableProperty> liveProperties = new HashSet<MonitorableProperty>();
        // Initialize/update states for created/updated properties
        for (Map.Entry<Long, Trigger> entry : newTriggers.entrySet()) {
            final Long oid = entry.getKey();
            final Trigger trigger = entry.getValue();
            liveTriggers.put(oid, trigger);
            if (trigger instanceof PropertyTrigger) {
                PropertyTrigger pt = (PropertyTrigger) trigger;
                MonitorableProperty mp = (MonitorableProperty) pt.getMonitorable();
                liveProperties.add(mp);
                PropertyState<?> ps = pstates.get(mp);
                if (ps == null) {
                    logger.info("Starting to monitor property " + mp + " on " + pt.getComponentId());
                    final PropertySampler<Serializable> sampler;
                    try {
                        sampler = samplerFactory.makeSampler(mp, pt.getComponentId());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Couldn't create sampler for " + mp, e);
                        continue;
                    }
                    //noinspection unchecked
                    ps = new PropertyState(mp, pt.getComponentId(), pt.getMaxSamplingInterval(), sampler);
                } else {
                    // TODO check for compatibility!
                    // TODO don't bother making a new state if the trigger is unchanged
                    //noinspection unchecked
                    ps = new PropertyState(ps, pt.getMaxSamplingInterval());
                }
                pstates.put(mp, ps);
            } else if (trigger instanceof EventTrigger) {
                EventTrigger et = (EventTrigger) trigger;
                MonitorableEvent me = et.getMonitorable();
                liveEvents.add(me);
                EventState es = estates.get(me);
                if (es == null) {
                    logger.info("Starting to monitor event " + me + " on " + et.getComponentId());
                    es = new EventState(me, et.getComponentId());
                } else {
                    // TODO check for compatibility?
                    es = new EventState(es);
                }
                estates.put(me, es);
            }
        }

        // Forget about the monitorables that are no longer in the config
        cancelDeadProperties(pstates, liveProperties, "properties");
//        cancelDeadProperties(estates, liveEvents, "events");

        pstates.keySet().retainAll(liveProperties);
        estates.keySet().retainAll(liveEvents);

        currentTriggerState = new TriggerState(liveTriggers);
        currentPropertyState = new MonitoringState<MonitorableProperty, PropertyState<?>>(pstates);
        for (PropertyState<?> state : pstates.values()) {
            samplerTimer.schedule(state.getTask(), 0, state.getSamplingInterval());
        }

        kickTheSampler();

        // TODO apply created NotificationRules
        // TODO apply updated NotificationRules
        // TODO apply deleted NotificationRules

        kickTheNotifier(); // if any notifications have changed
    }

    private void cancelDeadProperties(Map<MonitorableProperty, PropertyState<?>> pstates, Set<MonitorableProperty> liveProperties, String what) {
        Set<MonitorableProperty> deletes = new HashSet<MonitorableProperty>();
        for (Map.Entry<MonitorableProperty, PropertyState<?>> entry : pstates.entrySet()) {
            MonitorableProperty mp = entry.getKey();
            PropertyState<?> ps = entry.getValue();
            if (!liveProperties.contains(mp)) {
                logger.fine("Forgetting about property: " + mp);
                deletes.add(mp);
                ps.getTask().cancel();
            }
        }

        if (!deletes.isEmpty() && logger.isLoggable(Level.INFO)) {
            StringBuilder sb = new StringBuilder("The following " + what + " are no longer being monitored: ");
            for (Iterator<MonitorableProperty> it = deletes.iterator(); it.hasNext();) {
                MonitorableProperty byeProp = it.next();
                sb.append(byeProp.toString());
                if (it.hasNext()) sb.append(", ");
            }
            logger.log(Level.INFO, sb.toString());
        }
    }

    /**
     * Make the sampling thread(s) aware of changes to the configuration // TODO O RLY
     */
    private void kickTheSampler() {
    }

    /**
     * Make the notification thread(s) aware of changes to the configuration
     */
    private void kickTheNotifier() {
    }
}
