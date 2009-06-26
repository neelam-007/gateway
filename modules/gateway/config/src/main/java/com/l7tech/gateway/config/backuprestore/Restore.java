/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 3:07:37 PM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.PrintStream;

/**
 * Public api for Restore functionality
 */
public interface Restore {

    /**
     * Any restore method can result in success, failure or not aplicable, if the component does not apply for the
     * given back up image
     */
    public enum Result{SUCCESS, FAILURE, NOT_APPLICABLE}

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
     * @param verbose boolean, if true print messages to the supplied printStream
     * @param printStream PrintStream if not null and verbose is true, this is where messages will be written to
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * DatabaseConfig represents a remote database
     */
    public Result restoreComponentMainDb(final boolean isRequired,
                                         final boolean isMigrate,
                                         final boolean newDatabaseIsRequired,
                                         final String pathToMappingFile,
                                         final boolean updateNodeProperties,
                                         final boolean verbose,
                                         final PrintStream printStream) throws RestoreImpl.RestoreException;


    /**
     * @param isRequired if true, this component must be found in the backup image
     * @param verbose boolean, if true print messages to the supplied printStream
     * @param printStream PrintStream if not null and verbose is true, this is where messages will be written to
     * @return
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * DatabaseConfig represents a remote database
     */
    public Result restoreComponentAudits(final boolean isRequired,
                                         final boolean verbose,
                                         final PrintStream printStream) throws RestoreImpl.RestoreException;

    /**
     * Restore OS configuration files. Regardless of whether isRequired is true or not, these files will only
     * ever be copied if the Appliance home directory is found on the target
     * @param isRequired if true, this component must be found in the backup image
     * @param verbose boolean, if true print messages to the supplied printStream
     * @param printStream PrintStream if not null and verbose is true, this is where messages will be written to
     * @return Result one of either success, failure or not applicable. Not applicable can happen if the
     * target does not have the appliance installed
     */
    public Result restoreComponentOS(final boolean isRequired,
                                     final boolean verbose,
                                     final PrintStream printStream) throws RestoreImpl.RestoreException;
}

