/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.server.management.api.monitoring.*;
import com.l7tech.server.management.config.monitoring.*;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.monitoring.notification.Notifier;
import com.l7tech.server.processcontroller.monitoring.notification.NotifierFactory;
import com.l7tech.server.processcontroller.monitoring.sampling.PropertySampler;
import com.l7tech.server.processcontroller.monitoring.sampling.PropertySamplerFactory;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.Pair;
import com.l7tech.util.Sets;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives all the monitoring behaviour of the ProcessController.
 */
public class MonitoringKernelImpl implements MonitoringKernel {
    private static final Logger logger = Logger.getLogger(MonitoringKernelImpl.class.getName());

    public static final int MIN_SAMPLING_INTERVAL = 1000;

    private final Timer samplerTimer = new Timer("Monitoring System / Property Sampler", true);
    private final Timer triggerCheckTimer = new Timer("Monitoring System / Trigger Condition Checker", true);
    private final TimerTask triggerCheckTask = new TriggerCheckTask();
    private final Thread notificationThread = new Thread(new NotificationRunner());
    // TODO configurable or heuristic sizes?
    private final ExecutorService notifierThreadPool = new ThreadPoolExecutor(2, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100));

    private volatile Map<MonitorableProperty, PropertyState<?>> currentPropertyStates;
    private volatile Map<MonitorableEvent, EventState> currentEventStates;
    private volatile Map<Long, TriggerState> currentTriggerStates = null;
    private volatile Map<Long, NotificationState> currentNotificationStates = new ConcurrentHashMap<Long, NotificationState>();

    private volatile MonitoringConfiguration currentConfig = null;

    // TODO configurable or heuristic size?
    private final BlockingQueue<NotifiableCondition<?>> notificationQueue = new LinkedBlockingQueue<NotifiableCondition<?>>(500);
    private static final int OUT_OF_TOLERANCE_LOG_INTERVAL = 60000;

    private static abstract class TriggerState<T extends Trigger> {
        private final T trigger;
        private volatile Long outOfTolerance;
        private volatile Long logged;

        private TriggerState(T trigger) {
            this.trigger = trigger;
        }
    }

    private static class EventTriggerState extends TriggerState<EventTrigger> {
        private EventTriggerState(EventTrigger trigger) {
            super(trigger);
        }
    }

    private static class PropertyTriggerState<T extends Serializable & Comparable> extends TriggerState<PropertyTrigger> {
        private final Comparable comparisonValue;

        private PropertyTriggerState(PropertyTrigger trigger) {
            super(trigger);
            final MonitorableProperty prop = BuiltinMonitorables.getBuiltinProperty(trigger.getComponentType(), trigger.getMonitorableId());
            final String tval = trigger.getTriggerValue();
            if (prop == null) {
                comparisonValue = tval;
            } else {
                Class<T> clazz = (Class<T>)prop.getValueClass();
                if (Integer.class.isAssignableFrom(clazz)) {
                    comparisonValue = Integer.valueOf(tval);
                } else if (Long.class.isAssignableFrom(clazz)) {
                    comparisonValue = Long.valueOf(tval);
                } else if (Double.class.isAssignableFrom(clazz)) {
                    comparisonValue = Double.valueOf(tval);
                } else if (String.class.isAssignableFrom(clazz)) {
                    comparisonValue = tval;
                } else if (Enum.class.isAssignableFrom(clazz)) {
                    Class<? extends Enum> eclass = (Class<? extends Enum>) clazz;
                    comparisonValue = Enum.valueOf(eclass, tval);
                } else {
                    logger.log(Level.WARNING, "Can't convert " + prop + " value " + tval + "; using String comparison");
                    comparisonValue = tval;
                }
            }

        }
    }

    @Resource
    private PropertySamplerFactory samplerFactory;

    @Resource
    private NotifierFactory notifierFactory;

    @Resource
    private ConfigService configService;

    @PostConstruct
    public void start() {
        triggerCheckTimer.scheduleAtFixedRate(triggerCheckTask, 10000, 4991); // TODO configurable or heuristic scheduling?
        notificationThread.start();
    }

    @PreDestroy
    public void close() {
        triggerCheckTask.cancel();
    }

    public void setConfiguration(MonitoringConfiguration newConfiguration, boolean doClusterMonitoring) {
        final MonitoringConfiguration oldConfiguration;

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
        if (oldConfiguration == null) {
            logger.info("Accepted initial configuration");
        } else {
            logger.info("Monitoring Configuration has been updated");
        }

        final Map<Long, TriggerState> buildingTstates = new HashMap<Long, TriggerState>();

        // Check for properties that are starting to be monitored or are no longer being monitored
        final Map<MonitorableProperty, PropertyState<?>> buildingPstates = new HashMap<MonitorableProperty, PropertyState<?>>();
        final Map<MonitorableProperty, PropertyState<?>> priorPstates = currentPropertyStates; // getfield once to ensure consistency
        if (priorPstates != null) buildingPstates.putAll(priorPstates);

        final Map<MonitorableEvent, EventState> buildingEstates = new HashMap<MonitorableEvent, EventState>();
        final Map<MonitorableEvent, EventState> priorEstates = currentEventStates;
        if (priorEstates != null) buildingEstates.putAll(priorEstates);

        final Set<MonitorableEvent> liveEvents = new HashSet<MonitorableEvent>();
        final Set<MonitorableProperty> liveProperties = new HashSet<MonitorableProperty>();

        // Initialize/update states for created/updated properties
        for (Map.Entry<Long, Trigger> entry : newTriggers.entrySet()) {
            final Long triggerOid = entry.getKey();
            final Trigger<?> trigger = entry.getValue();

            if (trigger instanceof PropertyTrigger) {
                PropertyTrigger pt = (PropertyTrigger) trigger;
                buildingTstates.put(triggerOid, new PropertyTriggerState(pt)); // TODO inherit anything from predecessor version?
                MonitorableProperty mp = pt.getMonitorable();
                liveProperties.add(mp);
                PropertyState<?> ps = buildingPstates.get(mp);
                Long interval = pt.getMaxSamplingInterval();
                if (interval == null || interval < 0) {
                    interval = 5000L;
                } else if (interval < MIN_SAMPLING_INTERVAL) {
                    interval = (long) MIN_SAMPLING_INTERVAL;
                }
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
                    ps = new PropertyState(mp, pt.getComponentId(), Collections.singleton(triggerOid), interval, sampler);
                } else {
                    // TODO check for compatibility!
                    // TODO don't bother making a new state if the trigger is unchanged
                    //noinspection unchecked
                    ps = new PropertyState(ps, Sets.union(ps.triggerOids, triggerOid), interval);
                }
                buildingPstates.put(mp, ps);
            } else if (trigger instanceof EventTrigger) {
                EventTrigger et = (EventTrigger) trigger;
                buildingTstates.put(triggerOid, new EventTriggerState(et)); // TODO inherit anything from predecessor version?
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
        cancelDeadMonitors(buildingPstates, liveProperties, "properties");
        cancelDeadMonitors(buildingEstates, liveEvents, "events");

        for (PropertyState<?> state : buildingPstates.values()) {
            state.schedule(samplerTimer);
        }

        currentTriggerStates = buildingTstates;
        currentPropertyStates = buildingPstates;

        kickTheSampler();
        kickTheNotifier(); // TODO if any notifications have changed

        this.currentConfig = newConfiguration;
    }

    private static class TransientStatus {
        private final Set<Long> badTriggerOids = new HashSet<Long>();
        private MonitoredStatus.StatusType status = MonitoredStatus.StatusType.OK;
        private Serializable value;
        private Long timestamp;
        public String componentId;
    }

    @Override
    public List<MonitoredPropertyStatus> getCurrentPropertyStatuses() {
        if (currentConfig == null)
            return Collections.emptyList();

        final Map<MonitorableProperty, TransientStatus> stati = new HashMap<MonitorableProperty, TransientStatus>();

        final Map<MonitorableProperty, PropertyState<?>> pstates = currentPropertyStates;
        final Map<Long, TriggerState> tstates = currentTriggerStates;
        final Map<Long, NotificationState> nstates = currentNotificationStates;

        final long now = System.currentTimeMillis();

        for (Map.Entry<MonitorableProperty, PropertyState<?>> entry : pstates.entrySet()) {
            final MonitorableProperty prop = entry.getKey();
            final PropertyState<?> pstate = entry.getValue();
            final Set<Long> toids = pstate.getTriggerOids();
            for (Long toid : toids) {
                final TriggerState tstate = tstates.get(toid);
                if (tstate == null) continue;
                final Trigger<?> trig = tstate.trigger;
                if (!(trig instanceof PropertyTrigger)) continue;

                final PropertyTrigger ptrig = (PropertyTrigger) trig;
                TransientStatus transientStatus = stati.get(prop);
                if (transientStatus == null) {
                    transientStatus = new TransientStatus();
                    transientStatus.componentId = trig.getComponentId();
                    stati.put(prop, transientStatus);
                }

                if (tstate.outOfTolerance != null) {
                    transientStatus.badTriggerOids.add(toid);
                    transientStatus.status = MonitoredStatus.StatusType.WARNING;
                }

                final Pair<Long, ? extends Serializable> lastSample = pstate.getLastSample();
                if (lastSample == null) {
                    logger.info("No sample values available for " + prop);
                    transientStatus.timestamp = now;
                    continue;
                }
                transientStatus.timestamp = transientStatus.timestamp == null || transientStatus.timestamp == 0 ? lastSample.left : Math.min(lastSample.left, transientStatus.timestamp);
                transientStatus.value = lastSample.right;

                if (nstates != null) {
                    for (NotificationRule rule : ptrig.getNotificationRules()) {
                        NotificationState nstate = nstates.get(rule.getOid());
                        if (nstate == null) continue;
                        if (nstate.isNotified()) {
                            transientStatus.status = MonitoredStatus.StatusType.NOTIFIED;
                        }
                    }
                }
            }
        }

        final List<MonitoredPropertyStatus> result = new ArrayList<MonitoredPropertyStatus>();
        for (Map.Entry<MonitorableProperty, TransientStatus> entry : stati.entrySet()) {
            final MonitorableProperty prop = entry.getKey();
            final TransientStatus transientStatus = entry.getValue();
            final Serializable value = transientStatus.value;
            result.add(new MonitoredPropertyStatus(prop, transientStatus.componentId, transientStatus.timestamp, transientStatus.status, transientStatus.badTriggerOids, value == null ? null : value.toString()));
        }

        return result;
    }

    @Override
    public List<NotificationAttempt> getRecentNotificationAttempts(long sinceWhen) {
        List<NotificationAttempt> result = new ArrayList<NotificationAttempt>();
        Map<Long, NotificationState> nstates = currentNotificationStates;
        for (Map.Entry<Long, NotificationState> entry : nstates.entrySet()) {
            Long noid = entry.getKey();
            NotificationState nstate = entry.getValue();
            result.addAll(nstate.getNotificationAttempts(sinceWhen).values());
        }
        return result;
    }

    private <MT extends Monitorable, ST extends MonitorState> void cancelDeadMonitors(Map<MT, ST> states, Set<MT> liveOnes, String what) {
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

        states.keySet().retainAll(liveOnes);
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
                    final PropertyTriggerState ptstate = (PropertyTriggerState) tstate;
                    final PropertyTrigger ptrigger = (PropertyTrigger) tstate.trigger;
                    final MonitorableProperty property = ptrigger.getMonitorable();

                    final PropertyState<?> mstate = pstates.get(property);
                    if (mstate == null) {
                        logger.warning("Couldn't find PropertyState for " + property + " (required by trigger #" + oid + "); skipping");
                        continue;
                    }

                    final Pair<Long, ? extends Comparable> sample = mstate.getLastSample();
                    if (sample == null) continue;
                    
                    final Long when = sample.left;
                    final Comparable what = sample.right;
                    final long sampleAge = now - when;
                    if (sampleAge > (2 * mstate.getSamplingInterval())) {
                        // TODO parameterize max sample age, and/or figure out how/whether to handle failures specially?
                        logger.log(Level.WARNING, "Last sample for " + property + " more than " + sampleAge + "ms old");
                    }
                    mstate.expireHistory(now - (10 * mstate.getSamplingInterval()));

                    // Compare the sampled value
                    final ComparisonOperator op = ptrigger.getOperator();
                    final Comparable rvalue = ptstate.comparisonValue;
                    final Long prevOut = tstate.outOfTolerance;
                    final Long prevLogged = tstate.logged;
                    final String sright = sample.right.toString();
                    if (op.compare(what, rvalue, false)) {
                        if (prevOut != null) {
                            if (now - prevLogged >= OUT_OF_TOLERANCE_LOG_INTERVAL) {
                                // TODO consider suppression of repeated notifications as a configurable aspect of NotificationRules
                                logger.log(Level.INFO, "{0} value {1} is still out of tolerance from {2}ms ago, skipping repeated notification", new Object[] { property, sright, now - prevOut});
                                tstate.logged = now;
                            }
                            continue;
                        }
                        logger.log(Level.INFO, "{0} value {1} is out of tolerance ({2} {3})", new Object[] { property, sright, op, rvalue });
                        tstate.outOfTolerance = now;
                        tstate.logged = now;
                        PropertyCondition cond = conditions.get(property);
                        if (cond == null) {
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), when, Collections.singleton(oid), rvalue));
                        } else {
                            // Merge the previous condition with this one
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), cond.getTimestamp(), Sets.union(cond.getTriggerOids(), oid), cond.getValue()));
                        }
                    } else {
                        if (prevOut != null) {
                            logger.log(Level.INFO, "{0} value {1} is back in tolerance ({2} {3})", new Object[] { property, sright, op, rvalue });
                        } else {
                            logger.log(Level.FINE, "{0} value {1} is in tolerance ({2} {3})", new Object[] { property, sright, op, rvalue });
                        }
                        tstate.outOfTolerance = null;
                    }
                }
            }

            notificationQueue.addAll(conditions.values());
        }
    }


    private class NotificationRunner implements Runnable {
        @Override
        public void run() {
            while(true) {
                try {
                    final NotifiableCondition<?> got = notificationQueue.poll(10000, TimeUnit.MILLISECONDS); // TODO timeout configurable?
                    if (got == null) continue;

                    final Set<Long> triggerOids = got.getTriggerOids();
                    if (triggerOids == null || triggerOids.isEmpty()) {
                        logger.warning("Got a condition with no trigger OIDs");
                        continue;
                    }

                    final Map<Long, TriggerState> tstates = currentTriggerStates;
                    final Map<Long, NotificationState> nstates = currentNotificationStates;
                    if (tstates == null) continue;

                    for (Long triggerOid : triggerOids) {
                        final TriggerState tstate = tstates.get(triggerOid);
                        if (tstate == null) {
                            logger.log(Level.FINE, "Trigger #{0} has been deleted; skipping", triggerOid);
                            continue;
                        }

                        final Trigger<?> trigger = tstate.trigger;

                        final List<NotificationRule> rules = trigger.getNotificationRules();
                        for (final NotificationRule rule : rules) {
                            final Long notificationOid = rule.getOid();
                            NotificationState nstate = nstates.get(notificationOid);
                            if (nstate == null) {
                                nstate = new NotificationState(trigger, rule, 1, TimeUnit.MINUTES);
                                nstates.put(notificationOid, nstate);
                            } else {
                                nstate.condition(got.getTimestamp());
                            }

                            if (!nstate.isOKToFire()) {
                                logger.fine("Suppressing repeated notification for " + rule);
                                continue;
                            }

                            final Notifier notifier;
                            try {
                                notifier = notifierFactory.getNotifier(rule);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Couldn't get notifier for " + rule, e);
                                continue;
                            }

                            if (got instanceof PropertyCondition) {
                                final PropertyCondition pc = (PropertyCondition) got;
                                final NotificationState nstate1 = nstate;
                                notifierThreadPool.submit(new Runnable() {
                                    public void run() {
                                        try {
                                            final NotificationAttempt.StatusType status = notifier.doNotification(got.getTimestamp(), pc.getValue(), trigger);
                                            nstate1.notified(new NotificationAttempt(status, null, System.currentTimeMillis()));
                                        } catch (Exception e) {
                                            logger.log(Level.WARNING, "Couldn't notify for " + rule, e);
                                            nstate1.failed(new NotificationAttempt(e, System.currentTimeMillis()));
                                        }
                                    }
                                });
                            }
                        }
                    }
                    MonitoringKernelImpl.this.currentNotificationStates = nstates;
                } catch (InterruptedException e) {
                    logger.info("Interrupted waiting for notification queue");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
