/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.TextUtils;

import java.io.PrintStream;

/**
 * Represents a verb like "set", "show", or "quite" in the SSB command line configurator.
 */
abstract class Command extends Word {
    private final boolean interactive;
    private final boolean oneshot;

    /**
     * Create a new command with the specified name and description.
     * The command will be tagged as valid both interactively and as a command line oneshot.
     */
    protected Command(String name, String desc) {
        this(name, desc, true, true);
    }

    /**
     * Create a new command.
     *
     * @param name         the unique name of this command, ie "show".   Must not be null or empty.
     * @param desc         the short description of this comand, ie "Shows information about an object".  Never null.
     * @param interactive  true if this command should be listed in the interactive help.
     * @param oneshot      true if this command should be listed in the command line usage.
     */
    protected Command(String name, String desc, boolean interactive, boolean oneshot) {
        super(name, desc);
        this.interactive = interactive;
        this.oneshot = oneshot;
    }

    /** @return true if this command should be listed in the interactive help. */
    public boolean isInteractive() {
        return interactive;
    }

    /** @return true if this command should be listed in the command line usage. */
    public boolean isOneshot() {
        return oneshot;
    }

    /**
     * Run this command in the specified session.  The parameter args, if given and nonempty, should identify
     * a noun (and possibly property) on which to run the command.
     *
     * @param session the session in which the command is to be run.  Must not be null.
     * @param out     where any textual output of the command is to be sent.  Must not be null.
     * @param args    extra arguments for the command.  May be null or empty.
     */
    public abstract void execute(CommandSession session, PrintStream out, String[] args) throws CommandException;

    /**
     * Utility method for subclasses that finds the noun being referred to by args.
     *
     * @param session   the session to search.  Must not be null.
     * @param args      arguments which may refer to a noun.  May be null or empty.
     * @return the located noun.  Never null.
     * @throws CommandException if args is null or empty, or no matching noun could be found in the session.
     */
    protected Noun findNoun(CommandSession session, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + getName() + " <object>");
        Noun noun = (Noun)session.getNouns().getByPrefix(args[0]);
        if (noun == null)
            throw new CommandException("Unrecognized object '" + args[0] + "'.  Type 'help' for more information.");
        return noun;
    }

    protected void printHeaderLine(PrintStream out, String header) {
        out.print(TextUtils.pad(header, 55));
        out.print(TextUtils.pad("Cmdline:" + (isOneshot()?'Y':'N'), 10));
        out.print(TextUtils.pad("Interactive:" + (isInteractive()?'Y':'N'), 14));
        out.println();
    }
}
