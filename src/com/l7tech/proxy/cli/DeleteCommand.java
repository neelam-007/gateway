/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;

/**
 * The "delete" command.
 */
class DeleteCommand extends Command {
    protected DeleteCommand() {
        super("delete", "Delete an object and all its properties");
        setHelpText("The delete command can destroy an object.\n" +
                "Unlike other commands, for delete, the object name must be spelled out in full.\n" +
                "\n" +
                "        Usage: delete <object>\n" +
                "\n" +
                "     Examples: delete gateway37\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) {
        // TODO
        throw new RuntimeException("Not yet implemented");
    }
}
