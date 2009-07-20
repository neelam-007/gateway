package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.IOException;
import java.io.File;

/**
 * Public api for backup functionality
 */
public interface Backup {

    public static class BackupException extends Exception{
        public BackupException(String s) {
            super(s);
        }
    }
   
    /**
     * Create a file called 'version' in the folder tmpOutputDirectory. This file lists the current version of the
     * SSG software installed, when a backup is ran
     */
    public void backUpVersion() throws BackupException;

    /**
     * <p>
     * Backup the main db component, excluding any audit tables. This will generate a file called main_backup.sql
     * in the tmpOutputDirectory, under the maindb folder. This folder will also contain any other required database
     * files (my.cnf)
     * </p>
     *
     * <p>
     * If mappingFile is not null then a mapping file will be created providing a template to specify the mapping of
     * cluster property values and ip address values from the system being backed up to the system being restored.
     * This mapping file can be used when restoring
     * </p>
     *
     * <p>
     * If the database is not local and this method is called, then an IllegalState exception will be thrown.
     * </p> 
     *
     * @param mappingFile name of the mapping file. Can include path information. Can be null when it is not required
     * @param config DatabaseConfig object used for connecting to the database. It must represent a database which
     * is local, otherwise an exception will be thrown
     * @throws BackupException if any exception occurs writing the database back up files
     */
    public void backUpComponentMainDb(final String mappingFile,
                                      final DatabaseConfig config) throws BackupException;

    /**
     * Back up database audits. Audits are written directly from memory into a gzip file to
     * conserve space. The file created is called "audits.gz" and is contained inside the "audits" folder
     *
     * Note: The sql created in the file "audits.gz" will NOT contain create and drop statements. The drop and
     * create statments are created by addDatabaseToBackupFolder. When restoring or migrating the audits tables must
     * always be dropped and recreated. As a result the "audits.gz" file cannot be loaded into a database until
     * the audit tables have been recreated.
     *
     * Audits can only be backed up if the database is local. If it's not and this method is called an
     * IllegalStateException will be thrown.
     * @param config DatabaseConfig object used for connecting to the database
     * @throws BackupException if any exception occurs writing the database back up files
     */
    public void backUpComponentAudits(final DatabaseConfig config) throws BackupException;

    /**
     * Backs up the SSG config files node.properties, ssglog.properties, system.properties and omp.dat
     * from the node/default/etc/conf folder into the "config" directory under tmpOutputDirectory
     * @throws BackupException if any exception occurs writing the SSG config files to the backup folder
     */
    public void backUpComponentConfig() throws BackupException;

    /**
     * Backs up OS files listed in the file backup_manifest.conf, in the config/backup/cfg folder, into the
     * "os" directory under tmpOutputDirectory. The file backup_manifest only exists when the appliance is installed.
     * No action is taken if this method is called and the appliance is not installed
     *
     * @throws BackupException if any exception occurs writing the OS files to the backup folder
     */
    public void backUpComponentOS() throws BackupException;

    /**
     * Backs up custom assertions and their associated property files to the ca folder
     *
     * Custom assertion jars are stored in runtime/modules/lib and their property files are kept in node/default/etc/conf
     *
     * @throws BackupException if any exception occurs writing the custom assertions and property files to the backup folder
     */
    public void backUpComponentCA() throws BackupException;    

    /**
     * This method backs up modular assertions to the ma folder
     *
     * Custom assertion jars are stored in runtime/modules/assertions
     *
     * @throws BackupException if any exception occurs writing the modular assertions to the backup folder
     */
    public void backUpComponentMA() throws BackupException;

    /**
     * Back up the ESM, if it is installed and not running. Backs up the ESM to the esm folder
     *
     * Backing up the ESM consists of : backing up omp.dat, emconfig.properties and the /var/db folder
     *
     * Do not call if the ESM is running
     * @throws BackupException
     */
    public void backUpComponentESM() throws BackupException;

    /**
     * Create the back up image file. If an instance of Backup is configured with an ftp location, then the image
     * will be ftp'ed to the ftp host, after the image has been created
     * @throws BackupException
     */
    public void createBackupImage() throws BackupException;

    /**
     * Delete the temporary folder that was used by the back up methods to collect the back up data
     * Caller should call this when finished with an instance of Backup
     * @throws IOException
     */
    public void deleteTemporaryDirectory() throws IOException;

    /**
     * Get the folder which all the back up methods copy data to
     * @return
     */
    public File getBackupFolder();
}
