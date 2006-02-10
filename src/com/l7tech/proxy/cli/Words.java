/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Holds a collection of Word objects, and provides lookup services.
 */
class Words {
    protected final List words;

    /** Create a Words that knows only about the specified words. */
    public Words(List words) {
        this.words = words;
    }

    /** @return a List of all recognized words.  Never null or empty. */
    public List getAll() {
        return Collections.unmodifiableList(words);
    }

    /**
     * Finds a word that starts with the specified prefix.  If more than one word matches the specified prefix,
     * this returns the first match.
     *
     * @param prefix the name to look up.  If null or empty, no match will be attempted and null will be returned.
     * @return the matching word, or null if no match was found.
     */
    public Word getByPrefix(String prefix) {
        if (prefix == null || prefix.length() < 1) return null;
        prefix = prefix.trim().toLowerCase();
        List words = getAll();
        for (Iterator i = words.iterator(); i.hasNext();) {
            Word word = (Word)i.next();
            if (word.getName().startsWith(prefix))
                return word;
        }
        return null;
    }

    /**
     * Finds a word that matches the specified name, disregarding case and leading or trailing whitespace.
     *
     * @param name  the name to match.  If null or empty, no match will be attempted and null will be returned.
     * @return the matching word, or null if no match was found.
     */
    public Word getByName(String name) {
        if (name == null || name.length() < 1) return null;
        name = name.trim().toLowerCase();
        List words = getAll();
        for (Iterator i = words.iterator(); i.hasNext();) {
            Word word = (Word)i.next();
            if (word.getName().equalsIgnoreCase(name))
                return word;
        }
        return null;
    }
}
