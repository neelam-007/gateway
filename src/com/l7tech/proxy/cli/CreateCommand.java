/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;

/**
 * The "create" command.
 */
class CreateCommand extends Command {
    protected CreateCommand() {
        super("create", "Create a new object");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) {
        // TODO
        throw new RuntimeException("Not yet implemented");
    }
}
