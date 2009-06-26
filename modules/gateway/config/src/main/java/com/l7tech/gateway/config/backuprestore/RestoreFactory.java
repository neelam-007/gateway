/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 24, 2009
 * Time: 10:39:03 AM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.File;
import java.io.PrintStream;

/**
 * Factory for getting an instance of a Restore
 */
public class RestoreFactory {

    public static Restore getRestoreInstance(final String applianceHome,
                       final BackupImage backupImage,
                       final DatabaseConfig dbConfig,
                       final String clusterPassphrase,
                       final boolean verbose,
                       final File ssgHome,
                       final PrintStream printStream) throws RestoreImpl.RestoreException {

        return new RestoreImpl(applianceHome,
                backupImage,
                dbConfig,/*I might be null and that's ok*/
                clusterPassphrase,
                verbose,
                ssgHome,
                printStream);
    }
}
