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
     * Any restore method can result in success, failure or not aplicable, if the component does not apply for the
     * given back up image
     * There is no FAILURE case, as when a failure happens, a RestoreException is thrown
     */
    public enum Result{SUCCESS, NOT_APPLICABLE}

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
     * @param isRequired if true, this component must be found in the backup image
     * @param pathToMappingFile String path to any mapping file, if a migrate is happening. Can be null
     * @param isMigrate boolean whether this restore is a migrate
     * @param newDatabaseIsRequired when isMigrate true, if newDatabaseIsRequired is true, then a new database
     * must be created. This is an instruction to create a new database
     * @param propertiesConfiguration if not null, this will be written to node.properties
     * @param ompDatFile if not null, it will overwrite the target's omp.dat
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * DatabaseConfig represents a remote database
     */
    public Result restoreComponentMainDb(final boolean isRequired,
                                         final boolean isMigrate,
                                         final boolean newDatabaseIsRequired,
                                         final String pathToMappingFile,
                                         final PropertiesConfiguration propertiesConfiguration,
                                         final File ompDatFile) throws RestoreException;


    /**
     * @param isRequired if true, this component must be found in the backup image
     * @param isMigate if true, and isRequired is true, an exception will not be thrown if no audit data is found
     * in the backup image 
     * @return Result is either success or not applicable. Not applicable can happen if the
     * DatabaseConfig represents a remote database
     */
    public Result restoreComponentAudits(final boolean isRequired,
                                         final boolean isMigate) throws RestoreException;

    /**
     * Restore OS configuration files. Regardless of whether isRequired is true or not, these files will only
     * ever be copied if the Appliance home directory is found on the target
     * @param isRequired if true, this component must be found in the backup image
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * target does not have the appliance installed
     */
    public Result restoreComponentOS(final boolean isRequired) throws RestoreException;

    /**
     * Restore any found custom assertions and their associated properties files, if any, from the ca folder of the i
     * mage. The custom assertion component will only ever exist in a post 5.0 image
     *
     * This will restore any .properties files found in the ca folder from the image into the ssg configuration folder
     * and will copy any custom assertion jars into the custom assertion jar folder
     *
     * @param isRequired if true, this component must be found in the backup image
     * @return Result is either success or not applicable, which happens when no custom assertion data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentCA(final boolean isRequired) throws RestoreException;

    /**
     * Restore the ssg configuration files to the ssg configuration folder. Configuratoin data is found in the
     * config folder in a post 5.0 image and in the root folder of a 5.0 image
     *
     * 5.0 does not back up any files which represent the nodes identity (node.properties or omp.dat)
     *
     * @param isRequired if true, this component must be found in the backup image
     * @param isMigrate if true, the file /config/backup/cfg/exclude_files will be consulted and any files
     * listed will be ignored during the restore
     * @param ignoreNodeIdentity if true, then node.properties and omp.dat will not be restored. This happens when the
     * restore process creates node.properties due to all database information having been supplied during the restore
     * process, when false, node.properties will be restored, if it's found in the backup image 
     * @return Result is either success or not applicable, which happens when no configuration data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentConfig(final boolean isRequired,
                                         final boolean isMigrate,
                                         final boolean ignoreNodeIdentity) throws RestoreException;

    /**
     * Restore any found modular assertions from the ma folder of the image. The modular assertion component
     * will only ever exist in a post 5.0 image
     *
     * This will restore found modular assertion aar jar files into the modular assertion folder
     *
     * Only assertions which do not exist on the target will be copied. The backup / restore of modular assertions
     * is designed to catch any custom modular assertions which may be installed. It is not intended to be a backup
     * /restore for modular assertions which come with the product
     *
     * @param isRequired if true, this component must be found in the backup image
     * @return Result is either success or not applicable, which happens when no modular assertion data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentMA(final boolean isRequired) throws RestoreException;

    /**
     * Restore the ESM. The ESM must be installed for the ESM component to be called.
     * The ESM cannot be restored if it is running
     *
     * Restoring a ESM consists of : restoring omp.dat, restoring emconfig.properties and restoring the /var/db folder
     * 
     * @param isRequired if true, this component must be found in the backup image
     * @return Result is either success or not applicable, which happens when no esm data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentESM(final boolean isRequired) throws RestoreException;
    
    public static class RestoreException extends Exception{
        public RestoreException(String message) {
            super(message);
        }
    }
}

