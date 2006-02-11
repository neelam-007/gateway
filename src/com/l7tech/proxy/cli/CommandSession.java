/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgManagerImpl;
import com.l7tech.common.util.ExceptionUtils;

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

    private final PrintStream out;
    private final PrintStream err;
    private Commands currentCommands = null;
    private boolean interactive = false;
    private SsgManager ssgManager = null;

    public CommandSession(final PrintStream out, final PrintStream err) {
        if (out == null || err == null) throw new NullPointerException();
        this.out = out;
        this.err = err;
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
    public void processCommands(final InputStream in) {
        setInteractive(true);
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        out.println(TYPE_HELP);
        out.println();
        while (interactive) {
            try {
                out.print("SSB> ");
                out.flush();
                String line = bin.readLine();
                if (line == null)
                    break;
                String[] args = line.split("\\s");
                processCommand(args);
            } catch (IOException e) {
                err.println("Error: " + ExceptionUtils.getMessage(e));
                break; // Stop interactive mode if we couldn't read a command
            } catch (IllegalArgumentException e) {
                err.println(ExceptionUtils.getMessage(e));
            } catch (RuntimeException e) {
                err.print("Internal error: ");
                e.printStackTrace(err);
            } catch (Throwable e) {
                err.println("Error: " + ExceptionUtils.getMessage(e));
            }
        }
        out.println("Interactive mode exiting");
    }

    /** Process single command. */
    public void processCommand(String[] args) {
        final String cmdStr = args[0];
        if (cmdStr == null || cmdStr.length() < 1) return; // Ignore blank line
        Command command = (Command)allCommands.getByName(cmdStr);
        if (command == null) {
            command = (Command)allCommands.getByPrefix(cmdStr);
            if (command != null)
                out.println("(" + command.getName() + ")");
        }
        if (command == null) throw new IllegalArgumentException("Unknown command '" + cmdStr + "'. " + TYPE_HELP);

        // Check for correct interactivity
        if (isInteractive() && !command.isInteractive())
            throw new IllegalArgumentException("Command '" + command.getName() + "' is not available in interactive mode");
        if (!isInteractive() && !command.isOneshot())
            throw new IllegalArgumentException("Command '" + command.getName() + "' is not available in command line mode");

        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        command.execute(this, out, rest);
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
    public Nouns getNouns() {
        List global = Arrays.asList(new Object[] {new GatewaysNoun(this)});
        List ssgs = getSsgManager().getSsgList();
        List ret = new ArrayList(global);
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            ret.add(new SsgNoun(ssg));
        }
        return new Nouns(ret);
    }
}

