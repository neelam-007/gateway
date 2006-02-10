/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

/**
 * Represents either a command or a configuration noun in the Bridge CLI.
 */
abstract class Word {
    protected final String name;
    protected final String desc;
    protected String help = null;

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
