package com.l7tech.server.transport.ftp;

import org.apache.ftpserver.command.Command;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.command.NotSupportedCommand;
import org.apache.ftpserver.command.impl.*;

import java.util.HashMap;

import static com.l7tech.common.ftp.FtpCommand.*;

/**
 * Self-contained implementation of CommandFactory that has no need for a CommandFactoryFactory.
 *
 * The UploadOnlyCommandFactory provides routing support for STOR and STOU commands only.
 *
 * <p>This implementation cannot be extended via configuration.</p>
 *
 * @author Steve Jones
 * @author Jamie Williams - jamie.williams2@ca.com
 */
class UploadOnlyCommandFactory implements CommandFactory {

    /**
     * Add this to the supported extensions if/when explicit FTPS is needed:
     *
     *   \n AUTH SSL\n AUTH TLS
     */
    private final static String[] EXTENSIONS =  new String[] {
            "SIZE",
            "MDTM",
            "REST STREAM",
            "LANG en",
            "MLST Size;Modify;Type;Perm",
            "MODE Z",
            "UTF8",
            "EPRT",
            "EPSV",
            "PASV",
            "TVFS",
    };

    private final HashMap<String, Command> commandMap;

    public UploadOnlyCommandFactory() {
        commandMap = new HashMap<>();

        // Commands are RFC 959 unless otherwise noted
        commandMap.put("ABOR", new ABOR());
        commandMap.put("ACCT", new ACCT());
        commandMap.put("APPE", new APPE());
        commandMap.put("AUTH", new AUTH()); // RFC 2228 Security Extensions
        commandMap.put("CDUP", new CDUP());
        commandMap.put("CWD",  new CWD());
        commandMap.put("DELE", new DELE());
        commandMap.put("EPRT", new EPRT()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("EPSV", new EPSV()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("FEAT", new FtpCommands.FEAT(EXTENSIONS)); // RFC 2389 Feature negotiation
        commandMap.put("HELP", new HELP());
        commandMap.put("LANG", new FtpCommands.LANG()); // RFC 2640 I18N
        commandMap.put("LIST", new LIST());
        commandMap.put("MDTM", new MDTM()); // RFC 3659 Extensions to FTP
        commandMap.put("MKD",  new MKD());
        commandMap.put("MLSD", new MLSD()); // RFC 3659 Extensions to FTP
        commandMap.put("MLST", new MLST()); // RFC 3659 Extensions to FTP
        commandMap.put("MODE", new MODE());
        commandMap.put("NLST", new NLST());
        commandMap.put("NOOP", new NOOP());
        commandMap.put("OPTS", new OPTS()); // RFC 2389 Feature negotiation
        commandMap.put("PASS", new PASS());
        commandMap.put("PASV", new PASV());
        commandMap.put("PBSZ", new PBSZ()); // RFC 2228 Security Extensions
        commandMap.put("PORT", new PORT());
        commandMap.put("PROT", new PROT()); // RFC 2228 Security Extensions
        commandMap.put("PWD",  new PWD());
        commandMap.put("QUIT", new QUIT());
        commandMap.put("REIN", new REIN());
        commandMap.put("REST", new REST()); // RFC 3659 Extensions to FTP
        commandMap.put("RETR", new RETR());
        commandMap.put("RMD",  new RMD());
        commandMap.put("RNFR", new RNFR()); // reply "not supported"
        commandMap.put("RNTO", new RNTO()); // reply "not supported"
        commandMap.put("SITE", new NotSupportedCommand()); // reply "not supported"
        commandMap.put("SIZE", new SIZE()); // RFC 3659 Extensions to FTP
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
