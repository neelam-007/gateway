/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 29, 2009
 * Time: 12:54:57 PM
 */
package com.l7tech.gateway.config.backuprestore;

final class CommonCommandLineOptions {
    static final CommandLineOption FTP_HOST =
            new CommandLineOption("-ftp_host",
                                    "host to ftp backup image to: "+
                                    "host.domain.com[:port]",
                                     false, false);
    static final CommandLineOption FTP_USER = new CommandLineOption("-ftp_user",
                                                                               "ftp username",
                                                                               false, false);
    static final CommandLineOption FTP_PASS = new CommandLineOption("-ftp_pass",
                                                                                   "ftp password",
                                                                                   false, false);
    static final CommandLineOption[] ALL_FTP_OPTIONS = {FTP_HOST, FTP_USER, FTP_PASS};
    static final CommandLineOption VERBOSE = new CommandLineOption("-v",
            "verbose output to the console",
                                                                               false, true);
    static final CommandLineOption HALT_ON_FIRST_FAILURE = new CommandLineOption("-halt",
            "halt on first failure. Default behaviour is to try each component independently",
                                                                               false, true);

    public static final CommandLineOption OS_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.OS.getComponentName(),
            "os configuration files on a SecureSpan Appliance only", false, true);
    public static final CommandLineOption CONFIG_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
            "SecureSpan Gateway configuration files", false, true);
    public static final CommandLineOption MAINDB_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.MAINDB.getComponentName(),
            "the database, not including audit data", false, true);
    public static final CommandLineOption AUDITS_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.AUDITS.getComponentName(),
            "database audits", false, true);
    public static final CommandLineOption CA_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.CA.getComponentName(),
            "custom assertion jars and property files", false, true);
    public static final CommandLineOption MA_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.MA.getComponentName(),
            "modular assertion jars", false, true);

    /**
     * The EM_OPTION should never go into ALL_COMPONENTS. This variable is used to determine if a selective
     * backup or restore is being done. The -em never causes a selective backup / restore
     */
    public static final CommandLineOption [] ALL_COMPONENTS = new CommandLineOption[]{OS_OPTION, CONFIG_OPTION,
            MAINDB_OPTION, AUDITS_OPTION, CA_OPTION, MA_OPTION};

    public static final CommandLineOption ESM_OPTION = new CommandLineOption("-"+ ImportExportUtilities.ComponentType.ESM.getComponentName(),
            "the Enterprise Service Manager", false, true);

}
