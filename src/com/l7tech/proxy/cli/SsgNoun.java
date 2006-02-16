/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.TextUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.util.SslUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.security.KeyStoreException;

/**
 * Configuration noun referring to an Ssg instance.
 */
class SsgNoun extends Noun {
    private final CommandSession session;
    final Ssg ssg;
    final Words properties;
    private final Commands specialCommands;

    private static final SsgNoun EXAMPLE = new SsgNoun(null, new Ssg(0));

    public SsgNoun(CommandSession session, Ssg inssg) {
        super(inssg.getLocalEndpoint(), "Gateway Account for " + inssg.getUsername() + "@" + inssg.getSsgAddress());
        this.session = session;
        this.ssg = inssg;
        properties = new Words(Arrays.asList(new NounProperty[] {
                new NounProperty(ssg, "hostname", "SsgAddress", "Hostname or IP address of SecreSpan Gateway"),
                new NounProperty(ssg, "username", "Username", "Username of account on this Gateway"),
                new PasswordProperty(),
                new NounProperty(ssg, "chainCredentials", "ChainCredentialsFromClient", "Chain credentials from client (HTTP Basic)"),
                new NounProperty(ssg, "savePassword", "SavePasswordToDisk", "Save the password in the configuration file"),
                new NounProperty(ssg, "preferSsl", "UseSslByDefault", "Use SSL unless a policy says otherwise"),
                new ServerCertProperty(this),
                new ClientCertProperty(this),
                new DefaultSsgProperty(),
        }));

        this.specialCommands = new Commands(Arrays.asList(new Command[] {
                new DiscoverCommand(this),
                new RequestCommand(),
                new ChangePassCommand(),
                new ImportCommand(),
        }));
    }

    /** @return the Ssg this configuration noun refers to.  Never null. */
    public Ssg getSsg() {
        return ssg;
    }

    /** @return the introductory overview text "The Gateway Accouint is the blah blah blah". */
    public static String getOverviewHelpText() {
        return OVERVIEW_HELP_TEXT;
    }

    /** @return the "Available Properties:" table as a String. */
    public static String getPropertiesText() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        EXAMPLE.printProperties(out);
        out.close();
        return baos.toString();
    }

    /** @return the "Special Commands:" table as a String. */
    public static String getSpecialCommandsText() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        EXAMPLE.printSpecialCommands(out);
        out.close();
        return baos.toString();
    }

    /** @return help text for a generic (ie, not attached to a particular Ssg instance) SsgNoun. */
    public static String getGenericHelpText() {
        return EXAMPLE.getHelpText();
    }

    private void printWords(PrintStream out, Words words) {
        List props = words.getAll();
        for (Iterator i = props.iterator(); i.hasNext();) {
            Word word = (Word)i.next();
            out.print(TextUtils.pad(word.getName(), 20));
            out.print(' ');
            out.println(word.getDesc());
        }

    }

    private void printProperties(PrintStream out) {
        out.println("Available properties:\n");
        printWords(out, properties);
    }

    private void printSpecialCommands(PrintStream out) {
        out.println("Special commands:\n");
        printWords(out, specialCommands);
    }

    public String getHelpText() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        out.println(getOverviewHelpText());
        out.println();
        printProperties(out);
        out.println();
        printSpecialCommands(out);
        out.println();
        out.println(EXAMPLE_COMMANDS);

        out.close();
        return baos.toString();
    }

    public void create(PrintStream out, String[] args) throws CommandException {
        // This message shouldn't have gotten here.  Pass the buck to the main gateways object
        Noun gateways = (Noun)session.getNouns().getByName("gateways");
        if (gateways == null) throw new IllegalStateException("No gateways object");
        gateways.create(out, args);
    }

    public void delete(final PrintStream out, String[] args) throws CommandException {
        if (args != null && args.length > 0 && !"force".equalsIgnoreCase(args[0])) {
            NounProperty prop = (NounProperty)properties.getByName(args[0]);
            if (prop == null)
                throw new CommandException(getName() + " has no property " + args[0] + ".  (Note that 'delete' doesn't allow abbreviations.)");
            prop.delete();
            return;
        }

        if (ssg.getClientCertificate() != null) {
            if (args == null || args.length < 1 || !args[0].equalsIgnoreCase("force")) {
                out.println(getName() + " has a client certificate.  Deleting this gateway will destroy");
                out.println("this certificate and private key.  To proceed, use 'delete " + getName() + " force'");
                return;
            }
        }

        ssg.setTrustedGateway(null); // break federation prior to removing key stores
        ssg.setWsTrustSamlTokenStrategy(null); // break federation prior to removing key stores TODO fix hack
        final File kf = ssg.getKeyStoreFile();
        final File ts = ssg.getTrustStoreFile();
        final boolean haveKf = kf.exists();
        final boolean haveTs = ts.exists();
        try {
            session.getSsgManager().remove(ssg);
            session.onChangesMade();
            if (ssg.getRuntime().getSsgKeyStoreManager().deleteStores()) {
                if (haveKf) out.println((kf.exists() ? "Failed to delete " : "Deleted ") + kf);
                if (haveTs) out.println((ts.exists() ? "Failed to delete " : "Deleted ") + ts);
            }
        } catch (SsgNotFoundException e) {
            // Was it already removed in the meantime?  Oh well.  We'll eat this and just not delete it.
        }
    }

    public void set(PrintStream out, String propertyName, String[] args) throws CommandException {
        if (propertyName == null || propertyName.length() < 1)
            throw new CommandException("Use 'help " + getName() + "' for a description of available properties.");

        NounProperty prop = (NounProperty)properties.getByPrefix(propertyName);
        if (prop == null) throw new CommandException("Unknown property '" + propertyName + "'.  Use 'help " + getName() + "' for a list.");

        prop.set(args);
    }

    // Display all relevant info about this noun to the specified output stream.
    public void show(PrintStream out, String[] args) throws CommandException {
        super.show(out, args);

        if (args != null && args.length > 0) {
            String arg = args[0];
            //noinspection UnusedAssignment
            args = ArrayUtils.shift(args);
            NounProperty prop = (NounProperty)properties.getByPrefix(arg);
            if (prop == null) throw new CommandException("Unknown property '" + arg + "'.  Use 'help " + getName() + "' for a list.");
            out.println("  " + prop.getName() + " - " + prop.getDesc() + "\n");
            prop.printValue(out, false);
            return;
        }

        out.println("Property            Value");
        out.println("=================== ====================================================");

        List props = this.properties.getAll();
        for (Iterator i = props.iterator(); i.hasNext();) {
            NounProperty prop = (NounProperty)i.next();
            out.print(TextUtils.pad(prop.getName(), 19));
            out.print(' ');
            prop.printValue(out, true);
            out.println();
        }
    }

    protected boolean executeSpecial(CommandSession session, PrintStream out, String cmdName, String[] args) throws CommandException {
        Command command = (Command)specialCommands.getByPrefix(cmdName);
        if (command == null)
            return false;
        command.execute(session, out, args);
        return true;
    }

    private static final String OVERVIEW_HELP_TEXT =
            "The Gateway Account is at the heart of the SecureSpan Bridge.  Each \n" +
                    "Gateway Account holds a set of Bridge credentials for a target Gateway.";

    private static final String EXAMPLE_COMMANDS = "Example Commands: show gateways\n" +
            "                  show gateway37\n" +
            "                  create gateway ssg.example.com testuser secret\n" +
            "                  set gateway1 password\n" +
            "                  delete g3";


    /**
     * Property representing cached password.
     */
    private class PasswordProperty extends NounProperty {
        public PasswordProperty() {
            super(SsgNoun.this.ssg.getRuntime(), "password", "CachedPassword", "Password of account on this Gateway");
        }

        public void printValue(PrintStream out, boolean singleLine) {
            char[] pass = (char[])getValue();
            if (pass == null) {
                out.print("<not yet set>");
            } else {
                if (pass.length < 1)
                    out.print("<empty password>");
                else
                    out.print("<present but not shown>");
            }
            if (!singleLine) out.println();
        }

        protected Object getValue() {
            PasswordAuthentication creds = ssg.getRuntime().getCredentials();
            return creds == null ? null : creds.getPassword();
        }

        public void set(String[] args) throws CommandException {
            if (args == null || args.length < 1 || args[0] == null)
                throw new CommandException("Password must be specified.");
            ssg.getRuntime().setCachedPassword(args[0].toCharArray());
            if (ssg.isSavePasswordToDisk())
                return;
            final PrintStream out = session.getOut();
            if (!session.isInteractive()) {
                // If not interactive, this oneshot command would be completely useless if we didn't save the passwd
                out.println("NOTE: Setting savePassword to true for " + SsgNoun.this.getName());
                ssg.setSavePasswordToDisk(true);
            } else {
                // Interactive mode.  Warn the admin that password is being saved only for this session
                out.println("NOTE: password set for session only: savePassword not enabled for " + SsgNoun.this.getName());
                out.println("To save password to disk now, use '" + SsgNoun.this.getName() + " set savePassword true'");
            }
        }
    }

    private class DefaultSsgProperty extends NounProperty {
        public DefaultSsgProperty() {
            super(SsgNoun.this.ssg, "default", "DefaultSsg", "Make this the default Gateway Account");
        }

        public void set(String[] args) throws CommandException {
            // For this property, we will accept setting without an argument to mean "make this one the default"
            if (args == null || args.length < 1)
                setValue(Boolean.TRUE);
            else
                super.set(args);
        }

        // Hook setValue to make sure other Ssgs get downgraded when this one is upgraded to default
        protected void setValue(Object value) throws CommandException {
            super.setValue(value);
            if (((Boolean)value).booleanValue()) {
                try {
                    SsgNoun.this.session.getSsgManager().setDefaultSsg(ssg);
                } catch (SsgNotFoundException e) {
                    throw new CommandException("Gateway Account no longer exists");
                }
            }
        }
    }

    private class RequestCommand extends Command {
        public RequestCommand() {
            super("request", "Request client certificate");
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            if (args == null || args.length < 1 || args[0].length() < 1)
                throw new CommandException("Usage: " + SsgNoun.this.getName() + " request clientCert\n\n" +
                                           "Requires that a username and password be configured.");

            PasswordAuthentication creds = ssg.getRuntime().getCredentials();
            if (creds == null)
                throw new CommandException("To apply for a client certificate, first set a username and password:\n" +
                                           "    " + SsgNoun.this.getName() + " set username alice\n" +
                                           "    " + SsgNoun.this.getName() + " set password s3cr3t");

            if (ssg.getServerCertificate() == null) {
                session.getOut().println("Attempting automatic server certificate discovery...");
                new DiscoverCommand(SsgNoun.this).execute(session, out, new String[] { "serverCert" });
            }

            try {
                ssg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(creds);
                session.onChangesMade();
                out.println("Now using client certificate with DN: " + ssg.getClientCertificate().getSubjectDN().getName());
            } catch (BadCredentialsException e) {
                throw new CommandException("Gateway indicates invalid current credentials (bad password? missing cert?)", e);
            } catch (CertificateAlreadyIssuedException e) {
                throw new CommandException("The Gateway has already issued a certificate to this account.  Please use\n" +
                                           "a different account, or have the Gateway administrator revoke the old cert");
            } catch (Exception e) {
                throw new CommandException("Unable to obtain client certificate: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    private class ChangePassCommand extends Command {
        public ChangePassCommand() {
            super("changePass", "Ask Gateway to change password and revoke client cert");
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            if (args == null || args.length < 1 || args[0].length() < 1)
                throw new CommandException("Usge: " + SsgNoun.this.getName() + " changePass <new password>");

            PasswordAuthentication oldpass = ssg.getRuntime().getCredentials();
            if (oldpass == null) {
                throw new CommandException("To change your password, first set a username and existing password:\n" +
                                           "    " + SsgNoun.this.getName() + " set username alice\n" +
                                           "    " + SsgNoun.this.getName() + " set password s3cr3t");
            }

            try {
                final char[] newpass = args[0].toCharArray();
                SslUtils.changePasswordAndRevokeClientCertificate(ssg,
                                                                  oldpass.getUserName(),
                                                                  oldpass.getPassword(),
                                                                  newpass);
                ssg.getRuntime().setCachedPassword(newpass);
                ssg.getRuntime().getSsgKeyStoreManager().deleteClientCert();

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
            } finally {
                session.onChangesMade();
            }

        }
    }

    private class ImportCommand extends Command {
        protected ImportCommand() {
            super("import", "Import a client or server certificate");
        }

        public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
            if (args == null || args.length < 2 || args[0] == null || args[1] == null)
                throw new CommandException("Usage: " + SsgNoun.this.getName() + " import serverCert <path of PEM or DER file>\n" +
                                           "       " + SsgNoun.this.getName() + " import clientCert <path of PKCS#12 file> <pass phrase> [<alias>]");

            try {
                throw new CommandException("Not yet implemented");
            } finally {
                session.onChangesMade();
            }
        }
    }
}
