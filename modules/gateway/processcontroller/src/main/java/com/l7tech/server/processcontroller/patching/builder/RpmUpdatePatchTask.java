package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.FileUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;

/**
 * Patch task for updating RPMs.
 *
 * @author jbufu
 */
public class RpmUpdatePatchTask implements PatchTask {

    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        File tempDir = null;
        Exception toThrow = null;
        try {
            String rpmFileList = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);

            // extract RPMs from patch
            String[] rpms = rpmFileList.split("\n");
            InputStream rpmIn = null;
            OutputStream rpmOut = null;
            tempDir = FileUtils.createTempDirectory(this.getClass().getName(), "", null, false);
            StringBuilder extractedRpmList = new StringBuilder();
            for (String rpm : rpms) {
                try {
                    rpmIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + rpm);
                    File rpmOutFile = new File(tempDir, rpm);
                    rpmOut = new FileOutputStream(rpmOutFile);
                    IOUtils.copyStream(rpmIn, rpmOut);
                    extractedRpmList.append(rpmOutFile.getAbsolutePath()).append(" ");
                } finally {
                    ResourceUtils.closeQuietly(rpmIn);
                    ResourceUtils.closeQuietly(rpmOut);
                }
            }

            String commandLine = "/bin/rpm -U --force " + extractedRpmList;
            ProcResult result = ProcUtils.exec(commandLine);
            if (result.getExitStatus() != 0) {
                byte[] output = result.getOutput();
                throw new PatchException("Error executing patch task: '" + commandLine + (output == null ? "'" : "', error message: " + new String(output) + "\n"));
            }
        } finally {
            if (tempDir != null && tempDir.exists() && !FileUtils.deleteDir(tempDir))
                toThrow = new RuntimeException("Failed to delete temporary patch dir: " + tempDir.getAbsolutePath());
        }

        if (toThrow != null)
            throw toThrow;
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            PatchException.class.getName(),
            "com.l7tech.util.FileUtils",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.common.io.ProcUtils$1",
            "com.l7tech.common.io.ProcUtils$ByteArrayHolder",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.util.CausedIOException"
        };
    }
}
