package com.l7tech.server.transport.ftp;

import java.util.HashMap;

import org.apache.ftpserver.interfaces.CommandFactory;
import org.apache.ftpserver.interfaces.Command;

/**
 * Cut down copy of the {@link org.apache.ftpserver.DefaultCommandFactory DefaultCommandFactory}
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

//        // first populate the default command list
//        // Commands are RFC 959 unless otherwise noted
//        commandMap.put("ABOR", new org.apache.ftpserver.command.ABOR());
//        commandMap.put("ACCT", new org.apache.ftpserver.command.ACCT());
//        commandMap.put("APPE", new org.apache.ftpserver.command.APPE());
//        commandMap.put("AUTH", new org.apache.ftpserver.command.AUTH()); // RFC 2228 Security Extensions
//        commandMap.put("CDUP", new org.apache.ftpserver.command.CDUP());
//        commandMap.put("CWD",  new org.apache.ftpserver.command.CWD());
//        commandMap.put("DELE", new org.apache.ftpserver.command.DELE());
//        commandMap.put("EPRT", new org.apache.ftpserver.command.EPRT()); // RFC 2428 Nat / IPv6 extensions
//        commandMap.put("EPSV", new org.apache.ftpserver.command.EPSV()); // RFC 2428 Nat / IPv6 extensions
//        commandMap.put("FEAT", new com.l7tech.server.transport.ftp.FtpCommands.FEAT()); // RFC 2389 Feature negotiation
//        commandMap.put("HELP", new org.apache.ftpserver.command.HELP());
//        commandMap.put("LANG", new com.l7tech.server.transport.ftp.FtpCommands.LANG()); // RFC 2640 I18N
//        commandMap.put("LIST", new org.apache.ftpserver.command.LIST());
//        //commandMap.put("MD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5
//        //commandMap.put("MMD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5
//        commandMap.put("MDTM", new org.apache.ftpserver.command.MDTM()); // RFC 3659 Extensions to FTP
//        commandMap.put("MKD",  new org.apache.ftpserver.command.MKD());
//        commandMap.put("MLSD", new org.apache.ftpserver.command.MLSD()); // RFC 3659 Extensions to FTP
//        commandMap.put("MLST", new org.apache.ftpserver.command.MLST()); // RFC 3659 Extensions to FTP
//        commandMap.put("MODE", new org.apache.ftpserver.command.MODE());
//        commandMap.put("NLST", new org.apache.ftpserver.command.NLST());
//        commandMap.put("NOOP", new org.apache.ftpserver.command.NOOP());
//        commandMap.put("OPTS", new org.apache.ftpserver.command.OPTS()); // RFC 2389 Feature negotiation
//        commandMap.put("PASS", new org.apache.ftpserver.command.PASS());
//        commandMap.put("PASV", new org.apache.ftpserver.command.PASV());
//        commandMap.put("PBSZ", new org.apache.ftpserver.command.PBSZ()); // RFC 2228 Security Extensions
//        commandMap.put("PORT", new org.apache.ftpserver.command.PORT());
//        commandMap.put("PROT", new org.apache.ftpserver.command.PROT()); // RFC 2228 Security Extensions
//        commandMap.put("PWD",  new org.apache.ftpserver.command.PWD());
//        commandMap.put("QUIT", new org.apache.ftpserver.command.QUIT());
//        commandMap.put("REIN", new org.apache.ftpserver.command.REIN());
//        commandMap.put("REST", new org.apache.ftpserver.command.REST()); // RFC 3659 Extensions to FTP
//        commandMap.put("RETR", new org.apache.ftpserver.command.RETR());
//        commandMap.put("RMD",  new org.apache.ftpserver.command.RMD());
//        commandMap.put("RNFR", new org.apache.ftpserver.command.RNFR());
//        commandMap.put("RNTO", new org.apache.ftpserver.command.RNTO());
//        commandMap.put("SITE", new org.apache.ftpserver.command.SITE());
//        commandMap.put("SIZE", new org.apache.ftpserver.command.SIZE()); // RFC 3659 Extensions to FTP
//        commandMap.put("STAT", new org.apache.ftpserver.command.STAT());
//        commandMap.put("STOR", new org.apache.ftpserver.command.STOR());
//        commandMap.put("STOU", new org.apache.ftpserver.command.STOU());
//        commandMap.put("STRU", new org.apache.ftpserver.command.STRU());
//        commandMap.put("SYST", new org.apache.ftpserver.command.SYST());
//        commandMap.put("TYPE", new org.apache.ftpserver.command.TYPE());
//        commandMap.put("USER", new org.apache.ftpserver.command.USER());// first populate the default command list

        // Commands are RFC 959 unless otherwise noted
        commandMap.put("ABOR", new org.apache.ftpserver.command.ABOR());
        commandMap.put("ACCT", new FtpCommands.ACCT());
        commandMap.put("APPE", new FtpCommands.APPE());
        commandMap.put("AUTH", new org.apache.ftpserver.command.AUTH()); // RFC 2228 Security Extensions
        commandMap.put("CDUP", new FtpCommands.CDUP());
        commandMap.put("CWD",  new FtpCommands.CWD());
        commandMap.put("DELE", new FtpCommands.DELE());
        commandMap.put("EPRT", new org.apache.ftpserver.command.EPRT()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("EPSV", new org.apache.ftpserver.command.EPSV()); // RFC 2428 Nat / IPv6 extensions
        commandMap.put("FEAT", new com.l7tech.server.transport.ftp.FtpCommands.FEAT()); // RFC 2389 Feature negotiation
        commandMap.put("HELP", new org.apache.ftpserver.command.HELP());
        commandMap.put("LANG", new FtpCommands.LANG()); // RFC 2640 I18N
        commandMap.put("LIST", new FtpCommands.LIST());
        //commandMap.put("MD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5
        //commandMap.put("MMD5", new org.apache.ftpserver.command.MD5()); // draft-twine-ftpmd5-00.txt MD5
        commandMap.put("MDTM", new FtpCommands.MDTM()); // RFC 3659 Extensions to FTP
        commandMap.put("MKD",  new FtpCommands.MKD());
        commandMap.put("MLSD", new FtpCommands.LIST()); // RFC 3659 Extensions to FTP
        commandMap.put("MLST", new FtpCommands.MLST()); // RFC 3659 Extensions to FTP
        commandMap.put("MODE", new org.apache.ftpserver.command.MODE());
        commandMap.put("NLST", new FtpCommands.NLST());
        commandMap.put("NOOP", new org.apache.ftpserver.command.NOOP());
        commandMap.put("OPTS", new org.apache.ftpserver.command.OPTS()); // RFC 2389 Feature negotiation
        commandMap.put("PASS", new org.apache.ftpserver.command.PASS());
        commandMap.put("PASV", new org.apache.ftpserver.command.PASV());
        commandMap.put("PBSZ", new org.apache.ftpserver.command.PBSZ()); // RFC 2228 Security Extensions
        commandMap.put("PORT", new org.apache.ftpserver.command.PORT());
        commandMap.put("PROT", new org.apache.ftpserver.command.PROT()); // RFC 2228 Security Extensions
        commandMap.put("PWD",  new FtpCommands.PWD());
        commandMap.put("QUIT", new org.apache.ftpserver.command.QUIT());
        commandMap.put("REIN", new org.apache.ftpserver.command.REIN());
        commandMap.put("REST", new org.apache.ftpserver.command.REST()); // RFC 3659 Extensions to FTP
        commandMap.put("RETR", new org.apache.ftpserver.command.RETR());
        commandMap.put("RMD",  new FtpCommands.RMD());
        commandMap.put("RNFR", new FtpCommands.RNFR());
        commandMap.put("RNTO", new FtpCommands.RNTO());
        commandMap.put("SITE", new org.apache.ftpserver.command.SITE());
        commandMap.put("SIZE", new FtpCommands.SIZE()); // RFC 3659 Extensions to FTP
        commandMap.put("STAT", new org.apache.ftpserver.command.STAT());
        commandMap.put("STOR", new org.apache.ftpserver.command.STOR());
        commandMap.put("STOU", new org.apache.ftpserver.command.STOU());
        commandMap.put("STRU", new org.apache.ftpserver.command.STRU());
        commandMap.put("SYST", new org.apache.ftpserver.command.SYST());
        commandMap.put("TYPE", new org.apache.ftpserver.command.TYPE());
        commandMap.put("USER", new org.apache.ftpserver.command.USER());

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
        if(cmdName == null || cmdName.equals("")) {
            return null;
        }

        return commandMap.get(cmdName);
    }
}
