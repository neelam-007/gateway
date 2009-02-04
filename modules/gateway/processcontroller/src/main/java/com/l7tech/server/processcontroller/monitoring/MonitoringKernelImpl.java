/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.server.management.api.monitoring.Monitorable;
import com.l7tech.server.management.api.monitoring.MonitorableEvent;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.monitoring.notification.NotifierFactory;
import com.l7tech.server.processcontroller.monitoring.sampling.PropertySampler;
import com.l7tech.server.processcontroller.monitoring.sampling.PropertySamplerFactory;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.Pair;
import com.l7tech.util.Sets;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitoringKernelImpl implements MonitoringKernel {
    private static final Logger logger = Logger.getLogger(MonitoringKernelImpl.class.getName());

    private final Timer samplerTimer = new Timer("Monitoring System / Property Sampler", true);
    private final Timer triggerCheckTimer = new Timer("Monitoring System / Trigger Condition Checker", true);
    private final TimerTask triggerCheckTask = new TriggerCheckTask();

    private volatile Map<MonitorableProperty, PropertyState<?>> currentPropertyStates;
    private volatile Map<MonitorableEvent, EventState> currentEventStates;
    private volatile Map<Long, TriggerState> currentTriggerStates = null;

    private volatile MonitoringConfiguration currentConfig = null;

    private final BlockingQueue<NotifiableCondition<?>> notificationQueue = new LinkedBlockingQueue<NotifiableCondition<?>>(500); // TODO configurable or heuristic size?

    private static class TriggerState {
        private final Trigger trigger;

        public TriggerState(Trigger trigger) {
            this.trigger = trigger;
        }
    }

    @Resource
    private PropertySamplerFactory samplerFactory;

    @Resource
    private NotifierFactory notifierFactory;

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
            if (currentPropertyStates != null) {
                for (PropertyState<?> state : currentPropertyStates.values()) {
                    state.close();
                }
                kickTheSampler();
            }

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

        final Map<Long, TriggerState> oldTstate = currentTriggerStates;
        final Map<Long, TriggerState> buildingTstates = new HashMap<Long, TriggerState>();

        // Check for properties that are starting to be monitored or are no longer being monitored
        final Map<MonitorableProperty, PropertyState<?>> buildingPstates = new HashMap<MonitorableProperty, PropertyState<?>>();
        final Map<MonitorableProperty, PropertyState<?>> priorPstates = currentPropertyStates; // getfield once to ensure consistency
        if (priorPstates != null) buildingPstates.putAll(priorPstates);

        final Map<MonitorableEvent, EventState> buildingEstates = new HashMap<MonitorableEvent, EventState>();
        final Map<MonitorableEvent, EventState> priorEstates = currentEventStates;
        if (priorPstates != null) buildingEstates.putAll(priorEstates);

        final Set<MonitorableEvent> liveEvents = new HashSet<MonitorableEvent>();
        final Set<MonitorableProperty> liveProperties = new HashSet<MonitorableProperty>();
        // Initialize/update states for created/updated properties
        for (Map.Entry<Long, Trigger> entry : newTriggers.entrySet()) {
            final Long triggerOid = entry.getKey();
            final Trigger trigger = entry.getValue();
            buildingTstates.put(triggerOid, new TriggerState(trigger)); // TODO inherit anything from predecessor version?
            if (trigger instanceof PropertyTrigger) {
                PropertyTrigger pt = (PropertyTrigger) trigger;
                MonitorableProperty mp = (MonitorableProperty) pt.getMonitorable();
                liveProperties.add(mp);
                PropertyState<?> ps = buildingPstates.get(mp);
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
                    ps = new PropertyState(mp, pt.getComponentId(), Collections.singleton(triggerOid), pt.getMaxSamplingInterval(), sampler);
                } else {
                    // TODO check for compatibility!
                    // TODO don't bother making a new state if the trigger is unchanged
                    //noinspection unchecked
                    ps = new PropertyState(ps, Sets.union(ps.triggerOids, triggerOid), pt.getMaxSamplingInterval());
                }
                buildingPstates.put(mp, ps);
            } else if (trigger instanceof EventTrigger) {
                EventTrigger et = (EventTrigger) trigger;
                MonitorableEvent me = et.getMonitorable();
                liveEvents.add(me);
                EventState es = buildingEstates.get(me);
                if (es == null) {
                    logger.info("Starting to monitor event " + me + " on " + et.getComponentId());
                    es = new EventState(me, et.getComponentId(), Collections.singleton(triggerOid));
                } else {
                    // TODO check for compatibility?
                    es = new EventState(es, Sets.union(es.triggerOids, triggerOid));
                }
                buildingEstates.put(me, es);
            }
        }

        // Forget about the monitorables that are no longer in the config
        cancelDeadProperties(buildingPstates, liveProperties, "properties");
//        cancelDeadProperties(buildingEstates, liveEvents, "events");

        buildingPstates.keySet().retainAll(liveProperties);
        buildingEstates.keySet().retainAll(liveEvents);

        currentTriggerStates = buildingTstates;
        currentPropertyStates = buildingPstates;
        for (PropertyState<?> state : buildingPstates.values()) {
            state.schedule(samplerTimer);
        }

        // TODO do we need to do anything to kick off the event samplers?

        kickTheSampler();

        // TODO apply created NotificationRules
        // TODO apply updated NotificationRules
        // TODO apply deleted NotificationRules

        kickTheNotifier(); // if any notifications have changed
    }

    private <MT extends Monitorable, ST extends MonitorState> void cancelDeadProperties(Map<MT, ST> states, Set<MT> liveOnes, String what) {
        Set<MT> deletes = new HashSet<MT>();
        for (Map.Entry<MT,ST> entry : states.entrySet()) {
            MT mp = entry.getKey();
            ST ps = entry.getValue();
            if (!liveOnes.contains(mp)) {
                logger.fine("Forgetting about property: " + mp);
                deletes.add(mp);
                try {
                    ps.close();
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't close state", e);
                }
            }
        }

        if (!deletes.isEmpty() && logger.isLoggable(Level.INFO)) {
            StringBuilder sb = new StringBuilder("The following " + what + " are no longer being monitored: ");
            for (Iterator<MT> it = deletes.iterator(); it.hasNext();) {
                MT byeProp = it.next();
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

    private class TriggerCheckTask extends TimerTask {
        @Override
        public void run() {
            final long now = System.currentTimeMillis();

            final Map<MonitorableProperty, PropertyState<?>> pstates = currentPropertyStates;
            final Map<Long, TriggerState> tstates = currentTriggerStates;
            if (tstates == null || pstates == null) {
                logger.fine("No configuration; skipping trigger condition check");
                return;
            }

            final Map<MonitorableProperty, PropertyCondition> conditions = new HashMap<MonitorableProperty, PropertyCondition>();
            for (Map.Entry<Long,TriggerState> entry : tstates.entrySet()) {
                final Long oid = entry.getKey();
                final TriggerState tstate = entry.getValue();
                if (tstate.trigger instanceof PropertyTrigger) {
                    final PropertyTrigger<?> ptrigger = (PropertyTrigger) tstate.trigger;
                    final MonitorableProperty property = ptrigger.getMonitorable();

                    final PropertyState<?> mstate = pstates.get(property);
                    if (mstate == null) {
                        logger.warning("Couldn't find PropertyState for " + property + " (required by trigger #" + oid + "); skipping");
                        continue;
                    }

                    final Pair<Long, ? extends Comparable> sample = mstate.getLastSample();
                    final Long when = sample.left;
                    final Comparable what = sample.right;
                    final long sampleAge = now - when;
                    if (sampleAge > (2 * mstate.getSamplingInterval())) {
                        logger.log(Level.WARNING, "Last sample for " + property + " more than " + sampleAge + "ms old");
                    }

                    final ComparisonOperator op = ptrigger.getOperator();
                    final Comparable rvalue = ptrigger.getTriggerValue();
                    if (op.compare(what, rvalue, false)) {
                        logger.log(Level.INFO, property + " is out of tolerance");
                        PropertyCondition cond = conditions.get(property);
                        if (cond == null) {
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), when, Collections.singleton(oid), rvalue));
                        } else {
                            // Merge the previous condition with this one
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), cond.getTimestamp(), Sets.union(cond.getTriggerOids(), oid), cond.getValue()));
                        }
                    }
                }
            }

            notificationQueue.addAll(conditions.values());
        }
    }
}
