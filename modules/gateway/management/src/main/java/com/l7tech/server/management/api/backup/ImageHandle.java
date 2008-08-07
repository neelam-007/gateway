/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.backup;

import java.util.Date;

/**
 * A handle to a restorable backup image that has been uploaded via {@link NodeBackupApi#uploadRestoreImage}.  The PC
 * will attempt to retain the image file through the {@link #handleExpiry} time of any outstanding handles,
 * but may delete any image file without warning as long as no restoration is in progress.
 *
 * @author alex
 */
public class ImageHandle {
    private String name;
    private String sourceNode;
    private String sourceHost;

    private long size;
    private Date imageCreated;
    private Date handleExpiry;

    private BackupCatalog contents;
}
