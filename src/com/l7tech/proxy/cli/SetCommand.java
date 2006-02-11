/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "set" command.
 */
class SetCommand extends Command {
    protected SetCommand() {
        super("set", "Set an object property");
        setMinAbbrev(2);
        setHelpText("The set command changes the properties of an object.\n" +
                "You can get a description of the properties for an object with 'help <object>'.\n" +
                "You can see the current values of the properties for an object with 'show <object>'.\n" +
                "In interactive mode, a setter may prompt for additional information (like passwords).\n" +
                "For some other properties, the value may be omitted.\n" +
                "\n" +
                "        Usage: set <object> <property> <value>\n" +
                "               set <object> <property>\n" +
                "\n" +
                "     Examples: set gateway1 clientCertificate auto\n" +
                "               set gateway3 defaultGateway\n" +
                "               set gateway5 password\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 2 || args[0] == null || args[0].length() < 1 || args[1] == null || args[1].length() < 1)
            throw new CommandException("Usage: set <object> <property> [<value>]");
        Noun noun = findNoun(session, args);
        args = ArrayUtils.shift(args);
        noun.set(out, args[0], ArrayUtils.shift(args));
        session.onChangesMade();
    }
}
