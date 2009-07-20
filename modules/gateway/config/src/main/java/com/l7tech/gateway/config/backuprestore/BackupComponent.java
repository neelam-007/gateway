/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 19, 2009
 * Time: 1:53:18 PM
 */
package com.l7tech.gateway.config.backuprestore;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * Simple interface for backup / restore components
 */
interface SsgComponent{
    public ImportExportUtilities.ComponentType getComponentType();
}

interface BackupComponent<E extends Exception> extends SsgComponent{
    public void doBackup() throws E;
}

interface RestoreComponent<E extends Exception> extends SsgComponent{
    public void doRestore() throws E;
}
