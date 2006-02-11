/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.TextUtils;
import com.l7tech.proxy.datamodel.Ssg;

import java.io.PrintStream;
import java.util.List;
import java.util.Iterator;

/**
 * SSB config command that displays help information.
 */
class HelpCommand extends Command {
    private static final int LEFTWIDTH = 20;     // Size of left-hand column in help
    private static final int MAX_LIST_SSGS = 4;  // Maximum number of Ssg instances to list individually

    protected HelpCommand() {
        super("help", "Show command usage information");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) {
        if (args != null && args.length > 0) {
            String wordStr = args[0];

            // Try to match a word or object
            final Commands sessionCommands = session.getCommands();
            final Nouns sessionNouns = session.getNouns();
            Word word = sessionCommands.getByName(wordStr);
            Words globalCommands = Commands.getInstance();
            if (word == null) word = globalCommands.getByName(wordStr);
            if (word == null) word = sessionNouns.getByName(wordStr);
            if (word == null) word = sessionCommands.getByPrefix(wordStr);
            if (word == null) word = globalCommands.getByPrefix(wordStr);
            if (word == null) word = sessionNouns.getByPrefix(wordStr);

            // If we matched one, show its help page and return
            if (word != null) {
                printWordHelp(session, out, word);
                return;
            }

            // Last check for our own made-up word
            if ("gatewayn".equalsIgnoreCase(wordStr)) {
                printWordHelp(session, out, new Word("gatewayN", "Gateway Account #N (where N is a natural number)") {
                    public String getHelpText() {
                        return SsgNoun.getOverviewHelpText();
                    }
                });
                return;
            }

            out.println("No help is available for '" + wordStr + "' -- it's not a recognized command or object.");
            if (session.isInteractive()) {
                out.println("Type 'help' for a list of available commands and objects.\n");
                return;
            }
            out.println();
        }

        if (!session.isInteractive()) {
            String progname = "BridgeConfig"; // TODO maybe look this up, if possible to do so in Java
            out.println("Usage: " + progname + " command [object [parameter [value]]]\n");
        }

        printAvailableCommands(session, out);
        out.println();
        printAvailableObjects(session, out);
        out.println();
        out.println("Use 'help <command>' or 'help <object>' for more information.\n");
    }

    private void printWordHelp(CommandSession session, PrintStream out, Word word) {
        String header = word.getName() + " - " + word.getDesc();
        if (word instanceof Command) {
            Command command = (Command)word;
            out.print(TextUtils.pad(header, 55));
            out.print(TextUtils.pad("Cmdline:" + (command.isOneshot()?'Y':'N'), 10));
            out.print(TextUtils.pad("Interactive:" + (command.isInteractive()?'Y':'N'), 14));
            out.println();
        } else {
            out.println(header);
        }
        out.println();
        out.println(word.getHelpText());
    }

    private void printAvailableCommands(CommandSession session, PrintStream out) {
        out.println("Available Commands:\n");
        List commands = session.getCommands().getAll();
        for (Iterator i = commands.iterator(); i.hasNext();) {
            Command command = (Command)i.next();
            out.println("  " + TextUtils.pad(command.getName(), LEFTWIDTH) + command.getDesc());
        }
    }

    private void printAvailableObjects(CommandSession session, PrintStream out) {
        out.println("Available Objects:\n");
        List nouns = session.getNouns().getAll();
        List ssgs = session.getSsgManager().getSsgList();
        final int numSsgs = ssgs.size();
        boolean listSsgs = numSsgs <= MAX_LIST_SSGS;

        for (Iterator i = nouns.iterator(); i.hasNext();) {
            Noun noun = (Noun)i.next();
            if (!listSsgs && noun.getName().startsWith("gateway") && !noun.getName().equals("gateways"))
                continue; // Skip listing every single Ssg; we'll summarize them below, instead
            out.println("  " + TextUtils.pad(noun.getName(), LEFTWIDTH) + noun.getDesc());
        }

        if (ssgs.isEmpty()) {
            out.println("  " + TextUtils.pad("gatewayN", LEFTWIDTH) +  "Gateway Account number N");
        } else {
            if (numSsgs > MAX_LIST_SSGS) {
                long low = Long.MAX_VALUE;
                long high = Long.MIN_VALUE;
                for (Iterator iterator = ssgs.iterator(); iterator.hasNext();) {
                    Ssg ssg = (Ssg)iterator.next();
                    long id = ssg.getId();
                    if (id < low) low = id;
                    if (id > high) high = id;
                }
                out.println("  " + TextUtils.pad("gateway" + low + " to gateway" + high, LEFTWIDTH) + "Current Gateway Accounts");
            }
        }
    }
}
