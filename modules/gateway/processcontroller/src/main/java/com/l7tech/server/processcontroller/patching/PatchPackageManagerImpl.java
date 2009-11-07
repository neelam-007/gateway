package com.l7tech.server.processcontroller.patching;

import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;

import java.io.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File-based implementation for the PatchManager API.
 *
 * The patch package repository directory is configured through the Process Controller's config service.
 *
 * @author jbufu
 */
public class PatchPackageManagerImpl implements PatchPackageManager, InitializingBean {

    // - PUBLIC

    public PatchPackageManagerImpl() { }

    public PatchPackageManagerImpl(File repositoryDir) {
        init(repositoryDir);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init(configService.getPatchesDirectory());
    }

    @Override
    public PatchStatus getPackageStatus(String patchId) throws PatchException {
        return PatchStatus.getPackageStatus(repositoryDir, patchId);
    }

    @Override
    public PatchStatus setPackageStatus(PatchStatus status) throws PatchException {
        status.save(repositoryDir);
        return status;
    }

    @Override
    public PatchStatus updatePackageStatus(String patchId, PatchStatus.Field field, String value) throws PatchException {
        PatchStatus status = getPackageStatus(patchId);
        status.setField(field, value);
        return setPackageStatus(status);
    }

    @Override
    public PatchPackage getPackage(String patchId) throws PatchException {
        File patchFile = getPackageFile(patchId);
        if (! patchFile.exists())
            throw new PatchException("The patch package for patch id: " + patchId + " was not found in the repository.");
        
        try {
            return new PatchPackageImpl(patchFile);
        } catch (IOException e) {
            throw new PatchException("Error retrieving patch package for patch identifier: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public PatchStatus savePackage(final PatchPackage patch) throws PatchException {
        String patchId = patch.getProperty(PatchPackage.Property.ID);
        PatchStatus status = getPackageStatus(patchId);

        if (! status.allowDelete()) {
            throw new PatchException("Cannot overwrite patch when status is " + status.getField(PatchStatus.Field.STATE));
        }

        File targetFile = getPackageFile(patchId);
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(patch.getFile());
            final FileInputStream fis1 = fis;
            FileUtils.saveFileSafely(targetFile.getPath(), new FileUtils.Saver() {
                @Override
                public void doSave(FileOutputStream fos) throws IOException {
                    IOUtils.copyStream(fis1, fos);
                }
            });
        } catch (IOException e) {
            throw new PatchException("Error saving patch package: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(fis);
        }

        PatchStatus newStatus = PatchStatus.newPatchStatus(patchId, patch.getProperty(PatchPackage.Property.DESCRIPTION), PatchStatus.State.UPLOADED);
        String rollbackId = patch.getProperty(PatchPackage.Property.ROLLBACK_FOR_ID);
        if (rollbackId != null) newStatus.setField(PatchStatus.Field.ROLLBACK_FOR_ID, rollbackId);
        return setPackageStatus(newStatus);
    }

    @Override
    public PatchStatus deletePackage(String patchId) throws PatchException {
        PatchStatus status = getPackageStatus(patchId);
        if (! status.allowDelete() || PatchStatus.State.NONE.name().equals(status.getField(PatchStatus.Field.STATE))) {
            throw new PatchException("Cannot delete patch package when status is " + status.getField(PatchStatus.Field.STATE));
        }

        if (!getPackageFile(patchId).delete()) {
            throw new PatchException("Error deleting patch package " + patchId);
        }

        if (! PatchStatus.State.ROLLED_BACK.name().equals(status.getField(PatchStatus.Field.STATE)))
            status = updatePackageStatus(patchId, PatchStatus.Field.STATE, PatchStatus.State.NONE.name());

        return status;
    }

    @Override
    public Collection<PatchStatus> listPatches() {
        Collection<PatchStatus> list = new ArrayList<PatchStatus>();
        File[] statusFiles = repositoryDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(PatchStatus.PATCH_STATUS_SUFFIX);
            }
        });

        for(File file : statusFiles) {
            try {
                list.add(PatchStatus.loadPatchStatus(file));
            } catch (PatchException e) {
                logger.log(Level.WARNING, "Error reading patch status file: " + file.getName() + " : " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        return list;
    }

    @Override
    public Collection<String> getRollbacksFor(String patchId) {
        Collection<String> rollbacks = new ArrayList<String>();
        for(PatchStatus status : listPatches()) {
            if(patchId.equals(status.getField(PatchStatus.Field.ROLLBACK_FOR_ID)))
                rollbacks.add(status.getField(PatchStatus.Field.ID));
        }
        return rollbacks;
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(PatchPackageManagerImpl.class.getName());

    @Resource
    private ConfigService configService;

    private File repositoryDir;

    private File getPackageFile(String patchId) {
        return new File(repositoryDir, patchId + PATCH_EXTENSION);
    }

    private void init(File patchesDirectory) {
        this.repositoryDir = patchesDirectory;
    }
}
