/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

/**
 * Represents either a command or a configuration noun in the Bridge CLI.
 */
abstract class Word {
    private final String name;
    private final String desc;
    private int minAbbrev = 1; // minimum length of abbreviation that may match this word
    private String help = null;

    /**
     * Create a new Word with the specified name and description.
     *
     * @param name         the unique name of this word, ie "show".   Must not be null or empty.
     * @param desc         the short description of this word, ie "Shows information about an object".  Never null.
     */
    protected Word(String name, String desc) {
        if (name == null || desc == null) throw new NullPointerException();
        if (name.length() < 1 || desc.length() < 1) throw new IllegalArgumentException();
        this.name = name;
        this.desc = desc;
    }

    protected void setHelpText(String helpText) {
        if (helpText == null || helpText.length() < 1) throw new IllegalArgumentException();
        this.help = helpText;
    }

    /** @return minimum abbreviation length.  Always positive, and never greater than the length of {@link #getName}. */
    public int getMinAbbrev() {
        return minAbbrev;
    }

    protected void setMinAbbrev(int minAbbrev) {
        if (minAbbrev < 1) throw new IllegalArgumentException();
        if (minAbbrev > getName().length()) throw new IllegalArgumentException();
        this.minAbbrev = minAbbrev;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    /** @return the name of this word, ie "show". Never null or empty. */
    public String getName() {
        return name;
    }

    /** @return the short description of this word, ie "Shows information about an object".  Never null or empty. */
    public String getDesc() {
        return desc;
    }

    /**
     * @return the multiline help text for this word.  Never null or empty.
     */
    public String getHelpText() {
        return help != null ? help : "There is no additional help available for this word.";
    }
}
