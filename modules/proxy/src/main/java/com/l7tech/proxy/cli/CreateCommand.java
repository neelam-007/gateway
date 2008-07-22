/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;

import java.io.PrintStream;

/**
 * The "create" global command.
 */
class CreateCommand extends Command {
    protected CreateCommand() {
        super("create", "Create a new object");
        setHelpText("The create command can create a new instance of an object.\n" +
                "\n" +
                "        Usage: create <object>\n" +
                "               create <object> <property> [<property> [<property]]]\n" +
                "\n" +
                "     Examples: create gateway 192.168.1.57\n" +
                "               create gateway ssg.example.com testuser secret\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        Noun noun = findNoun(session, args);
        noun.create(out, ArrayUtils.shift(args));
        session.onChangesMade();
    }
}
