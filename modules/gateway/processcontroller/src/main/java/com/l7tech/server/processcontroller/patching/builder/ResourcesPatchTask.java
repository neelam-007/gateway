package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Copies
 * @author jbufu
 */
public class ResourcesPatchTask implements PatchTask {
    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String fileNames = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);
        String[] files = fileNames.split("\n");

        File tempDir = new File( ConfigFactory.getProperty( PatchMain.RESOURCE_TEMP_PROPERTY ) );
        InputStream fileIn = null;
        OutputStream fileOut = null;
        for (String file : files) {
            try {
                fileIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + file);
                File outFile = new File(tempDir, file);
                outFile.deleteOnExit();
                fileOut = new FileOutputStream(outFile);
                IOUtils.copyStream(fileIn, fileOut);
            } finally {
                ResourceUtils.closeQuietly(fileIn);
                ResourceUtils.closeQuietly(fileOut);
            }
        }
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.util.FileUtils",
            "com.l7tech.util.FileUtils$Saver",
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.server.processcontroller.patching.PatchException",
            "com.l7tech.util.CausedIOException",
        };
    }

}
