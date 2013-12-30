package com.l7tech.server.transport.ftp;

import java.util.HashMap;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.NotSupportedCommand;
import org.apache.ftpserver.command.impl.*;

import static com.l7tech.common.ftp.FtpCommand.*;
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

        // Commands are RFC 959 unless otherwise noted
        commandMap.put("ABOR", new ABOR());
        commandMap.put("ACCT", new ACCT());
        commandMap.put("APPE", new ProxyFtpCommand(APPE));
        commandMap.put("AUTH", new AUTH()); // RFC 2228 Security Extensions
        commandMap.put("CDUP", new ProxyFtpCommand(CDUP));
        commandMap.put("CWD",  new ProxyFtpCommand(CWD));
        commandMap.put("DELE", new ProxyFtpCommand(DELE));
        commandMap.put("EPRT", new EPRT()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("EPSV", new EPSV()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("FEAT", new FtpCommands.FEAT()); // RFC 2389 Feature negotiation
        commandMap.put("HELP", new HELP());
        commandMap.put("LANG", new FtpCommands.LANG()); // RFC 2640 I18N
        commandMap.put("LIST", new ProxyFtpCommand(LIST));
        commandMap.put("MDTM", new ProxyFtpCommand(MDTM)); // RFC 3659 Extensions to FTP
        commandMap.put("MKD",  new ProxyFtpCommand(MKD));
        commandMap.put("MLSD", new ProxyFtpCommand(MLSD)); // RFC 3659 Extensions to FTP
        commandMap.put("MLST", new ProxyFtpCommand(MLST)); // RFC 3659 Extensions to FTP
        commandMap.put("MODE", new MODE());
        commandMap.put("NLST", new ProxyFtpCommand(NLST));
        commandMap.put("NOOP", new NOOP());
        commandMap.put("OPTS", new OPTS()); // RFC 2389 Feature negotiation // TODO jwilliams: implement OPTS & OPTS_MLST (affects MLST response); OPTS_UTF8 can keep the library implementation
        commandMap.put("PASS", new PASS());
        commandMap.put("PASV", new PASV());
        commandMap.put("PBSZ", new PBSZ()); // RFC 2228 Security Extensions
        commandMap.put("PORT", new PORT());
        commandMap.put("PROT", new PROT()); // RFC 2228 Security Extensions
        commandMap.put("PWD",  new ProxyFtpCommand(PWD));
        commandMap.put("QUIT", new QUIT());
        commandMap.put("REIN", new REIN());
        commandMap.put("REST", new REST()); // RFC 3659 Extensions to FTP
        commandMap.put("RETR", new ProxyFtpCommand(RETR));
        commandMap.put("RMD",  new ProxyFtpCommand(RMD));
        commandMap.put("RNFR", new NotSupportedCommand()); // reply "not supported" // TODO jwilliams: check NotSupportedCommands work
        commandMap.put("RNTO", new NotSupportedCommand()); // reply "not supported"
        commandMap.put("SITE", new NotSupportedCommand()); // reply "not supported"
        commandMap.put("SIZE", new ProxyFtpCommand(SIZE)); // RFC 3659 Extensions to FTP
        commandMap.put("STAT", new STAT());
        commandMap.put("STOR", new ProxyFtpCommand(STOR));
        commandMap.put("STOU", new ProxyFtpCommand(STOU));
        commandMap.put("STRU", new STRU());
        commandMap.put("SYST", new SYST());
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
