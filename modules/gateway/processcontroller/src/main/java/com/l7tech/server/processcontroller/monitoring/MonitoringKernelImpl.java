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
    private volatile Map<Long, NotificationState> currentNotificationStates = null;

    private volatile MonitoringConfiguration currentConfig = null;

    // TODO configurable or heuristic size?
    private final BlockingQueue<NotifiableCondition<?>> notificationQueue = new LinkedBlockingQueue<NotifiableCondition<?>>(500);

    private static class TriggerState {
        private final Trigger trigger;
        private volatile boolean outOfTolerance;

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

    @PostConstruct
    public void start() {
        triggerCheckTimer.scheduleAtFixedRate(triggerCheckTask, 16000, 120000); // TODO use a shorter interval when not debugging
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
        if (priorPstates != null) buildingEstates.putAll(priorEstates);

        final Set<MonitorableEvent> liveEvents = new HashSet<MonitorableEvent>();
        final Set<MonitorableProperty> liveProperties = new HashSet<MonitorableProperty>();

        // Initialize/update states for created/updated properties
        for (Map.Entry<Long, Trigger> entry : newTriggers.entrySet()) {
            final Long triggerOid = entry.getKey();
            final Trigger<?> trigger = entry.getValue();
            buildingTstates.put(triggerOid, new TriggerState(trigger)); // TODO inherit anything from predecessor version?

            if (trigger instanceof PropertyTrigger) {
                PropertyTrigger pt = (PropertyTrigger) trigger;
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

        // Remove dead ones from current state
        buildingPstates.keySet().retainAll(liveProperties);
        buildingEstates.keySet().retainAll(liveEvents);

        currentTriggerStates = buildingTstates;
        currentPropertyStates = buildingPstates;
        for (PropertyState<?> state : buildingPstates.values()) {
            state.schedule(samplerTimer);
        }

        kickTheSampler();

        // TODO apply created NotificationRules
        // TODO apply updated NotificationRules
        // TODO apply deleted NotificationRules

        kickTheNotifier(); // if any notifications have changed

        this.currentConfig = newConfiguration;
    }

    private static class TransientStatus {
        private final Set<Long> badTriggerOids = new HashSet<Long>();
        private MonitoredStatus.StatusType status = MonitoredStatus.StatusType.OK;
        private Serializable value;
        private Long timestamp;
    }

    @Override
    public List<MonitoredPropertyStatus> getCurrentPropertyStatuses(long configurationOid) {
        final Map<MonitorableProperty, TransientStatus> stati = new HashMap<MonitorableProperty, TransientStatus>();

        final Map<MonitorableProperty, PropertyState<?>> pstates = currentPropertyStates;
        final Map<Long, TriggerState> tstates = currentTriggerStates;
        final Map<Long, NotificationState> nstates = currentNotificationStates;

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
                if (ptrig.getMonitoringConfig().getOid() != configurationOid) continue;

                TransientStatus transientStatus = stati.get(prop);
                if (transientStatus == null) {
                    transientStatus = new TransientStatus();
                    stati.put(prop, transientStatus);
                }

                if (tstate.outOfTolerance) {
                    transientStatus.badTriggerOids.add(toid);
                    transientStatus.status = MonitoredStatus.StatusType.WARNING;
                }

                final Pair<Long, ? extends Serializable> lastSample = pstate.getLastSample();
                transientStatus.timestamp = transientStatus.timestamp == 0 ? lastSample.left : Math.min(lastSample.left, transientStatus.timestamp);
                transientStatus.value = lastSample.right;

                for (NotificationRule rule : ptrig.getNotificationRules()) {
                    if (rule.getMonitoringConfiguration().getOid() != configurationOid) continue;
                    NotificationState nstate = nstates.get(rule.getOid());
                    if (nstate.isNotified()) {
                        transientStatus.status = MonitoredStatus.StatusType.NOTIFIED;
                    }
                }
            }
        }

        final List<MonitoredPropertyStatus> result = new ArrayList<MonitoredPropertyStatus>();
        for (Map.Entry<MonitorableProperty, TransientStatus> entry : stati.entrySet()) {
            final MonitorableProperty prop = entry.getKey();
            final TransientStatus transientStatus = entry.getValue();
            result.add(new MonitoredPropertyStatus(prop, transientStatus.timestamp, transientStatus.status, transientStatus.badTriggerOids, transientStatus.value));
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
                    final PropertyTrigger ptrigger = (PropertyTrigger) tstate.trigger;
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
                        // TODO parameterize max sample age, and/or figure out how/whether to handle failures specially?
                        logger.log(Level.WARNING, "Last sample for " + property + " more than " + sampleAge + "ms old");
                    }

                    // Compare the sampled value
                    final ComparisonOperator op = ptrigger.getOperator();
                    final Comparable rvalue = ptrigger.getTriggerValue();
                    if (op.compare(what, rvalue, false)) {
                        logger.log(Level.INFO, property + " is out of tolerance"); // TODO maybe log the value and test expression here too
                        tstate.outOfTolerance = true;
                        PropertyCondition cond = conditions.get(property);
                        if (cond == null) {
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), when, Collections.singleton(oid), rvalue));
                        } else {
                            // Merge the previous condition with this one
                            conditions.put(property, new PropertyCondition(property, ptrigger.getComponentId(), cond.getTimestamp(), Sets.union(cond.getTriggerOids(), oid), cond.getValue()));
                        }
                    } else {
                        tstate.outOfTolerance = false;
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
                final Map<Long, TriggerState> tstates = currentTriggerStates;
                final Map<Long, NotificationState> nstates = currentNotificationStates;
                try {
                    final NotifiableCondition<?> got = notificationQueue.poll(500, TimeUnit.MILLISECONDS); // TODO timeout configurable?
                    if (got == null) continue;

                    final Set<Long> triggerOids = got.getTriggerOids();
                    if (triggerOids == null || triggerOids.isEmpty()) {
                        logger.warning("Got a condition with no trigger OIDs");
                        continue;
                    }

                    for (Long triggerOid : triggerOids) {
                        final TriggerState tstate = tstates.get(triggerOid);
                        if (tstate == null) {
                            logger.log(Level.FINE, "Trigger #{0} has been deleted; skipping", triggerOid);
                            continue;
                        }

                        final Trigger<?> trigger = tstate.trigger;

                        final List<NotificationRule> rules = trigger.getNotificationRules();
                        for (NotificationRule rule : rules) {
                            final Long notificationOid = rule.getOid();
                            NotificationState nstate = nstates.get(notificationOid);
                            if (nstate == null) {
                                nstate = new NotificationState(trigger, rule, 1, TimeUnit.MINUTES);
                                nstates.put(notificationOid, nstate);
                            } else {
                                nstate.condition(got.getTimestamp());
                            }

                            if (!nstate.isOKToFire()) {
                                logger.fine("Suppressing repeated notification"); // TODO log more details
                                continue;
                            }

                            final Notifier notifier = notifierFactory.getNotifier(rule);
                            if (got instanceof PropertyCondition) {
                                final PropertyCondition pc = (PropertyCondition) got;
                                final NotificationState nstate1 = nstate;
                                notifierThreadPool.submit(new Runnable() {
                                    public void run() {
                                        try {
                                            final NotificationAttempt.StatusType status = notifier.doNotification(got.getTimestamp(), pc.getValue(), trigger);
                                            nstate1.notified(new NotificationAttempt(status, null, System.currentTimeMillis()));
                                        } catch (Exception e) {
                                            logger.log(Level.WARNING, "Couldn't notify", e);
                                            nstate1.failed(new NotificationAttempt(e, System.currentTimeMillis()));
                                        }
                                    }
                                });
                            }
                        }
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
