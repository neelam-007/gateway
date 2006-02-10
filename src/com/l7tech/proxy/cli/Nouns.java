/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.util.List;
import java.util.Collections;
import java.util.Arrays;

/**
 * Holds all known configuration nouns, and provides a lookup service.
 * Note that the available configuration nouns can vary depending on what objects currently exist.
 */
class Nouns extends Words {
    private static final Noun[] NOUNS = {
            new GatewaysNoun(),
    };

    /**
     * Create a Nouns that knows only about the specified Noun objects.
     */
    public Nouns(List nouns) {
        super(nouns);
    }

    /** @return a list of only the global configuration nouns.  Never null or empty. */
    public static List getGlobal() {
        return Collections.unmodifiableList(Arrays.asList(NOUNS));
    }
}
