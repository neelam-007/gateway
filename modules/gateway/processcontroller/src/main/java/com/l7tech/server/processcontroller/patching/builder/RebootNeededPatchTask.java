package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.util.IOUtils;

import java.io.InputStream;
import java.io.File;

/**
 * @author jbufu
 */
public class RebootNeededPatchTask implements PatchTask {

    // - PUBLIC

    public RebootNeededPatchTask() { }

    @Override
    public void runPatch() throws Exception {
        String rebootMsg = null;
        InputStream msgIn = this.getClass().getResourceAsStream(REBOOT_MSG_ENTRY_NAME);
        if (msgIn != null)
            rebootMsg = new String(IOUtils.slurpStream(msgIn));
        String patchId = null; // todo: add to interface?
        System.out.println("Patch ID: " + patchId + " is installed. " + (rebootMsg == null ? DEFAULT_REBOOT_MSG : rebootMsg));
    }

    // - PACKAGE

    static final String REBOOT_MSG_ENTRY_NAME = RebootNeededPatchTask.class.getPackage().getName().replace(".", File.separator) + "/reboot_msg.txt";

    // - PRIVATE

    private static final String DEFAULT_REBOOT_MSG = "Reboot is required.";
}
