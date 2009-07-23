/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 3:07:37 PM
 */
package com.l7tech.gateway.config.backuprestore;

import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;

/**
 * Public api for Restore functionality
 */
public interface Restore {

    /**
     * <p>
     * Restore the main database component. If this is post 5.0 release, this may also include my.cnf, but only if it's
     * on an Appliance. If the configured database host is not local, then no database information will be restored,
     * however my.cnf if found can still be restored
     * </p>
     * <p>
     * This will never restore database audits
     * </p>
     * <p>
     * The restore of /etc/my.cnf is a two step process and works just how the os files are restored. When this
     * method is called, if my.cnf is found in the backup, its copied into an internal folder to the SSG. When
     * the SSG's host restarts, if it's configured, my.cnf will be copied from this internal folder to its correct
     * folder. This has to be done to ensure the procedure to copy my.cnf has the correct priviledges
     * </p>
     *
     * @param pathToMappingFile     String path to any mapping file, if a migrate is happening. Can be null
     * @param isMigrate             boolean whether this restore is a migrate
     * @param newDatabaseIsRequired when isMigrate true, if newDatabaseIsRequired is true, then a new database
     *                              must be created. This is an instruction to create a new database
     * @return ComponentResult whos getResult is either success or not applicable. Not applicable can happen if the
     *         DatabaseConfig represents a remote database or no database data is contained in the backup image
     */
    public ComponentResult restoreComponentMainDb(final boolean isMigrate,
                                                  final boolean newDatabaseIsRequired,
                                                  final String pathToMappingFile) throws RestoreException;


    /**
     * Restore database audits. The audit tables must exist in the database for this to be successful. Restoring of
     * audits will not create the audit tables, it will just populate them with data from the backup image.
     *
     * @param isMigate used to advise the Restore instance that a migrate is being done. If no audits are in the image
     * then the ComponentResult will contain SUCCESS, which would normally be NOT_APPLICABLE.
     * @return ComponentResult whos getResult is either success or not applicable. Not applicable can happen if the
     *         DatabaseConfig represents a remote database or no audit data is contained in the backup image
     */
    public ComponentResult restoreComponentAudits(final boolean isMigate) throws RestoreException;

    /**
     * Restore OS configuration files. These files will only ever be copied if the Appliance is installed on the restore
     * target
     *
     * @return ComponentResult whos getResult is one of either success or not applicable. Not applicable can happen if
     *         the target does not have the appliance installed or if no os data is in the backup image
     */
    public ComponentResult restoreComponentOS() throws RestoreException;

    /**
     * Restore any found custom assertions and their associated properties files, if any, from the ca folder of the
     * image. The custom assertion component will only ever exist in a post 5.0 image
     * <p/>
     * This will restore any .properties files found in the ca folder from the image into the ssg configuration folder
     * and will copy any custom assertion jars into the custom assertion jar folder
     *
     * @return ComponentResult whos getResult is either success or not applicable, which happens when no custom
     *         assertion data is found in the backup image
     * @throws RestoreException
     */
    public ComponentResult restoreComponentCA() throws RestoreException;

    /**
     * Restore the ssg configuration files to the ssg configuration folder. Configuration data is found in the
     * config folder in a post 5.0 image and in the root folder of a 5.0 image
     * <p/>
     * 5.0 does not back up any files which represent the nodes identity (node.properties or omp.dat)
     *
     * @param isMigrate          if true, the file /config/backup/cfg/exclude_files will be consulted and any files
     *                           listed will be ignored during the restore
     * @param ignoreNodeIdentity if true, then node.properties and omp.dat will not be restored. This happens when the
     *                           restore process creates node.properties due to all database information having been
     *                           supplied during the restore
     *                           process, when false, node.properties will be restored, if it's found in the backup image
     * @return ComponentResult whos getResult is either success or not applicable, which happens when no configuration
     *         data is found in the backup image
     * @throws RestoreException
     */
    public ComponentResult restoreComponentConfig(final boolean isMigrate,
                                                  final boolean ignoreNodeIdentity) throws RestoreException;

    /**
     * Restore any found modular assertions from the ma folder of the image. The modular assertion component
     * will only ever exist in a post 5.0 image
     * <p/>
     * This will restore found modular assertion aar jar files into the modular assertion folder
     * <p/>
     * Only assertions which do not exist on the target will be copied. The backup / restore of modular assertions
     * is designed to catch any custom modular assertions which may have been installed. It is not intended to be a backup
     * /restore for modular assertions which come with the core product
     *
     * @return ComponentResult whos getResult is either success or not applicable, which happens when no modular
     *         assertion data is found in the backup image
     * @throws RestoreException
     */
    public ComponentResult restoreComponentMA() throws RestoreException;

    /**
     * Restore the ESM. The ESM must be installed for the ESM component to be called.
     * The ESM cannot be restored if it is running
     * <p/>
     * Restoring a ESM consists of : restoring omp.dat, restoring emconfig.properties and restoring the /var/db folder
     *
     * @return ComponentResult whos getResult is either success or not applicable, which happens when no esm data is found
     *         in the backup image
     * @throws RestoreException
     */
    public ComponentResult restoreComponentESM() throws RestoreException;

    /**
     * Write the supplied propertiesConfiguration to the hosts node.properties, if not null, and write ompDatFile
     * to the hosts omp.dat, if not null
     * <p/>
     * Node.properties on the restore host can get updated via restoreComponentConfig
     * This method is supplied for the case when restoreComponentConfig is not used, or is called with
     * ignoreNodeIdentity = true
     * <p/>
     * Note: Both parameters can be null however ompDatFile cannot be null, unless propertiesConfiguration is also
     * null
     *
     * @param isMigrate               simply used for log messages
     * @param propertiesConfiguration the configuration to write to node.properties. If not null, this will overwrite
     *                                the hosts node.properties. Any elements in this configuration which should be
     *                                encrypted, should be encrypted
     *                                before calling this method.
     * @param ompDatFile              if not null, this will be written to the target and will overwrite any existing
     *                                omp.dat file
     *                                Note: this file is not used to encrypt any values of the propertiesConfiguration,
     *                                but if anything in
     *                                propertiesConfiguration is encrypted, then it should have been encrypted with the
     *                                contents of ompDatFile
     * @return ComponentResult whos getResult will be Success or else an exception will be thrown
     * @throws RestoreException
     */
    public ComponentResult restoreNodeIdentity(final boolean isMigrate,
                                               final PropertiesConfiguration propertiesConfiguration,
                                               final File ompDatFile)
            throws RestoreException;

    public static class RestoreException extends Exception{
        public RestoreException(String message) {
            super(message);
        }
    }
}

