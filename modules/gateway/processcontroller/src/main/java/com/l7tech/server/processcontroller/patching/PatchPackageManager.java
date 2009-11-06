package com.l7tech.server.processcontroller.patching;

import java.util.Collection;

/**
 * API for managing patch package archives on a gateway node.
 */
public interface PatchPackageManager {

    public static final String PATCH_EXTENSION = ".L7P";

    public PatchStatus getPackageStatus(String patchId) throws PatchException;

    public PatchStatus setPackageStatus(PatchStatus status) throws PatchException;

    public PatchStatus updatePackageStatus(String patchId, PatchStatus.Field field, String value) throws PatchException;

    public PatchStatus savePackage(PatchPackage patch) throws PatchException;

    public PatchPackage getPackage(String patchId) throws PatchException;

    public PatchStatus deletePackage(String patchId) throws PatchException;

    public Collection<PatchStatus> listPatches();

    public Collection<String> getRollbacksFor(String patchId);
    
}
