/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 3:07:37 PM
 */
package com.l7tech.gateway.config.backuprestore;

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
     * Restore the main database component. If this is post 5.0 release, this may also include my.cnf
     * </p>
     * <p>
     * This will never restore database audits
     * </p>
     * @param isRequired if true, this component must be found in the backup image
     * @param pathToMappingFile String path to any mapping file, if a migrate is happening. Can be null
     * @param isMigrate boolean whether this restore is a migrate
     * @param newDatabaseIsRequired when isMigrate true, if newDatabaseIsRequired is true, then a new database
     * must be created. This is an instruction to create a new database
     * @param updateNodeProperties if true, then node.properties will be updated with database information
     * supplied on the command line
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * DatabaseConfig represents a remote database
     */
    public Result restoreComponentMainDb(final boolean isRequired,
                                         final boolean isMigrate,
                                         final boolean newDatabaseIsRequired,
                                         final String pathToMappingFile,
                                         final boolean updateNodeProperties) throws RestoreException;


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
     * @param isMigrate if true, the file /config/backup/cfg/exclude_files.conf will be consulted and any files
     * listed will be ignored during the restore
     * @return Result is either success or not applicable, which happens when no configuration data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentConfig(final boolean isRequired, final boolean isMigrate) throws RestoreException;

    /**
     * Restore any found modular assertions from the ma folder of the image. The modular assertion component
     * will only ever exist in a post 5.0 image
     *
     * This will restore found modular assertion aar jar files into the modular assertion folder
     *
     * @param isRequired if true, this component must be found in the backup image
     * @return Result is either success or not applicable, which happens when no modular assertion data is found
     * in the backup image
     * @throws RestoreException
     */
    public Result restoreComponentMA(final boolean isRequired) throws RestoreException;
    
    public static class RestoreException extends Exception{
        public RestoreException(String message) {
            super(message);
        }
    }
}

