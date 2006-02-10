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
    }

    public void execute(CommandSession session, PrintStream out, String[] args) {
        // TODO
        throw new RuntimeException("Not yet implemented");
    }
}
