package com.l7tech.server.custom.format;

import com.l7tech.policy.assertion.ext.message.format.CustomMessageFormat;

import com.l7tech.server.StashManagerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * This is the base class for {@link CustomMessageFormat} implementer classes.
 * Provides simple extraction of <tt>CustomMessageFormat</tt> name and description fields,
 * which is common for all <tt>CustomMessageFormat</tt> implementations.
 */
public abstract class CustomMessageFormatBase<T> implements CustomMessageFormat<T> {
    protected final String name;
    protected final String description;
    protected final StashManagerFactory stashManagerFactory;

    public CustomMessageFormatBase(@NotNull StashManagerFactory stashManagerFactory, @NotNull final String name, @NotNull final String description) {
        this.name = name;
        this.description = description;
        this.stashManagerFactory = stashManagerFactory;
    }

    @Override
    public String getFormatName() {
        return name;
    }

    @Override
    public String getFormatDescription() {
        return description;
    }
}
