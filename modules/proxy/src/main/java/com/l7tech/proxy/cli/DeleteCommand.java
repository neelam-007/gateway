/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "delete" global command.
 */
class DeleteCommand extends Command {
    protected DeleteCommand() {
        super("delete", "Delete an object and all its properties");
        setMinAbbrev(3);
        setHelpText("The delete command can destroy an object.  If deleting the object might\n" +
                "cause the only copy of a private key to be destroyed, the 'force' option will\n " +
                "be required in order to delete it.\n" +
                "\n" +
                "        Usage: delete <object> [force]\n" +
                "\n" +
                "     Examples: delete gateway37\n" +
                "               delete gateway12 force\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + getName() + " <object>");
        final Words nouns = session.getNouns();
        Noun noun = (Noun)nouns.getByName(args[0]);
        if (noun == null) {
            noun = (Noun)nouns.getByPrefix(args[0]);
            if (noun == null)
                throw new CommandException("Unrecognized object '" + args[0] + "'.  Type 'help' for more information.");
            throw new CommandException("For safety, 'delete' doesn't allow abbreviations.  Use 'delete " + noun.getName() + "' instead.");
        }
        noun.delete(out, ArrayUtils.shift(args));
        session.onChangesMade();
    }
}
