/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.util.List;

/**
 * Holds all known configuration nouns, and provides a lookup service.
 * Note that the available configuration nouns can vary depending on what objects currently exist.
 */
class Nouns extends Words {
    /**
     * Create a Nouns that knows only about the specified Noun objects.
     */
    public Nouns(List nouns) {
        super(nouns);
    }
}
