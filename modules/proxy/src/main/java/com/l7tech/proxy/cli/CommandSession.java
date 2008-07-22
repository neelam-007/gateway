/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ArrayUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a one-shot or interactive command line session.
 */
class CommandSession {
    public static final String TYPE_HELP = "Type 'help' for instructions.";
    private static final Commands allCommands = Commands.getInstance();

    final private CommandSessionCredentialManager credentialManager;
    private final PrintStream out;
    private final PrintStream err;
    private Commands currentCommands = null;
    private boolean interactive = false;
    private SsgManagerImpl ssgManager = null;
    boolean grid = false;

    public CommandSession(final PrintStream out, final PrintStream err) {
        if (out == null || err == null) throw new NullPointerException();
        this.out = out;
        this.err = err;
        this.credentialManager = new CommandSessionCredentialManager(this);
    }

    /**
     * @return the credential manager attached to this session.  Never null.
     */
    public CommandSessionCredentialManager getCredentialManager() {
        return credentialManager;
    }

    /** @return true if this session is running in interactive command mode. */
    public boolean isInteractive() {
        return interactive;
    }

    private void setInteractive(boolean interactive) {
        if (this.interactive != interactive) currentCommands = null;
        this.interactive = interactive;
    }

    /** @return the SsgManager held by this session. */
    public SsgManager getSsgManager() {
        if (ssgManager == null) {
            ssgManager = SsgManagerImpl.getSsgManagerImpl();
            ssgManager.load();
        }
        return ssgManager;
    }

    /** @return the output stream.  Never null. */
    public PrintStream getOut() {
        return out;
    }

    /** @return the error stream.  Never null. */
    public PrintStream getErr() {
        return err;
    }

    /** Repeatedly read allCommands and process them. */
    public void processInteractiveCommands(final InputStream in) {
        setInteractive(true);
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        getSsgManager();
        out.println("Editing configuration in " + ssgManager.getStorePath());
        out.println(TYPE_HELP);
        out.println();
        while (interactive) {
            try {
                if (grid)
                    out.println("---------|---------|---------|---------|---------|---------|---------|       ||");
                out.print("SSXVC> ");
                out.flush();
                String line = bin.readLine();
                if (line == null)
                    break;
                String[] args = line.split("\\s");
                processCommand(args);
            } catch (IOException e) {
                err.println("Error: " + ExceptionUtils.getMessage(e));
                break; // Stop interactive mode if we couldn't read a command
            } catch (CommandException e) {
                // Normal exception used to report error message
                err.println(ExceptionUtils.getMessage(e));
            } catch (Throwable e) {
                err.print("Internal error: ");
                e.printStackTrace(err);
            }
        }
        out.println("Interactive mode exiting");
    }

    /** Process single command. */
    public void processCommand(String[] args) throws CommandException {
        if (args == null || args.length < 1) return;  // ignore blank lines
        final String wordStr = args[0];
        args = ArrayUtils.shift(args);

        // First, check for COMMAND NOUN ARG ARG ARG style
        Command command = (Command)allCommands.getByPrefix(wordStr);

        if (command == null) {
            // Check for NOUN COMMAND ARG ARG ARG style
            Words nouns = getNouns();
            final Noun noun = (Noun)nouns.getByPrefix(wordStr);
            if (noun == null)
                throw new CommandException("Unknown command or object '" + wordStr + "'. " + TYPE_HELP);

            // Execute special command (object.verb)
            noun.execute(this, out, args);
            return;
        }

        // Check for correct interactivity
        if (isInteractive() && !command.isInteractive())
            throw new CommandException("Command '" + command.getName() + "' is not available in interactive mode");
        if (!isInteractive() && !command.isOneshot())
            throw new CommandException("Command '" + command.getName() + "' is not available in command line mode");

        // Execute global command (verb.object)
        command.execute(this, out, args);
    }

    /** Quit an interactive mode session. */
    public void quit() {
        setInteractive(false);
    }

    /** @return all Commands currently available in this session. Never null or empty. */
    public Commands getCommands() {
        if (currentCommands == null) {
            // Initialize list based on whether we are currently in interactive mode.
            List valid = new ArrayList();
            List all = allCommands.getAll();
            boolean interactive = isInteractive();
            for (Iterator i = all.iterator(); i.hasNext();) {
                Command command = (Command)i.next();
                if (interactive ? command.isInteractive() : command.isOneshot())
                    valid.add(command);
            }
            currentCommands = new Commands(valid);
        }
        return currentCommands;
    }

    /** @return all Nouns currently available in this session.  Never null or empty. */
    public Words getNouns() {
        List global = Arrays.asList(new Object[] {new GatewaysNoun(this)});
        List ssgs = getSsgManager().getSsgList();
        List ret = new ArrayList(global);
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            ret.add(new SsgNoun(this, ssg));
        }
        return new Words(ret);
    }

    /**
     * Set the unsaved changes flag.
     * Used by commands to report when changes have been made to the config file.
     * The changed config file won't be saved until saveChanges() is called.
     */
    public void onChangesMade() {
        try {
            saveConfig();
        } catch (IOException e) {
            err.println("Error: unable to save configuration: " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Unconditionally writes out the config file.
     */
    public void saveConfig() throws IOException {
        getSsgManager().save();
    }
}
