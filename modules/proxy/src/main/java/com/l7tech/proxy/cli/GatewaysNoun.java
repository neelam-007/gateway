/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.TextUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * Configuration noun representing the current set of Ssg objects.
 */
class GatewaysNoun extends Noun {
    private final CommandSession session;

    protected GatewaysNoun(CommandSession session) {
        super("gateways", "The set of all configured Gateway Accounts");
        if (session == null) throw new NullPointerException();
        this.session = session;
        setHelpText(SsgNoun.getOverviewHelpText() +
                "\n\nUse 'show gateways' to list the available gateways, 'show gateway5'\n" +
                "(or 'sh g5') to show just Gateway Account #5, and 'create gateway' to create\n" +
                "a new Gateway Account.\n\n" +
                    "Use 'help gatewayN' (where N is a positive integer) for information about\n" +
                    "properties and special commands available for Gateway Account #N.");
    }

    // Display all relevant info about this noun to the specified output stream.
    public void show(PrintStream out, String[] args) throws CommandException {
        if (args != null && args.length > 0) throw new CommandException("gateways has no property '" + args[0] + "'");
        super.show(out, args);
        List ssgs = session.getSsgManager().getSsgList();
        if (ssgs.size() < 1) {
            out.println("There are currently no Gateway Accounts.  Use 'create gateway' to create one.");
            return;
        }

        out.println(" Proxy      Label      Hostname                       Type      User Name");
        out.println(" ========== ========== ============================== ========= ===============");
                  // ---------1---------2---------3---------4---------5---------6---------7---------8---------9

        boolean sawDefault = false;
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            if (ssg.isDefaultSsg()) {
                out.print('*');
                sawDefault = true;
            } else
                out.print(' ');
            out.print(TextUtils.pad(ssg.makeDefaultLocalEndpoint(), 10));
            out.print(' ');
            out.print(TextUtils.pad(ssg.getLocalEndpoint(), 10));
            out.print(' ');
            out.print(TextUtils.pad(ssg.getSsgAddress(), 30));
            out.print(' ');
            out.print(TextUtils.pad(getGatewayType(ssg), 9));
            out.print(' ');
            out.print(TextUtils.pad(ssg.getUsername(), 15));
            out.println();
        }
        if (sawDefault) out.println("\n* Default Gateway Account");
    }

    private static String getGatewayType(Ssg ssg) {
        if (ssg.isGeneric())
            return "Generic";
        return ssg.isFederatedGateway() ? "Federated" : "Trusted";
    }

    public void create(PrintStream out, String[] args) throws CommandException {
        SsgManager ssgManager = session.getSsgManager();
        Ssg ssg = ssgManager.createSsg();
        if (args == null || args.length <= 0) {
            throw new CommandException("Usage: create gateway <hostname> [<username> [<password>]]");
        }

        ssg.setSsgAddress(args[0]);
        args = ArrayUtils.shift(args);

        if (args.length > 0) {
            ssg.setUsername(args[0]);
            args = ArrayUtils.shift(args);
        }
        if (args.length > 0) {
            ssg.getRuntime().setCachedPassword(args[0].toCharArray());
            ssg.setSavePasswordToDisk(true);
        }
        ssgManager.add(ssg);
        out.println(ssg.makeDefaultLocalEndpoint() + " created");
    }

    public void printHelp(PrintStream out, String[] args) throws CommandException {
        if (args != null && args.length > 0) {
            // Argument was given.  Try to find an SsgNoun to take over.
            Words nouns = session.getNouns();
            List all = nouns.getAll();
            for (Iterator i = all.iterator(); i.hasNext();) {
                Word word = (Word)i.next();
                if (word instanceof SsgNoun) {
                    word.printHelp(out, args);
                    return;
                }
            }

            // We'll have to make something up
            new SsgNoun(session, new Ssg(1)).printHelp(out, args);
            return;
        }

        super.printHelp(out, args);
    }
}
