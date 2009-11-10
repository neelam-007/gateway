package com.l7tech.server.processcontroller.patching;

import javax.jws.WebService;
import java.util.Collection;
import java.io.IOException;

/**
 * PatchService API.
 */
@WebService
public interface PatchServiceApi {

    public PatchStatus uploadPatch(byte[] patchData) throws PatchException;

    public PatchStatus installPatch(String patchId, Collection<String> nodes) throws IOException, PatchException;

    public PatchStatus deletePackageArchive(String patchId) throws PatchException;

    public Collection<PatchStatus> listPatches();

    public PatchStatus getStatus(String patchId) throws PatchException;

    enum Action { UPLOAD, INSTALL, ROLLBACK, PACKAGE_DELETE, LIST, STATUS }

    /**
     * System property for specifying the node IDs that a patch needs to be applied to.
     * The value is a coma-separated list of node IDs. Used by installPatch() API entry.
     */
    public static final String TARGET_NODE_IDS = "target.node.ids";
}
