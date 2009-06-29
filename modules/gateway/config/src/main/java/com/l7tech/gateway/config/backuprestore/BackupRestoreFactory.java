/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 24, 2009
 * Time: 10:39:03 AM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;

import java.io.File;
import java.io.PrintStream;

/**
 * Factory for getting instances of Restore or Backup interfaces
 */
public class BackupRestoreFactory {

    public static Restore getRestoreInstance(final String applianceHome,
                       final BackupImage backupImage,
                       final DatabaseConfig dbConfig,
                       final String clusterPassphrase,
                       final boolean verbose,
                       final File ssgHome,
                       final PrintStream printStream) throws Restore.RestoreException {

        return new RestoreImpl(applianceHome,
                backupImage,
                dbConfig,/*I might be null and that's ok*/
                clusterPassphrase,
                verbose,
                ssgHome,
                printStream);
    }

    /**
     * Get an instance of a Backup
     * @param ssgHome
     * @param applianceHome
     * @param ftpConfig can be null
     * @param verbose
     * @param printStream
     * @return
     * @throws Backup.BackupException
     */
    public static Backup getBackupInstance(final File ssgHome,
                                           final String applianceHome,
                                           final FtpClientConfig ftpConfig,
                                           final String pathToImageZipFile,
                                           final boolean verbose,
                                           final PrintStream printStream)
            throws Backup.BackupException {
        return new BackupImpl(ssgHome, applianceHome, ftpConfig, pathToImageZipFile, verbose, printStream);
    }
    
}