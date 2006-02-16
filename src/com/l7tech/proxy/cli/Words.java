/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern digitFinder = Pattern.compile("(\\d+)$");

    /**
     * Finds a word that starts with the specified prefix.  If more than one word matches the specified prefix,
     * this returns the first match.
     * <p/>
     * Special behavior for words followed by numbers: a prefix followed by digits will match a word followed
     * by those same digits.  For example, the prefix "g57" will match the word "gateway57"
     * (but not "gateway157" or "gateway575").
     *
     * @param prefix the name to look up.  If null or empty, no match will be attempted and null will be returned.
     * @return the matching word, or null if no match was found.
     */
    public Word getByPrefix(String prefix) {
        if (prefix == null || prefix.length() < 1) return null;
        prefix = prefix.trim().toLowerCase();

        String numbers = null;
        Matcher matcher = digitFinder.matcher(prefix);
        if (matcher.find()) {
            numbers = matcher.group(1);
            prefix = matcher.replaceAll("");
        }

        List words = getAll();
        final int prefixLength = prefix.length();
        for (Iterator i = words.iterator(); i.hasNext();) {
            Word word = (Word)i.next();
            final String name = word.getName().toLowerCase();
            if (name.startsWith(prefix) && prefixLength >= word.getMinAbbrev()) {
                if (numbers == null)
                    return word;
                matcher = digitFinder.matcher(name);
                if (!matcher.find())
                    continue;
                if (!numbers.equals(matcher.group(1)))
                    continue;
                return word;
            }
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
