/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.registry.RegistryStub;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Locator for use during tests, that uses stub versions of all manager classes.
 */
public class StubModeLocator extends Locator {
    private static final Logger log = Logger.getLogger(StubModeLocator.class.getName());

    public StubModeLocator() {
        log.info("New StubModeLocator");
    }

    public Object lookup(Class clazz) {
        log.info("Lookup: " + clazz);
        if (clazz.isAssignableFrom(Registry.class)) {
            return new RegistryStub();
        }

        return null;
    }

    public Locator.Matches lookup(final Locator.Template template) {
        log.info("Lookup");
        return new Locator.Matches() {
            public Collection allInstances() {
                Object got = lookup(template.getType());
                if (got == null)
                    return Collections.EMPTY_LIST;
                List list = new LinkedList();
                list.add(got);
                return list;
            }

            /**
             * Get all registered Items that match the criteria.
             * This should include all pairs of instances together
             * with their classes, IDs, and so on.
             *
             * @return collection of {@link com.l7tech.common.util.Locator.Item}
             */
            public Collection allItems() {
                throw new IllegalStateException("not implemented");
            }
        };
    }
}
