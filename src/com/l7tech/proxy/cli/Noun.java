/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;

/**
 * Represents a configuration noun like "gateways" or "gateway57".
 */
abstract class Noun extends Word {
    protected Noun(String name, String desc) {
        super(name, desc);
    }

    // Display all relevant info about this noun to the specified output stream.

    /**
     * Create a new instance of this noun.
     *
     * @param out
     * @param args
     */
    public void create(PrintStream out, String[] args) throws CommandException {
        throw new CommandException("Unable to create a new instance of object " + getName());
    }

    /**
     * Display information about this noun, or the specific property of this noun, to the specified output stream.
     *
     * @param out  the output stream.  Must not be null.
     * @param args info about the property to display; if not provided, all relevant overview info should be displayed.
     *             May be null or empty.
     */
    public void show(PrintStream out, String[] args) throws CommandException {
        out.println(getName() + " - " + getDesc() + "\n");
    }

    /**
     * Delete this noun, or some property of it.
     *
     * @param out  the output stream.  Must not be null.
     * @param args info about a property to delete; if not provided, the entire noun should be deleted.
     */
    public void delete(PrintStream out, String[] args) throws CommandException {
        throw new CommandException("Unable to delete object " + getName());
    }

    /**
     * Set a property of this noun.
     *
     * @param out  the output stream.  Must not be null.
     * @param propertyName   the name of the property to set.  Must not be null or empty.
     * @param args  the value to set it to.  May be null or empty.
     * @throws CommandException if there is no such property or the format of args is incorrect.
     */
    public void set(PrintStream out, String propertyName, String[] args) throws CommandException {
        throw new CommandException("Unable to set properties on object " + getName());
    }
}
