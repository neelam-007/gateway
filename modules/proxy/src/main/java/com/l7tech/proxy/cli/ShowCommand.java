/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "show" global command.
 */
class ShowCommand extends Command {
    protected ShowCommand() {
        super("show", "Show information about an object");
        setMinAbbrev(2);
        setHelpText("The show command displays the current properties of an object.\n" +
                "\n" +
                "        Usage: show <object>\n" +
                "               show <object> <property>\n" +
                "\n" +
                "     Examples: show gateways\n" +
                "               sh g3 clientCert\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: show <object> [<property>]");
        Noun noun = findNoun(session, args);
        noun.show(out, ArrayUtils.shift(args));
    }
}
