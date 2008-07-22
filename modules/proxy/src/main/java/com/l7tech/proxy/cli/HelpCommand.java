/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.TextUtils;
import com.l7tech.util.ArrayUtils;
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
                  //---------|---------|---------|---------|---------|---------|---------|       ||
        setHelpText("The help command shows additional information about a global command, object,\n" +
                    "property, or special command.\n" +
                    "\n" +
                    "    Usage: help\n" +
                    "           help <global command>\n" +
                    "           help <object>\n" +
                    "           help <object> <property>\n" +
                    "           help <object> <special command>\n" +
                    "\n" +
                    " Examples: help gateway1\n" +
                    "           h g3 disco");

    }

    public void execute(final CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args != null && args.length > 0) {
            String wordStr = args[0];
            args = ArrayUtils.shift(args);

            // Try to match a word or object
            final Commands sessionCommands = session.getCommands();
            final Words sessionNouns = session.getNouns();
            Word word = sessionCommands.getByName(wordStr);
            Words globalCommands = Commands.getInstance();
            if (word == null) word = globalCommands.getByName(wordStr);
            if (word == null) word = sessionNouns.getByName(wordStr);
            if (word == null) word = sessionCommands.getByPrefix(wordStr);
            if (word == null) word = globalCommands.getByPrefix(wordStr);
            if (word == null) word = sessionNouns.getByPrefix(wordStr);

            // If we matched one, show its help page and return
            if (word != null) {
                word.printHelp(out, args);
                return;
            }

            // Last check for someone trying to get help on our made-up word "gatewayN"
            if ("gatewayn".equalsIgnoreCase(wordStr)) {
                word = new Word("gatewayN",
                                "Gateway Account #N (where N is a natural number)",
                                SsgNoun.getGenericHelpText());
                word.printHelp(out, args);
                return;
            }

            out.println("No help is available for '" + wordStr + "' -- it's not a global command or object.");
            if (session.isInteractive()) {
                out.println("Use 'help' for a list of global commands and objects.");
                return;
            }
            out.println();
        }

        if (!session.isInteractive()) {
            String progname = "BridgeConfig"; // TODO maybe look this up, if possible to do so in Java
            out.println("Usage: " + progname + " <global command> [<object> [<parameter> [<value>]]]");
            out.println("       " + progname + " <object> <special command> [<parameter> [<value>]]\n");
        }

        printAvailableCommands(session, out);
        out.println();
        printAvailableObjects(session, out);
        out.println();
        out.println("Most names can be abbreviated (i.e. 'show' to 'sh', or 'gateway17' to 'g17')");
        out.println("Objects may have extra commands of their own (i.e. 'gateway4 discover')");
        out.println("Use 'help <command>' or 'help <object>' for more information.");
    }

    private void printAvailableCommands(CommandSession session, PrintStream out) {
        out.println("Global Commands:\n");
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
            if (!listSsgs && noun instanceof SsgNoun)
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
