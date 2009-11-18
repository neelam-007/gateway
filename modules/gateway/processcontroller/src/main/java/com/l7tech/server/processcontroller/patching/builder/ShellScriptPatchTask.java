package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * @author jbufu
 */
public class ShellScriptPatchTask implements PatchTask {
    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String scriptName = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);

        File tempFile = File.createTempFile("patch_sh_script", "");
        tempFile.deleteOnExit();
        InputStream scriptIn = null;
        OutputStream tempOut = null;
        try {
            scriptIn = PatchMain.getResourceStream(this.getClass(), resourceDirEntry + scriptName);
            tempOut = new FileOutputStream(tempFile);
            IOUtils.copyStream(scriptIn, tempOut);
        } finally {
            ResourceUtils.closeQuietly(scriptIn);
            ResourceUtils.closeQuietly(tempOut);
        }


        ProcResult result = ProcUtils.exec("/bin/bash " + tempFile.getAbsolutePath());
        if(result.getExitStatus() != 0) {
            byte[] output = result.getOutput();
            throw new PatchException("Error executing patch task: '" + scriptName + (output == null ? "'" : "', error message: " + new String(output) + "\n"));
        }
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.util.FileUtils",
            "com.l7tech.util.FileUtils$Saver",
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.common.io.ProcUtils$1",
            "com.l7tech.common.io.ProcUtils$ByteArrayHolder",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.server.processcontroller.patching.PatchException",
            "com.l7tech.util.CausedIOException",
        };
    }

}
