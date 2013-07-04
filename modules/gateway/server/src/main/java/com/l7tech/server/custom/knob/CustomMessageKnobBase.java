package com.l7tech.server.custom.knob;

import com.l7tech.policy.assertion.ext.message.knob.CustomMessageKnob;
import org.jetbrains.annotations.NotNull;

/**
 * This is the base class for {@link CustomMessageKnob} implementer classes.
 * Provides simple extraction of <tt>CustomMessageKnob</tt> name and description fields,
 * which is common for all <tt>CustomMessageKnob</tt> implementations.
 */
public class CustomMessageKnobBase implements CustomMessageKnob {
    protected final String name;
    protected final String description;

    public CustomMessageKnobBase(@NotNull final String name, @NotNull final String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getKnobName() {
        return name;
    }

    @Override
    public String getKnobDescription() {
        return description;
    }
}
