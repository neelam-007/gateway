package com.l7tech.proxy.cli;

import com.l7tech.common.security.kerberos.KerberosUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.proxy.datamodel.SsgFinderImpl;

import java.io.File;
import java.io.PrintStream;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class KerberosCommand extends Command {

    public KerberosCommand() {
        super("kerberos", "Configure Kerberos KDC and Realm");
        setMinAbbrev(4);
        setHelpText("Use this command to manually configure Kerberos for use with all gateways.\n" +
                    "\n" +
                    "  Usage: kerberos <kdc-host> <realm>\n" +
                    "\n" +
                    "Example: kerberos 10.0.0.1 MYREALM.COM\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null)
            throw new CommandException(usage());

        try {
            File configDir = new File(SsgFinderImpl.getSsgFinderImpl().getStorePath()).getParentFile();
            KerberosUtils.configureKerberos(new File(configDir, "krb5.conf"), args[0], args[1].toUpperCase());
        }
        catch(Exception e) {
            throw new CommandException(ExceptionUtils.getMessage(e));
        }
    }

    private String usage() {
        return "Usage: kerberos <kdc-host> <realm>";
    }
}
