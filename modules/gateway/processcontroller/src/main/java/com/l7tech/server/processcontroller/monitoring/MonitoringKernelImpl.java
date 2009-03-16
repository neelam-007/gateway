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
import com.l7tech.server.processcontroller.monitoring.sampling.PropertySamplingException;
import com.l7tech.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives all the monitoring behaviour of the ProcessController.
 */
public class MonitoringKernelImpl implements MonitoringKernel {
    private static final Logger logger = Logger.getLogger(MonitoringKernelImpl.class.getName());

    private static final int MIN_SAMPLING_INTERVAL = 1000;
    private static final int TOLERANCE_LOG_INTERVAL = 60000;

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

    private static abstract class TriggerState<T extends Trigger> {
        private final T trigger;
        private volatile Tolerance tolerance;

        private TriggerState(T trigger, Tolerance tolerance) {
            this.trigger = trigger;
            this.tolerance = tolerance;
        }
    }

    private static class EventTriggerState extends TriggerState<EventTrigger> {
        private EventTriggerState(EventTrigger trigger, Tolerance tolerance) {
            super(trigger, tolerance);
        }
    }

    private static class PropertyTriggerState<T extends Serializable & Comparable> extends TriggerState<PropertyTrigger> {
        private final Comparable comparisonValue;

        private PropertyTriggerState(PropertyTrigger trigger, Tolerance tolerance) {
            super(trigger, tolerance);
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

    public synchronized void setConfiguration(MonitoringConfiguration newConfiguration) {
        if (newConfiguration == null) {
            logger.info("setConfiguration(null)");
        } else {
            logger.info("setConfiguration" + CollectionUtils.mkString(newConfiguration.getTriggers(), "(", ", ", ")", new Functions.Unary<String, Trigger>() {
                @Override
                public String call(Trigger trigger) {
                    return trigger.getMonitorable().toString();
                }
            }));
        }

        if (newConfiguration != null) {
            if (currentConfig != null) {
                if (currentConfig.equals(newConfiguration)) {
                    logger.info("Configuration is unchanged");
                    return;
                } else {
                    logger.info("Monitoring Configuration has been updated");
                }
            }
        } else if (currentConfig != null) {
            logger.info("Monitoring configuration has been unset; all monitoring activities will now stop.");

            notificationQueue.clear();

            if (currentTriggerStates != null) currentTriggerStates.clear();
            if (currentNotificationStates != null) currentNotificationStates.clear();

            if (currentPropertyStates != null) {
                for (PropertyState<?> state : currentPropertyStates.values()) {
                    logger.info("Stopping monitoring of " + state.monitorable);
                    state.close();
                }
                currentPropertyStates.clear();
            }

            if (currentEventStates != null) {
                for (EventState state : currentEventStates.values()) {
                    logger.info("Stopping monitoring of " + state.monitorable);
                    state.close();
                }
                currentEventStates.clear();
            }

            return;
        } else {
            logger.info("No monitoring configuration is available yet; will check again later");
            return;
        }

        final Map<Long, Trigger> newTriggers = EntityUtil.buildEntityMap(newConfiguration.getTriggers());
        final Map<Long, TriggerState> priorTstates = currentTriggerStates;
        final Map<Long, TriggerState> buildingTstates = new HashMap<Long, TriggerState>();

        // Check for properties that are starting to be monitored or are no longer being monitored
        final Map<MonitorableProperty, PropertyState<?>> buildingPstates = new HashMap<MonitorableProperty, PropertyState<?>>();
        final List<PropertyState> newPstates = new ArrayList<PropertyState>();
        final Map<MonitorableProperty, PropertyState<?>> priorPstates = currentPropertyStates; // getfield once to ensure consistency
        if (priorPstates != null) buildingPstates.putAll(priorPstates);

        final Map<MonitorableEvent, EventState> buildingEstates = new HashMap<MonitorableEvent, EventState>();
        final Map<MonitorableEvent, EventState> priorEstates = currentEventStates;
        if (priorEstates != null) buildingEstates.putAll(priorEstates);

        final Map<Monitorable, Set<Long>> triggerOids = new HashMap<Monitorable, Set<Long>>();

        final Set<MonitorableEvent> liveEvents = new HashSet<MonitorableEvent>();
        final Set<MonitorableProperty> liveProperties = new HashSet<MonitorableProperty>();

        // Initialize/update states for created/updated properties
        for (Map.Entry<Long, Trigger> entry : newTriggers.entrySet()) {
            final Long triggerOid = entry.getKey();
            final Trigger<?> trigger = entry.getValue();
            final TriggerState priorTs = priorTstates == null ? null : priorTstates.get(triggerOid);

            Set<Long> oids = triggerOids.get(trigger.getMonitorable());
            if (oids == null) {
                oids = new HashSet<Long>();
                triggerOids.put(trigger.getMonitorable(), oids);
            }
            oids.add(triggerOid);

            if (trigger instanceof PropertyTrigger) {
                PropertyTrigger pt = (PropertyTrigger) trigger;
                buildingTstates.put(triggerOid, new PropertyTriggerState(pt, priorTs == null ? null : priorTs.tolerance));
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
                    logger.info("Initializing monitoring state for " + mp);
                    final PropertySampler<Serializable> sampler;
                    try {
                        sampler = samplerFactory.makeSampler(mp, pt.getComponentId());
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Couldn't create sampler for " + mp, e);
                        continue;
                    }
                    //noinspection unchecked
                    ps = new PropertyState(mp, pt.getComponentId(), interval, sampler);
                    newPstates.add(ps);
                } else {
                    if (ps.getSamplingInterval() != interval) {
                        // TODO reschedule?
                    }
                }
                buildingPstates.put(mp, ps);
            } else if (trigger instanceof EventTrigger) {
                EventTrigger et = (EventTrigger) trigger;
                buildingTstates.put(triggerOid, new EventTriggerState(et, priorTs == null ? null : priorTs.tolerance));
                MonitorableEvent me = et.getMonitorable();
                liveEvents.add(me);
                EventState es = buildingEstates.get(me);
                if (es == null) {
                    logger.info("Starting to monitor event " + me + " on " + et.getComponentId());
                    es = new EventState(me, et.getComponentId());
                } else {
                    // TODO check for compatibility?
                }
                buildingEstates.put(me, es);
            }
        }

        for (Map.Entry<Monitorable, Set<Long>> entry : triggerOids.entrySet()) {
            Monitorable mon = entry.getKey();
            Set<Long> oids = entry.getValue();
            if (mon instanceof MonitorableProperty) {
                MonitorableProperty property = (MonitorableProperty) mon;
                PropertyState<?> pstate = buildingPstates.get(property);
                if (pstate == null) {
                    logger.warning("Missing property state for " + mon);
                    continue;
                }

                pstate.setTriggerOids(oids);
            } else if (mon instanceof MonitorableEvent) {
                MonitorableEvent event = (MonitorableEvent) mon;

                EventState estate = buildingEstates.get(event);
                if (estate == null) {
                    logger.warning("Missing event state for " + mon);
                    continue;
                }

                estate.setTriggerOids(oids);
            }
        }

        // Forget about the monitorables that are no longer in the config
        cancelDeadMonitors(buildingPstates, liveProperties, "properties");
        cancelDeadMonitors(buildingEstates, liveEvents, "events");

        for (PropertyState<?> state : newPstates) {
            logger.log(Level.INFO, "Starting property sampler for " + state.monitorable);
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
        private MonitoredPropertyStatus.ValueType valueType;
        private Serializable value;
        private Long timestamp;
        public String componentId;
    }

    @Override
    public List<MonitoredPropertyStatus> getCurrentPropertyStatuses() {
        if (currentConfig == null)
            return Collections.emptyList();

        final Map<MonitorableProperty, TransientStatus> stati = new HashMap<MonitorableProperty, TransientStatus>();

        final Map<MonitorableProperty, PropertyState<?>> pstates;
        final Map<Long, TriggerState> tstates;
        final Map<Long, NotificationState> nstates;
        synchronized (MonitoringKernelImpl.this) {
            pstates = currentPropertyStates;
            tstates = currentTriggerStates;
            nstates = currentNotificationStates;
        }

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

                if (tstate.tolerance != null && tstate.tolerance.inOut == InOut.OUT) {
                    transientStatus.badTriggerOids.add(toid);
                    transientStatus.status = MonitoredStatus.StatusType.WARNING;
                }

                final Pair<Long, ? extends Serializable> lastSample = pstate.getLastSample();
                if (lastSample == null) {
                    logger.fine("No sample values available for " + prop);
                    transientStatus.valueType = MonitoredPropertyStatus.ValueType.NO_DATA_YET;
                    transientStatus.timestamp = now;
                    continue;
                } else {
                    Pair<Long, PropertySamplingException> lastFailure = pstate.getLastFailure();
                    if (lastFailure != null && lastFailure.left > lastSample.left) {
                        transientStatus.valueType = MonitoredPropertyStatus.ValueType.FAILED;
                    } else {
                        transientStatus.valueType = MonitoredPropertyStatus.ValueType.OK;
                    }
                }
                transientStatus.timestamp = transientStatus.timestamp == null || transientStatus.timestamp == 0 ? lastSample.left : Math.min(lastSample.left, transientStatus.timestamp);
                transientStatus.value = lastSample.right;

                if (tstate.tolerance != null && tstate.tolerance.inOut == InOut.OUT && nstates != null) {
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
            result.add(new MonitoredPropertyStatus(prop.getComponentType(), prop.getName(), transientStatus.componentId, transientStatus.timestamp, transientStatus.status, transientStatus.badTriggerOids, value == null ? null : value.toString(), transientStatus.valueType));
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
                logger.info("Stopping property sampler for " + mp);
                deletes.add(mp);
                try {
                    ps.close();
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't close state", e);
                }
            }
        }

        if (!deletes.isEmpty() && logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "The following " + what + " are no longer being monitored: " + CollectionUtils.mkString(deletes, "[", ",", "]"));
        }

        logger.info("The following " + what + " are still being monitored: " + CollectionUtils.mkString(liveOnes, "[", ",", "]"));
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

    private class Tolerance {
        private InOut inOut;
        private Long sinceWhen;
        private Long logged;

        private Tolerance(Tolerance prevTolerance) {
            if (prevTolerance == null) return;
            this.inOut = prevTolerance.inOut;
            this.sinceWhen = prevTolerance.sinceWhen;
            this.logged = prevTolerance.logged;
        }
    }

    private class TriggerCheckTask extends TimerTask {
        @Override
        public void run() {
            try {
                final long now = System.currentTimeMillis();

                final Map<MonitorableProperty, PropertyState<?>> pstates;
                final Map<Long, TriggerState> tstates;

                synchronized (MonitoringKernelImpl.this) {
                    pstates = currentPropertyStates;
                    tstates = currentTriggerStates;
                }

                if (tstates == null || pstates == null) {
                    logger.fine("No configuration; skipping trigger condition check");
                    return;
                }

                final Map<MonitorableProperty, PropertyCondition> conditions = new HashMap<MonitorableProperty, PropertyCondition>();
                for (Map.Entry<Long,TriggerState> entry : tstates.entrySet()) {
                    final Long oid = entry.getKey();
                    final TriggerState tstate = entry.getValue();
                    if (!(tstate.trigger instanceof PropertyTrigger)) continue;

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
                    final Comparable sampledValue = sample.right;
                    final long sampleAge = now - when;
                    if (sampleAge > (2 * mstate.getSamplingInterval())) {
                        // TODO parameterize max sample age, and/or figure out how/whether to handle failures specially?
                        logger.log(Level.WARNING, "Last sample for " + property + " more than " + sampleAge + "ms old");
                    }
                    mstate.expireHistory(now - (10 * mstate.getSamplingInterval()));

                    // Compare the sampled value
                    final ComparisonOperator op = ptrigger.getOperator();
                    final Comparable comparisonValue = ptstate.comparisonValue;
                    final Tolerance prevTolerance = tstate.tolerance;
                    final String sampledValueString = sample.right.toString();

                    final Tolerance newTolerance = new Tolerance(prevTolerance);

                    final InOut currentInOut;
                    try {
                        currentInOut = op.compare(sampledValue, comparisonValue, false) ? InOut.OUT : InOut.IN;
                    } catch (Exception e) {
                        logger.log(Level.WARNING, MessageFormat.format("{0} value {1} couldn''t be compared for {2} {3}; skipping", property, sampledValueString, op, comparisonValue), e);
                        continue;
                    }

                    newTolerance.inOut = currentInOut;

                    final boolean log, notify;
                    // TODO consider suppression of repeated notifications as a configurable aspect of NotificationRules

                    if (prevTolerance == null) {
                        // log & notify initial state only if it's OUT
                        log = currentInOut == InOut.OUT;
                        notify = currentInOut == InOut.OUT;
                    } else if (prevTolerance.inOut != currentInOut) {
                        // log & notify all state transitions
                        log = true;
                        notify = true;
                    } else {
                        // State unchanged since last time. Don't notify; log if enough time has elapsed
                        log = prevTolerance.logged != null && now - prevTolerance.logged >= TOLERANCE_LOG_INTERVAL;
                        notify = false;
                    }

                    if (log) {
                        logger.log(Level.INFO, "{0} value {1} is {2} ({3} {4})", new Object[] { property, sampledValueString, currentInOut, op, comparisonValue });
                        newTolerance.logged = now;
                    }

                    if (notify) {
                        PropertyCondition cond = conditions.get(property);
                        if (cond == null) {
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), currentInOut, when, Collections.singleton(oid), sampledValue));
                        } else {
                            // Merge the previous condition with this one
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), currentInOut, cond.getTimestamp(), Sets.union(cond.getTriggerOids(), oid), cond.getValue()));
                        }
                    }

                    tstate.tolerance = newTolerance;
                }

                notificationQueue.addAll(conditions.values());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't finish trigger check task", e);
            }
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

                    final InOut inOut = got.getInOut();

                    final Map<Long, TriggerState> tstates;
                    final Map<Long, NotificationState> nstates;
                    synchronized (MonitoringKernelImpl.this) {
                        tstates = currentTriggerStates;
                        nstates = currentNotificationStates;
                    }
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
                                            final NotificationAttempt.StatusType status = notifier.doNotification(got.getTimestamp(), inOut, pc.getValue(), trigger);
                                            nstate1.notified(new NotificationAttempt(status, null, System.currentTimeMillis()));
                                        } catch (IOException e) {
                                            logger.log(Level.INFO, "Couldn't notify for " + rule + ": " + ExceptionUtils.getMessage(e));
                                            nstate1.failed(new NotificationAttempt(e, System.currentTimeMillis()));
                                        } catch (Exception e) {
                                            logger.log(Level.WARNING, "Couldn't notify for " + rule, e);
                                            nstate1.failed(new NotificationAttempt(e, System.currentTimeMillis()));
                                        }
                                    }
                                });
                            }
                        }
                    }
                    synchronized (MonitoringKernelImpl.this) {
                        MonitoringKernelImpl.this.currentNotificationStates = nstates;
                    }
                } catch (InterruptedException e) {
                    logger.info("Interrupted waiting for notification queue");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
