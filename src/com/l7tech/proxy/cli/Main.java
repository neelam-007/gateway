/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.security.JceProvider;
import com.l7tech.proxy.datamodel.Managers;

/**
 * Command line tool for editing a Bridge configuration file.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("SecureSpan Bridge Configuration Editor\n");
        JceProvider.init();

        final CommandSession cc = new CommandSession(System.out, System.err);
        Managers.setCredentialManager(new CommandSessionCredentialManager(cc, System.in, System.out));

        if (args.length < 1 || cmdMatch(args[0], "i")) {
            // Interactive mode
            cc.processCommands(System.in);
            return;
        }

        if (cmdMatch(args[0], "h") || cmdMatch(args[0], "?")) {
            new HelpCommand().execute(cc, System.out, ArrayUtils.shift(args));
            return;
        }

        try {
            cc.processCommand(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            new HelpCommand().execute(cc, System.err, ArrayUtils.shift(args));
        }
    }

    /** @return true iff. arg starts with s, -s, or --s */
    private static boolean cmdMatch(String arg, String s) {
        while (arg.startsWith("-")) arg = arg.substring(1);
        return arg.toLowerCase().startsWith(s.toLowerCase());
    }
}
