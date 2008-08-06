/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.backup;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;

import javax.activation.DataHandler;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Set;

/** @author alex */
public interface NodeBackupApi {
    /**
     * Initiates a backup of the named node using the provided configuration, and immediately returns a handle that can
     * subsequently be used to {@link #refreshBackupStatus query} the status of the ongoing backup process, then
     * {@link #getBackupImage(BackupStatus) download} the backup when complete.
     *
     * @param nodeName the name of the node to be backed up
     * @param catalog the configuration indicating what component(s) of the node's data should be backed up, or null to
     *                backup everything.
     * @return a handle for checking on the status of the backup and eventually getting the backup image
     * @throws IOException if the backup cannot be started, likely because of a database connection failure.
     * @throws SQLException if the backup cannot be started, likely because of corrupt or inconsistent database state.
     */
    BackupStatus startNodeBackup(String nodeName, BackupCatalog catalog) throws IOException, SQLException;

    /**
     * Gets the current status for the backup process described by the provided handle.
     *
     * @param status the handle for a backup in progress (maybe already completed)
     * @return a new handle for the backup in progress reflecting its current status
     */
    BackupStatus refreshBackupStatus(BackupStatus status);

    /**
     * Gets the MIME blob of the backup image for the provided handle.
     *
     * @param status the handle for the backup that has concluded.
     * @return
     * @throws IOException
     */
    DataHandler getBackupImage(BackupStatus status) throws BackupNotCompletedException, IOException;

    /**
     * Uploads a backup image to the PC.  If the upload concludes successfully, the resulting ImageHandle can be used to
     * restore the image to one or nodes with {@link #startNodeRestore}.  Note that the PC may autonomously delete
     * restore images without notice; call {@link #findRestorableImages} to check whether an image that was uploaded
     * some time ago is still present.
     *
     * @param imageData the MIME blob of the backup image file
     * @return an ImageHandle describing the blob, that can then be used to begin a restore.
     * @throws SaveException if the image is unacceptable to the PC (e.g. it's of an unsupported or unexpected type)
     * @throws IOException if the image cannot be saved due to an underlying IOException (e.g. disk full)
     */
    ImageHandle uploadRestoreImage(DataHandler imageData) throws SaveException, IOException;

    /**
     * Finds any restorable images that have previously been uploaded with {@link #uploadRestoreImage} and returns an
     * ImageHandle for each.
     * @return the ImageHandles for the restore images known to the PC.
     * @throws FindException if the PC is unable to determine whether any restore images are available
     */
    Set<ImageHandle> findRestorableImages() throws FindException;

    /**
     * Deletes the restore image pointed to by the provided handle.
     * @param handle the handle to a restore image previously
     * @throws DeleteException
     * @throws IOException
     */
    void deleteRestoreImage(ImageHandle handle) throws DeleteException, IOException;

    /**
     * Initiates the process of restoring the image with the provided handle onto the named node.
     *
     * @param sourceImage the handle for a backup image that was previously returned by the {@link #uploadRestoreImage}
     *                    process.
     * @param targetNodeName the name of the node that should be restored
     * @param config an indication of what component(s) of the image should be restored, or null to restore everything
     * @param restorekey a base64'd SecretKey capable of decrypting the backup image
     * @param nodeLoginPass the login and password for an account on the target ServiceNode with permission to 
     *                      create/update/delete the entities in the backup set
     * @return a handle describing the status of the restore process.
     * @throws IOException if the restore cannot begin because the image is corrupt or unreadable (e.g. it may have been deleted)
     * @throws SQLException if the restore cannot begin because the PC is unable to connect to the database
     */
    RestoreHandle startNodeRestore(ImageHandle sourceImage, String targetNodeName, BackupCatalog config, String restorekey, PasswordAuthentication nodeLoginPass) throws SQLException, IOException;

    /**
     * Gets the current status of a restore process that was previously {@link #startNodeRestore}.
     * @param handle the handle for the restore process.
     * @return a new restore handle with the current status  
     */
    RestoreHandle refreshRestoreStatus(RestoreHandle handle);

    public class BackupNotCompletedException extends Exception {
        public BackupNotCompletedException(String nodeName) {
            super(MessageFormat.format("The backup of {0} has not completed", nodeName));
        }
    }
}
