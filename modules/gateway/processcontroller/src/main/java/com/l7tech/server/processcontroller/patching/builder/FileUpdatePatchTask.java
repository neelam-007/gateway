package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Patch task for updating a file.
 *
 * @author jbufu
 */
public class FileUpdatePatchTask implements PatchTask {

    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String[] fileToUpdate = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE).split("\n");

        InputStream fileIn = null;
        OutputStream fileOut = null;
        try {
            fileIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + fileToUpdate[1]);
            fileOut = new FileOutputStream(fileToUpdate[0]);
            IOUtils.copyStream(fileIn, fileOut);
        } finally {
            ResourceUtils.closeQuietly(fileIn);
            ResourceUtils.closeQuietly(fileOut);
        }

    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {};
    }
}
