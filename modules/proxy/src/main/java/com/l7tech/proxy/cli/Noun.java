/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;

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

    /**
     * Attempt to execute a command on this noun.  This method will try to find a global command first
     *
     * @param session  the session in which to execute the command.  Must not be null.
     * @param out      the output stream.  Must not be null.
     * @param args     the command followed by any arguments.  Must be non-null and non-empty.
     */
    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + getName() + " <command> [<parameter> [<value>]]");
        final String cmdName = args[0];
        args = ArrayUtils.shift(args);

        // First check for a special command
        if (executeSpecial(session, out, cmdName, args))
            return;

        // As a convenience, we'll allow global commands to be used as special commands
        Command gcmd = (Command)session.getCommands().getByPrefix(cmdName);
        if (gcmd != null) {
            gcmd.execute(session, out, ArrayUtils.unshift(args, getName()));
            return;
        }

        throw new CommandException("Object " + getName() + " has no special command '" + cmdName + "'");
    }

    /**
     * Attempt to execute a special command on this noun.
     *
     * @param session  the session in which to execute the command.  Must not be null.
     * @param out      the output stream.  Must not be null.
     * @param cmdName  the name of the special command to excute.  Must not be null or empty.
     * @param args     any arguments for the command.  May be null or empty.
     * @return true if a command was recognized and executed successfully; false if the command was not recognized
     * @throws CommandException if cmdName is recognized as a special command, but had invalid arguments or
     *                          couldn't be excecuted
     */
    protected boolean executeSpecial(CommandSession session, PrintStream out, String cmdName, String[] args) throws CommandException {
        return false;
    }
}
