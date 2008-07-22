/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "set" global command.
 */
class SetCommand extends Command {
    protected SetCommand() {
        super("set", "Set an object property");
        setMinAbbrev(2);
        setHelpText("The set command changes the properties of an object.\n" +
                "Use 'help <object>' for a description of the properties for an object.\n" +
                "You can see the current values of the properties with 'show <object>'.\n" +
                "\n" +
                "        Usage: <object> set <property> <value>\n" +
                "               <object> set <property>\n" +
                "\n" +
                "     Examples: gateway1 set clientCertificate auto\n" +
                "               gateway3 set defaultGateway true\n" +
                "               gateway5 set password s3cr3t\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 2 || args[0] == null || args[0].length() < 1 || args[1] == null || args[1].length() < 1)
            throw new CommandException("Usage: set <object> <property> [<value>]");

        // Hack -- enable a 79 column grid for debugging
        if ("grid".equals(args[0])) {
            session.grid = Boolean.valueOf(args[1]).booleanValue();
            return;
        }

        Noun noun = findNoun(session, args);
        args = ArrayUtils.shift(args);
        noun.set(out, args[0], ArrayUtils.shift(args));
        session.onChangesMade();
    }
}
