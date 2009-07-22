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

    /**
     * Get an instance of a Restore
     * @param secureSpanHome base instalation of Secure Span products e.g. /opt/SecureSpan
     * @param backupImage the BackupImage to restore
     * @param dbConfig if not null, will be used to restore database components if requested
     * @param clusterPassphrase the cluster passphrase of the system being restored
     * @param verbose print progress information
     * @param printStream where to send verbose output to, can be null
     * @return Restore an instance of Restore
     * @throws Restore.RestoreException
     */
    public static Restore getRestoreInstance(final File secureSpanHome,
                                             final BackupImage backupImage,
                                             final DatabaseConfig dbConfig,
                                             final String clusterPassphrase,
                                             final boolean verbose,
                                             final PrintStream printStream) throws Restore.RestoreException {

        return new RestoreImpl(secureSpanHome,
                backupImage,
                dbConfig,
                clusterPassphrase,
                verbose,
                printStream);
    }

    /**
     * Get an instance of a Backup
     * @param secureSpanHome c
     * @param ftpConfig can be null, where and how to download the image, if required
     * @param pathToImageZipFile can be to a local file, or relative to a log on directory on a ftp server. Cannnot
     * be null
     * @param isPostFiveO if true and pathToImageZipFile contains no path info, then the image will be placed into the
     * images folder in /opt/SecureSpan/Gateway/config/backup
     * @param verbose print progress information
     * @param printStream where to send verbose output to, can be null
     * @return Backup an instance of Backup
     * @throws Backup.BackupException if the instance of Backup cannot be created
     */
    public static Backup getBackupInstance(final File secureSpanHome,
                                           final FtpClientConfig ftpConfig,
                                           final String pathToImageZipFile,
                                           final boolean isPostFiveO,
                                           final boolean verbose,
                                           final PrintStream printStream)
            throws Backup.BackupException {
        return new BackupImpl(secureSpanHome, ftpConfig, pathToImageZipFile, isPostFiveO, verbose, printStream);
    }
    
}