/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "delete" command.
 */
class DeleteCommand extends Command {
    protected DeleteCommand() {
        super("delete", "Delete an object and all its properties");
        setMinAbbrev(3);
        setHelpText("The delete command can destroy an object.\n" +
                "Unlike other commands, for delete, the object name must be spelled out in full.\n" +
                "\n" +
                "        Usage: delete <object>\n" +
                "\n" +
                "     Examples: delete gateway37\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + getName() + " <object>");
        final Nouns nouns = session.getNouns();
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
