/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * State for a PropertyTrigger and its associated notifications
 * @param <V> the type of the sampled property values
 * @param <T> the type of PropertyTrigger
 */
public class PropertyState<V extends Serializable, T extends PropertyTrigger<V>> extends State<T> {
    private final NavigableMap<Long, V> lastSamples = new ConcurrentSkipListMap<Long,V>();

    /**
     * Create a new PropertyState, optionally copying the sample history from a preexisting PropertyState corresponding
     * to the same property.
     *  
     * @param oldState the old PropertyState to copy the sample history from, or null if no prior instance is available.
     * @param trigger the trigger object that
     * @param notificationStates
     */
    protected PropertyState(PropertyState<V, T> oldState, T trigger, List<NotificationState> notificationStates) {
        super(trigger, notificationStates);
        if (oldState != null) this.lastSamples.putAll(oldState.lastSamples);
    }

    protected void addSample(V value, Long when) {
        if (when == null) throw new IllegalArgumentException();
        lastSamples.put(when, value);
    }

    /**
     * Gets the samples that are available in the given time range.
     * @param from
     * @param to
     * @return
     */
    Map<Long, V> getSamples(Long from, Long to) {
        return Collections.unmodifiableMap(lastSamples.subMap(from, true, to, true));
        // TODO GC?
    }

    /**
     * Makes a new PropertyState, with possibly-new Trigger and/or NotificationRules, but retaining the sample history
     * @param newTrigger
     * @param newRulesList
     * @return
     */
    protected PropertyState<V, T> reconfigure(T newTrigger, List<NotificationRule> newRulesList) {
        if (newTrigger.isIncompatibleWith(this.trigger))
            throw new IllegalArgumentException("Old and new triggers are incompatible");

        final Map<Long, NotificationRule> newRulesMap = new HashMap<Long, NotificationRule>();
        for (NotificationRule rule : newRulesList) {
            newRulesMap.put(rule.getOid(), rule);
        }

        final List<NotificationState> newStates = new ArrayList<NotificationState>();
        for (NotificationState oldState : notificationStates) {
            final NotificationRule oldRule = oldState.getRule();
            NotificationRule newRule = newRulesMap.get(oldRule.getOid());
            if (newRule != null && newRule.isIncompatibleWith(oldRule)) {
                throw new IllegalArgumentException("Old and new notification rules are incompatible");
            }
            newStates.add(new NotificationState(oldState));
        }

        final PropertyState<V,T> ps = new PropertyState<V,T>(this, newTrigger, newStates);
        ps.lastSamples.putAll(this.lastSamples);
        return ps;
    }

}
