/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgRuntime;

import java.io.PrintStream;
import java.io.IOException;

/**
 * Special command to clone one account's configuration over top of anothers.
 */
class CopyToCommand extends Command {
    private final SsgNoun ssgNoun;

    protected CopyToCommand(SsgNoun ssgNoun) {
        super("copyTo", "Copy " + ssgNoun.getName() + " configuration");
        this.ssgNoun = ssgNoun;
        setHelpText("Use this command to copy the " + ssgNoun.getName() + " configuration over top of another\n" +
                    "Gateway Account's configuration.  This includes stored passwords and client\n" +
                    "and server certificates.  Keystore files will be copied as needed.\n\n" +
                    "   Usage: " + ssgNoun.getName() + " copyTo <target gateway>\n\n" +
                    " Example: g7 copyTo g4");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0] == null)
            throw new CommandException("Usage: " + ssgNoun.getName() + " copyTo <target gateway>");
        final String targetStr = args[0];
        args = ArrayUtils.shift(args);

        Noun target = (Noun)session.getNouns().getByPrefix(targetStr);
        if (!(target instanceof SsgNoun))
            throw new CommandException("No Gateway Account was found matching '" + targetStr + "'");
        SsgNoun dstNoun = (SsgNoun)target;
        final Ssg dstSsg = dstNoun.ssg;
        final Ssg srcSsg = ssgNoun.ssg;
        if (dstSsg.getId() == srcSsg.getId())
            throw new CommandException("Unable to copy " + ssgNoun.getName() + " configuration on to itself");

        if (dstSsg.getClientCertificate() != null &&
                (args == null || args.length < 1 || !args[0].equalsIgnoreCase("force")))
        {
            throw new CommandException(
                    dstNoun.getName() + " has a client certificate.  Copying a configuration onto this gateway\n" +
                            "will destroy its private key.  To proceed, use '" +
                            ssgNoun.getName() + " copyTo " + dstNoun.getName() + " force'");
        }

        final SsgRuntime dstRuntime = dstSsg.getRuntime();
        final SsgRuntime srcRuntime = srcSsg.getRuntime();

        dstRuntime.getSsgKeyStoreManager().deleteStores();
        dstSsg.copyFrom(srcSsg);
        dstRuntime.setCachedPassword(srcRuntime.getCachedPassword());

        // Restore the keystore and trust store files to default before copying
        final Ssg defaultDst = new Ssg(dstSsg.getId());
        dstSsg.setKeyStoreFile(defaultDst.getKeyStoreFile());
        dstSsg.setTrustStoreFile(defaultDst.getTrustStoreFile());

        try {
            if (srcSsg.getTrustStoreFile().exists())
                FileUtils.copyFile(srcSsg.getTrustStoreFile(), dstSsg.getTrustStoreFile());
        } catch (IOException e) {
            throw new CommandException("Unable to copy certs file: " + ExceptionUtils.getMessage(e), e);
        }

        try {
            if (srcSsg.getKeyStoreFile().exists())
                FileUtils.copyFile(srcSsg.getKeyStoreFile(), dstSsg.getKeyStoreFile());
        } catch (IOException e) {
            throw new CommandException("Unable to copy keys file: " + ExceptionUtils.getMessage(e), e);
        }

        session.onChangesMade();
    }
}
