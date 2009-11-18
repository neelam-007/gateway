package com.l7tech.server.processcontroller.patching.builder;

/**
 * Patch task that outputs a "resboot is needed" message on successful installation.
 *
 * @author jbufu
 */
public class RebootNeededPatchTask implements PatchTask {

    // - PUBLIC

    public RebootNeededPatchTask() { }

    @Override
    public void runPatch(String resourceDirEntry) throws Exception {
        String rebootMsg = PatchMain.readResource(this.getClass(), resourceDirEntry + PatchTask.TASK_RESOURCE_FILE);
        String patchId = null; // todo: add to interface?
        System.out.println("Patch ID: " + patchId + " is installed. " + (rebootMsg == null || rebootMsg.isEmpty() ? DEFAULT_REBOOT_MSG : rebootMsg));
    }

    @Override
    public String[] getClassDependencies() {
        return new String[] {
            "com.l7tech.util.IOUtils"
        };
    }

    // - PRIVATE

    private static final String DEFAULT_REBOOT_MSG = "Reboot is required.";
}
