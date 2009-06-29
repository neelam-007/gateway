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

    public static Restore getRestoreInstance(final File secureSpanHome,
                                             final BackupImage backupImage,
                                             final DatabaseConfig dbConfig,
                                             final String clusterPassphrase,
                                             final boolean verbose,
                                             final PrintStream printStream) throws Restore.RestoreException {

        return new RestoreImpl(secureSpanHome,
                backupImage,
                dbConfig,/*I might be null and that's ok*/
                clusterPassphrase,
                verbose,
                printStream);
    }

    /**
     * Get an instance of a Backup
     * @param secureSpanHome base instalation of Secure Span products e.g. /opt/SecureSpan
     * @param ftpConfig can be null, where and how to download the image, if required
     * @param verbose print progress information
     * @param printStream where to send verbose output to
     * @return Backup instance
     * @throws Backup.BackupException
     */
    public static Backup getBackupInstance(final File secureSpanHome,
                                           final FtpClientConfig ftpConfig,
                                           final String pathToImageZipFile,
                                           final boolean verbose,
                                           final PrintStream printStream)
            throws Backup.BackupException {
        return new BackupImpl(secureSpanHome, ftpConfig, pathToImageZipFile, verbose, printStream);
    }
    
}