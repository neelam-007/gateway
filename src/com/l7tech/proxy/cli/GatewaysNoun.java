/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.TextUtils;
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
        setHelpText(SsgNoun.getOverviewHelpText() + "\n\nUse 'show gateways' to list the available gateways, 'show gateway5' (or 'sh g5') to show\n" +
                    "just Gateway Account #5, and 'create gateway' to create a new Gateway Account.\n\n" +
                    SsgNoun.getPropertiesText() + "\n" + SsgNoun.getSpecialCommandsText());
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

        out.println("Proxy      Hostname                         Type      User Name");
        out.println("========== ================================ ========= =================");

        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg)i.next();
            out.print(TextUtils.pad(ssg.getLocalEndpoint(), 10));
            out.print(' ');
            out.print(TextUtils.pad(ssg.getSsgAddress(), 32));
            out.print(' ');
            out.print(TextUtils.pad((ssg.isFederatedGateway() ? "Federated" : "Trusted"), 9));
            out.print(' ');
            out.print(TextUtils.pad(ssg.getUsername(), 17));
            out.println();
        }
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
        out.println(ssg.getLocalEndpoint() + " created");
    }
}
