package com.l7tech.server.transport.ftp;

import java.util.HashMap;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.impl.*;

/**
 * Self-contained implementation of CommandFactory that has no need for a CommandFactoryFactory.
 *
 * <p>This implementation cannot be extended via configuration.</p>
 *
 * @author Steve Jones
 * @author jwilliams
 */
class FtpCommandFactory implements CommandFactory {
    private final HashMap<String, Command> commandMap;

    public FtpCommandFactory() {
        commandMap = new HashMap<>();

        // first populate the default command list
        // Commands are RFC 959 unless otherwise noted
        commandMap.put("ABOR", new ABOR());
        commandMap.put("ACCT", new FtpCommands.ACCT());
        commandMap.put("APPE", new FtpCommands.APPE());
        commandMap.put("AUTH", new AUTH()); // RFC 2228 Security Extensions
        commandMap.put("CDUP", new FtpCommands.CDUP());
        commandMap.put("CWD",  new FtpCommands.CWD());
        commandMap.put("DELE", new FtpCommands.DELE());
        commandMap.put("EPRT", new EPRT()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("EPSV", new EPSV()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("FEAT", new FtpCommands.FEAT()); // RFC 2389 Feature negotiation
        commandMap.put("HELP", new HELP());
        commandMap.put("LANG", new FtpCommands.LANG()); // RFC 2640 I18N
        commandMap.put("LIST", new FtpCommands.LIST());
        commandMap.put("MDTM", new FtpCommands.MDTM()); // RFC 3659 Extensions to FTP
        commandMap.put("MKD",  new FtpCommands.MKD());
        commandMap.put("MLSD", new FtpCommands.MLSD()); // RFC 3659 Extensions to FTP
        commandMap.put("MLST", new FtpCommands.MLST()); // RFC 3659 Extensions to FTP
        commandMap.put("MODE", new MODE());
        commandMap.put("NLST", new FtpCommands.NLST());
        commandMap.put("NOOP", new NOOP());
        commandMap.put("OPTS", new OPTS()); // RFC 2389 Feature negotiation // TODO jwilliams: implement OPTS & OPTS_MLST (affects MLST response); OPTS_UTF8 can keep the library implementation
        commandMap.put("PASS", new PASS());
        commandMap.put("PASV", new PASV());
        commandMap.put("PBSZ", new PBSZ()); // RFC 2228 Security Extensions
        commandMap.put("PORT", new PORT());
        commandMap.put("PROT", new PROT()); // RFC 2228 Security Extensions
        commandMap.put("PWD",  new FtpCommands.PWD());
        commandMap.put("QUIT", new QUIT());
        commandMap.put("REIN", new REIN());
        commandMap.put("REST", new REST()); // RFC 3659 Extensions to FTP // TODO jwilliams: make a command? set local attribute, AND pass in request?
        commandMap.put("RETR", new FtpCommands.RETR());
        commandMap.put("RMD",  new FtpCommands.RMD());
        commandMap.put("RNFR", new FtpCommands.RNFR());
        commandMap.put("RNTO", new FtpCommands.RNTO());
        commandMap.put("SITE", new FtpCommands.SITE());
        commandMap.put("SIZE", new FtpCommands.SIZE()); // RFC 3659 Extensions to FTP
        commandMap.put("STAT", new STAT());
        commandMap.put("STOR", new FtpCommands.STOR());
        commandMap.put("STOU", new FtpCommands.STOU());
        commandMap.put("STRU", new STRU());
        commandMap.put("SYST", new SYST()); // TODO jwilliams: custom command to obscure info?
        commandMap.put("TYPE", new TYPE());
        commandMap.put("USER", new USER());

        // Unimplemented commands
        //commandMap.put("MD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5
        //commandMap.put("MMD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5

        // Full list for RFC 2228 Security Extensions
        //      AUTH (Authentication/Security Mechanism),
        //      ADAT (Authentication/Security Data),
        //      PROT (Data Channel Protection Level),
        //      PBSZ (Protection Buffer Size),
        //      CCC (Clear Command Channel),
        //      MIC (Integrity Protected Command),
        //      CONF (Confidentiality Protected Command), and
        //      ENC (Privacy Protected Command).
    }

    /**
     * Get command. Returns null if not found.
     */
    public Command getCommand(String cmdName) {
        if (cmdName == null || cmdName.equals("")) {
            return null;
        }

        return commandMap.get(cmdName.toUpperCase());
    }
}
