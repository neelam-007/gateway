/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ArrayUtils;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.LogManager;

/**
 * Command line tool for editing a Bridge configuration file.
 */
public class Main {

    private static void initLogging(String configPath) {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");

        // Software-only TransformerFactory to ignore the alluring Tarari impl, even if tarari_raxj.jar is sitting right there
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        
        final File file = new File(configPath);
        final LogManager logManager = LogManager.getLogManager();

        try {
            if (file.exists()) {
                final InputStream in = file.toURI().toURL().openStream();
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

    private static void showHelp(CommandSession cc, String[] args) throws CommandException {
        new HelpCommand().execute(cc, System.out, args);
    }

    public static void main(String[] args) {
        initLogging("com/l7tech/proxy/resources/cliLogging.properties");
        System.out.println("SecureSpan "+ Constants.APP_NAME+" Configuration Editor");
        JceProvider.init();

        if (args.length < 1 || cmdMatch(args[0], "i")) {
            // Interactive mode
            final CommandSession cc = new CommandSession(System.out, System.err);
            Managers.setCredentialManager(cc.getCredentialManager());
            cc.processInteractiveCommands(System.in);
            return;
        }

        // One-shot command mode
        try {
            if (cmdMatch(args[0], "h") || cmdMatch(args[0], "?")) {
                // Show specific help (if arguments passed), and send output to stderr
                showHelp(new CommandSession(System.err, System.err), ArrayUtils.shift(args));
                System.exit(1);
            }
            final CommandSession cc = new CommandSession(System.out, System.err);
            Managers.setCredentialManager(cc.getCredentialManager());
            cc.processCommand(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            try {
                // Show general help, and send output to stderr
                showHelp(new CommandSession(System.err, System.err), null);
                System.exit(1);
            } catch (CommandException e1) {
                throw new RuntimeException(e1); // can't happen -- null args passed
            }
        }
    }

    /** @return true iff. arg starts with s, -s, or --s */
    private static boolean cmdMatch(String arg, String s) {
        while (arg.startsWith("-")) arg = arg.substring(1);
        return arg.toLowerCase().startsWith(s.toLowerCase());
    }
}
