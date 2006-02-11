/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ExceptionUtils;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Holds a set of commands, and provides a lookup service.
 */
class Commands extends Words {
    private static Command[] GLOBAL_COMMAND_ARRAY = {
            new NullCommand("interactive", "Enter interactive processing mode", false, true),
            new HelpCommand(),
            new ShowCommand(),
            new SetCommand(),
            new CreateCommand(),
            new DeleteCommand(),
            new QuitCommand(),
            new SaveCommand(),
            new RollbackCommand(),
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
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            if (session.isUnsavedChanges()) {
                if ((args == null || args.length < 1))
                    throw new CommandException("You have unsaved changes -- please use either 'quit save' or 'quit rollback'.");
                Command cmd = (Command)session.getCommands().getByPrefix(args[0]);
                final boolean isRoll = cmd != null && cmd.getName().equals("rollback");
                final boolean isSave = cmd != null && cmd.getName().equals("save");
                if (!isRoll && !isSave) {
                    throw new CommandException("You have unsaved changes -- please use either 'quit save' or 'quit rollback'.");
                } else {
                    if (isSave)
                        cmd.execute(session, out, args);
                }
            }
            session.quit();
        }
    }

    private static class SaveCommand extends Command {
        protected SaveCommand() {
            super("save", "Save configuration changes, overwriting the config file");
            setMinAbbrev(2);
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            try {
                session.saveConfig();
            } catch (IOException e) {
                throw new CommandException("Unable to save changes: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private static class RollbackCommand extends Command {
        protected RollbackCommand() {
            super("rollback", "Reload config file, discarding changes");
            setMinAbbrev(4);
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            session.rollback();
        }
    }
}
