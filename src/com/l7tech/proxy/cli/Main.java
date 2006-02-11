/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.security.JceProvider;
import com.l7tech.proxy.datamodel.Managers;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.LogManager;

/**
 * Command line tool for editing a Bridge configuration file.
 */
public class Main {

    private static void initLogging(String configPath) {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        final File file = new File(configPath);
        final LogManager logManager = LogManager.getLogManager();

        try {
            if (file.exists()) {
                final InputStream in = file.toURL().openStream();
                if (in != null) {
                    logManager.readConfiguration(in);
                }
            }

            ClassLoader cl = Main.class.getClassLoader();
            URL resource = cl.getResource(configPath);
            if (resource != null) {
                logManager.readConfiguration(resource.openStream());
            }
        } catch (IOException e) {
            // Oh well, we tried our best
        }
    }

    public static void main(String[] args) {
        initLogging("com/l7tech/proxy/resources/cliLogging.properties");
        System.out.println("SecureSpan Bridge Configuration Editor");
        JceProvider.init();

        final CommandSession cc = new CommandSession(System.out, System.err);
        Managers.setCredentialManager(new CommandSessionCredentialManager(cc, System.in, System.out));

        if (args.length < 1 || cmdMatch(args[0], "i")) {
            // Interactive mode
            cc.processInteractiveCommands(System.in);
            return;
        }

        if (cmdMatch(args[0], "h") || cmdMatch(args[0], "?")) {
            new HelpCommand().execute(cc, System.out, ArrayUtils.shift(args));
            return;
        }

        try {
            cc.processCommand(args);
            cc.saveUnsavedChanges();
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
