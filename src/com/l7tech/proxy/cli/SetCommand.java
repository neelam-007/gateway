/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import java.io.PrintStream;

/**
 * The "set" command.
 */
class SetCommand extends Command {
    protected SetCommand() {
        super("set", "Set an object property");
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

    public void execute(CommandSession session, PrintStream out, String[] args) {
        // TODO
        throw new RuntimeException("Not yet implemented");
    }
}
