package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.common.io.ProcUtils;
import com.l7tech.common.io.ProcResult;
import com.l7tech.server.processcontroller.patching.PatchException;

/**
 * Patch task for running a OS command.
 *
 * @author jbufu
 */
public class OSCommandPatchTask implements PatchTask {

    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String commandLine = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);
        ProcResult result = ProcUtils.exec(commandLine);
        if(result.getExitStatus() != 0) {
            byte[] output = result.getOutput();
            throw new PatchException("Error executing patch task: '" + commandLine + (output == null ? "'" : "', error message: " + new String(output) + "\n"));
        }
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            PatchException.class.getName(),
            "com.l7tech.common.io.ProcUtils",
            "com.l7tech.common.io.ProcUtils$1",
            "com.l7tech.common.io.ProcUtils$ByteArrayHolder",
            "com.l7tech.common.io.ProcResult",
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.util.CausedIOException"
        };
    }
}
