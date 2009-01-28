/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.PropertyTrigger;

import java.io.Closeable;
import java.io.Serializable;

/**
 * Abstract superclass of property samplers.  Concrete subclasses should sample the property's instantaneous value every
 * time {@link #sample} is called and leave the responsibility for interpreting the results (e.g. by calculating a
 * moving average or interpolating values to fill in sampling gaps) to the caller.
 * <p/>
 * Implementations should feel free to cache any infrastructure components that may be needed to retrieve property
 * values (e.g. ProcessBuilders, sockets, database connections...) but should not cache the values themselves.
 *
 * @param <V> the type of the sampled property values
 */
public abstract class PropertySampler<V extends Serializable> implements Closeable {
    /** The trigger that holds this sampler's configuration */
    protected final PropertyTrigger<V> trigger;

    protected PropertySampler(PropertyTrigger<V> trigger) {
        this.trigger = trigger;
    }

    /**
     * Attempts to samples the property described by the {@Link #trigger}, returning the property value, or throwing an
     * exception if no value can be sampled.
     *
     * @return the sampled property value.  Never null.
     * @throws PropertySamplingException if the property cannot be sampled (e.g. due to an IOException)
     */
    abstract V sample() throws PropertySamplingException;
}
