/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "show" command.
 */
class ShowCommand extends Command {
    protected ShowCommand() {
        super("show", "Show information about an object");
        setHelpText("The show command displays the current properties of an object.\n" +
                "\n" +
                "        Usage: show <object>\n" +
                "               show <object> <property>\n" +
                "\n" +
                "     Examples: show gateways\n" +
                "               sh g3 clientCert\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new IllegalArgumentException("Usage: show <object> [<property>]");
        Noun noun = (Noun)session.getNouns().getByPrefix(args[0]);
        if (noun == null)
            throw new IllegalArgumentException("Unrecognized object '" + args[0] + "'.  Type 'help' for more information.");
        noun.show(out, ArrayUtils.shift(args));
    }
}
