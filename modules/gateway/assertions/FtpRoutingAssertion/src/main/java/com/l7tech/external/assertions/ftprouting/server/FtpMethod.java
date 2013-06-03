package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.util.EnumTranslator;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nilic
 * @author jwilliams
 */
public class FtpMethod implements Serializable {
    /** Map for looking up instance by wspName. */
    private static final Map<String, FtpMethod> _wspNameMap = new HashMap<>();

    public static final FtpMethod FTP_GET  = new FtpMethod("RETR" , "Transfer a copy of the file", FtpMethodEnum.FTP_GET);
    public static final FtpMethod FTP_PUT  = new FtpMethod("STOR" , "Accept the data and to store the data as a file at the server site", FtpMethodEnum.FTP_PUT);
    public static final FtpMethod FTP_DELE = new FtpMethod("DELE", "Delete file", FtpMethodEnum.FTP_DELE);
    public static final FtpMethod FTP_LIST = new FtpMethod("LIST", "Returns information of a file or directory if specified, else information of the current working directory is returned", FtpMethodEnum.FTP_LIST);
    public static final FtpMethod FTP_ABOR = new FtpMethod("ABOR", "Abort an active file transfer", FtpMethodEnum.FTP_ABOR);
    public static final FtpMethod FTP_ACCT = new FtpMethod("ACCT", "Account Information", FtpMethodEnum.FTP_ACCT);
    public static final FtpMethod FTP_ADAT = new FtpMethod("ADAT", "Authentication/Security Mechanism", FtpMethodEnum.FTP_ADAT);
    public static final FtpMethod FTP_ALLO = new FtpMethod("ALLO", "Allocate sufficient disk space to receive file", FtpMethodEnum.FTP_ALLO);
    public static final FtpMethod FTP_APPE = new FtpMethod("APPE", "Append", FtpMethodEnum.FTP_APPE);
    public static final FtpMethod FTP_AUTH = new FtpMethod("AUTH", "Authentication/Security Mechanism", FtpMethodEnum.FTP_AUTH);
    public static final FtpMethod FTP_CCC  = new FtpMethod("CCC" , "Clear Command Channel", FtpMethodEnum.FTP_CCC);
    public static final FtpMethod FTP_CDUP = new FtpMethod("CDUP", "Change to Parent Directory", FtpMethodEnum.FTP_CDUP);
    public static final FtpMethod FTP_CONF = new FtpMethod("CONF", "Confidentiality Protection Command",FtpMethodEnum.FTP_CONF);
    public static final FtpMethod FTP_CWD  = new FtpMethod("CWD" , "Change Working Directory", FtpMethodEnum.FTP_CWD);
    public static final FtpMethod FTP_ENC  = new FtpMethod("ENC" , "Privacy Protected Channel", FtpMethodEnum.FTP_ENC);
    public static final FtpMethod FTP_EPRT = new FtpMethod("EPRT", "Specifies extended address & port for connection", FtpMethodEnum.FTP_EPRT);
    public static final FtpMethod FTP_EPSV = new FtpMethod("EPSV", "Enter extended passive mode", FtpMethodEnum.FTP_EPSV);
    public static final FtpMethod FTP_FEAT = new FtpMethod("FEAT", "Get the feature list implemented by the server", FtpMethodEnum.FTP_FEAT);
    public static final FtpMethod FTP_HELP = new FtpMethod("HELP", "Help", FtpMethodEnum.FTP_HELP);
    public static final FtpMethod FTP_LANG = new FtpMethod("LANG", "Language Negotiation", FtpMethodEnum.FTP_LANG);
    public static final FtpMethod FTP_MDTM = new FtpMethod("MDTM", "Return the last modified time of a specified file", FtpMethodEnum.FTP_MDTM);
    public static final FtpMethod FTP_MIC  = new FtpMethod("MIC" , "Integrity Protected Command", FtpMethodEnum.FTP_MIC);
    public static final FtpMethod FTP_MKD  = new FtpMethod("MKD" , "Make directory", FtpMethodEnum.FTP_MKD);
    public static final FtpMethod FTP_MLSD = new FtpMethod("MLSD", "Lists the contents of a directory if a directory is named", FtpMethodEnum.FTP_MLSD);
    public static final FtpMethod FTP_MLST = new FtpMethod("MLST", "Provides data about exactly the object named on the command line", FtpMethodEnum.FTP_MLST);
    public static final FtpMethod FTP_MODE = new FtpMethod("MODE", "Sets the transfer mode (Stream, Block or Compressed)", FtpMethodEnum.FTP_MODE);
    public static final FtpMethod FTP_NLST = new FtpMethod("NLST", "Returns a list of files in a specified directory", FtpMethodEnum.FTP_NLST);
    public static final FtpMethod FTP_NOOP = new FtpMethod("NOOP", "No operation", FtpMethodEnum.FTP_NOOP);
    public static final FtpMethod FTP_OPTS = new FtpMethod("OPTS", "Select options for a feature", FtpMethodEnum.FTP_OPTS);
    public static final FtpMethod FTP_PASS = new FtpMethod("PASS", "Authentication password", FtpMethodEnum.FTP_PASS);
    public static final FtpMethod FTP_PASV = new FtpMethod("PASV", "Enter passive mode", FtpMethodEnum.FTP_PASV);
    public static final FtpMethod FTP_PBSZ = new FtpMethod("PBSZ", "Protection Buffer Size", FtpMethodEnum.FTP_PBSZ);
    public static final FtpMethod FTP_PORT = new FtpMethod("PORT", "Speciifes an address and port to which the server should connect", FtpMethodEnum.FTP_PORT);
    public static final FtpMethod FTP_PROT = new FtpMethod("PROT", "Data Channel Protection Level", FtpMethodEnum.FTP_PROT);
    public static final FtpMethod FTP_PWD  = new FtpMethod("PWD" , "Print working directory.  Returns current working directory of the host", FtpMethodEnum.FTP_PWD);
    public static final FtpMethod FTP_QUIT = new FtpMethod("QUIT", "Disconnect", FtpMethodEnum.FTP_QUIT);
    public static final FtpMethod FTP_REIN = new FtpMethod("REIN", "Reinitialize the connection", FtpMethodEnum.FTP_REIN);
    public static final FtpMethod FTP_RMD  = new FtpMethod("RMD" , "Remove a directory", FtpMethodEnum.FTP_RMD);
    public static final FtpMethod FTP_RNFR = new FtpMethod("RNFR", "Rename from", FtpMethodEnum.FTP_RNFR);
    public static final FtpMethod FTP_RNTO = new FtpMethod("RNTO", "Rename to", FtpMethodEnum.FTP_RNTO);
    public static final FtpMethod FTP_SITE = new FtpMethod("SITE", "Sends site specific commands to remove server", FtpMethodEnum.FTP_SITE);
    public static final FtpMethod FTP_SIZE = new FtpMethod("SIZE", "Return the size of a file", FtpMethodEnum.FTP_SIZE);
    public static final FtpMethod FTP_STAT = new FtpMethod("STAT", "Returns the current status", FtpMethodEnum.FTP_STAT);
    public static final FtpMethod FTP_STOU = new FtpMethod("STOU", "Store file uniquely", FtpMethodEnum.FTP_STOU);
    public static final FtpMethod FTP_STRU = new FtpMethod("STRU", "Set file transfer structure", FtpMethodEnum.FTP_STRU);
    public static final FtpMethod FTP_SYST = new FtpMethod("SYST", "Return system type", FtpMethodEnum.FTP_SYST);
    public static final FtpMethod FTP_TYPE = new FtpMethod("TYPE", "Sets the transfer mode (ASCII/Binary)", FtpMethodEnum.FTP_TYPE);
    public static final FtpMethod FTP_USER = new FtpMethod("USER", "Authentication username", FtpMethodEnum.FTP_USER);
    public static final FtpMethod FTP_LOGIN = new FtpMethod("LOGIN", "Log in user", FtpMethodEnum.FTP_LOGIN);

    public enum FtpMethodEnum { FTP_GET, FTP_PUT, FTP_DELE, FTP_LIST, FTP_ABOR, FTP_ACCT, FTP_ADAT, FTP_ALLO, FTP_APPE, FTP_AUTH, FTP_CCC, FTP_CDUP,
        FTP_CONF, FTP_CWD, FTP_ENC, FTP_EPRT, FTP_EPSV, FTP_FEAT, FTP_HELP, FTP_LANG, FTP_MDTM, FTP_MIC, FTP_MKD, FTP_MLSD, FTP_MLST, FTP_MODE, FTP_NLST, FTP_NOOP, FTP_OPTS,
        FTP_PASS, FTP_PASV, FTP_PBSZ, FTP_PORT, FTP_PROT, FTP_PWD, FTP_QUIT, FTP_REIN, FTP_RMD, FTP_RNFR, FTP_RNTO, FTP_SITE, FTP_SIZE, FTP_STAT, FTP_STOU,
        FTP_STRU, FTP_SYST, FTP_TYPE, FTP_USER, FTP_LOGIN }

    private static final FtpMethod[] _ftpMethods = new FtpMethod[] { FTP_GET, FTP_PUT, FTP_DELE, FTP_LIST, FTP_ABOR, FTP_ACCT, FTP_ADAT, FTP_ALLO, FTP_APPE, FTP_AUTH, FTP_CCC, FTP_CDUP,
        FTP_CONF, FTP_CWD, FTP_ENC, FTP_EPRT, FTP_EPSV, FTP_FEAT, FTP_HELP, FTP_LANG, FTP_MDTM, FTP_MIC, FTP_MKD, FTP_MLSD, FTP_MLST, FTP_MODE, FTP_NLST, FTP_NOOP, FTP_OPTS,
        FTP_PASS, FTP_PASV, FTP_PBSZ, FTP_PORT, FTP_PROT, FTP_PWD, FTP_QUIT, FTP_REIN, FTP_RMD, FTP_RNFR, FTP_RNTO, FTP_SITE, FTP_SIZE, FTP_STAT, FTP_STOU,
        FTP_STRU, FTP_SYST, FTP_TYPE, FTP_USER, FTP_LOGIN };

    private static final String[] _wspNames = new String[] { FTP_GET.getWspName(),FTP_PUT.getWspName(), FTP_DELE.getWspName(), FTP_LIST.getWspName(),
            FTP_ABOR.getWspName(), FTP_ACCT.getWspName(), FTP_ADAT.getWspName(), FTP_ALLO.getWspName(), FTP_APPE.getWspName(), FTP_AUTH.getWspName(),
            FTP_CCC.getWspName(), FTP_CDUP.getWspName(), FTP_CONF.getWspName(), FTP_CWD.getWspName(), FTP_ENC.getWspName(), FTP_EPRT.getWspName(), FTP_EPSV.getWspName(),
            FTP_FEAT.getWspName(), FTP_HELP.getWspName(), FTP_LANG.getWspName(), FTP_MDTM.getWspName(), FTP_MIC.getWspName(), FTP_MKD.getWspName(), FTP_MLSD.getWspName(), FTP_MLST.getWspName(), FTP_MODE.getWspName(), FTP_NLST.getWspName(),
            FTP_NOOP.getWspName(), FTP_OPTS.getWspName(), FTP_PASS.getWspName(), FTP_PASV.getWspName(), FTP_PBSZ.getWspName(), FTP_PORT.getWspName(), FTP_PROT.getWspName(),
            FTP_PWD.getWspName(), FTP_QUIT.getWspName(), FTP_REIN.getWspName(), FTP_RMD.getWspName(), FTP_RNFR.getWspName(), FTP_RNTO.getWspName(),
            FTP_SITE.getWspName(), FTP_SIZE.getWspName(), FTP_STAT.getWspName(), FTP_STOU.getWspName(), FTP_STRU.getWspName(), FTP_SYST.getWspName(),
            FTP_TYPE.getWspName(), FTP_USER.getWspName(), FTP_LOGIN.getWspName()};


    /** String representation used in XML serialization.
     Must be unique. Must not change for backward compatibility. */
    private final String _wspName;

    /** For printing in logs and audits. */
    private final String _printInfo;

    private final FtpMethodEnum _ftpEnum;

    private FtpMethod(String wspName, String printInfo, FtpMethodEnum ftpEnum) {
        _wspName = wspName;
        _printInfo = printInfo;
        _ftpEnum = ftpEnum;
        if (_wspNameMap.put(wspName, this) != null) throw new IllegalArgumentException("Duplicate wspName: " + wspName);
    }

    public String getWspName() {
        return _wspName;
    }

    public String getPrintName() {
        return _printInfo;
    }

    public FtpMethodEnum getFtpMethodEnum() {
        return _ftpEnum;
    }

    public String toString() {
        return _printInfo;
    }

    public static FtpMethod[] ftpMethods() {
        return _ftpMethods.clone();
    }
    public static String[] wspNames() {
        return _wspNames.clone();
    }


    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            public Object stringToObject(String s) throws IllegalArgumentException {
                return _wspNameMap.get(s);
            }

            public String objectToString(Object o) throws ClassCastException {
                return ((FtpMethod)o).getWspName();
            }
        };
    }

    protected Object readResolve() throws ObjectStreamException {
        return _wspNameMap.get(_wspName);
    }
}
