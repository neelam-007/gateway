/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.util.SslUtils;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.BadPasswordFormatException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.util.ExceptionUtils;

import java.io.PrintStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.KeyStoreException;

/**
 * Special command to ask the Gateway to change your password and revoke your client cert.
 */
class ChangePassCommand extends Command {
    private SsgNoun ssgNoun;

    public ChangePassCommand(SsgNoun ssgNoun) {
        super("changePass", "Change password and revoke client cert");
        this.ssgNoun = ssgNoun;
        setHelpText("This command contacts the Gateway and requests it to change the password for\n" +
                    "this Gateway Account.  Changing the password for a Gateway Account also\n" +
                    "revokes any certificate issued by the Gateway for that account.\n\n" +
                    "  Usage: " + ssgNoun.getName() + " changePass <new password>\n\n" +
                    "Example: " + ssgNoun.getName() + " changePass abc123");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + ssgNoun.getName() + " changePass <new password>");

        PasswordAuthentication oldpass = ssgNoun.ssg.getRuntime().getCredentials();
        if (oldpass == null) {
            throw new CommandException("To change your password, first set a username and existing password:\n" +
                                       "    " + ssgNoun.getName() + " set username alice\n" +
                                       "    " + ssgNoun.getName() + " set password s3cr3t");
        }

        try {
            final char[] newpass = args[0].toCharArray();
            SslUtils.changePasswordAndRevokeClientCertificate(ssgNoun.ssg,
                                                              oldpass.getUserName(),
                                                              oldpass.getPassword(),
                                                              newpass);
            ssgNoun.ssg.getRuntime().setCachedPassword(newpass);
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().deleteClientCert();

        } catch (IOException e) {
            throw new CommandException("Unable to change password and revoke client cert: " + ExceptionUtils.getMessage(e), e);
        } catch (BadCredentialsException e) {
            throw new CommandException("Gateway indicates invalid current credentials (bad password? missing cert?)", e);
        } catch (BadPasswordFormatException e) {
            throw new CommandException("Gateway rejected the new password (too short?)", e);
        } catch (SslUtils.PasswordNotWritableException e) {
            throw new CommandException("Gateway is unable to change the password for this account", e);
        } catch (KeyStoreCorruptException e) {
            throw new CommandException("Unable to revoke client certificate -- keystore damaged: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // shouldn't happen
        } catch (RuntimeException e) {
            if (!ExceptionUtils.causedBy(e, CommandSessionCredentialManager.BadKeystoreException.class))
                throw e;

            throw new CommandException("Bad current password, or certs or keys file is corrupt for " + 
                                       ssgNoun.getName() + ": " + ExceptionUtils.getMessage(e) + "\n\n" +
                                       "Use '" + ssgNoun.getName() + " delete' to delete these key stores.",
                                       e);
        } finally {
            session.onChangesMade();
        }
    }
}
