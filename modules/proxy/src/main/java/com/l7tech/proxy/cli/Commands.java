/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Holds a set of commands, and provides a lookup service.
 */
class Commands extends Words {
    private static Command[] GLOBAL_COMMAND_ARRAY = {
            new NullCommand("interactive", "Enter interactive processing mode", false, true), // placeholder
            new HelpCommand(),
            new ShowCommand(),
            new SetCommand(),
            new CreateCommand(),
            new DeleteCommand(),
            new KerberosCommand(),
            new QuitCommand(),
    };

    private static final Commands GLOBAL_COMMANDS = new Commands();

    /** Create a Commands that knows about all global commands. */
    public Commands() {
        super(Collections.unmodifiableList(Arrays.asList(GLOBAL_COMMAND_ARRAY)));
    }

    /** Create a Commands that knows about only the specified commands. */
    public Commands(List commands) {
        super(commands);
    }

    public static Commands getInstance() {
        return GLOBAL_COMMANDS;
    }

    /** A command that does nothing when executed. */
    private static class NullCommand extends Command {
        NullCommand(String name, String desc) { super(name, desc); }
        NullCommand(String name, String desc, boolean interactive, boolean oneshot) {
            super(name, desc, interactive, oneshot);
        }
        public void execute(CommandSession session, PrintStream out, String[] args) {}
    }

    private static class QuitCommand extends Command {
        public QuitCommand() {
            super("quit", "Quit interactive mode", true, false);
            setHelpText("Use this command to end an interactive session.");
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            session.quit();
        }
    }
}
