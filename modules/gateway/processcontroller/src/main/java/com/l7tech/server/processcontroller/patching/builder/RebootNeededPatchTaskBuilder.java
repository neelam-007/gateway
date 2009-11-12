package com.l7tech.server.processcontroller.patching.builder;

import static com.l7tech.server.processcontroller.patching.builder.RebootNeededPatchTask.REBOOT_MSG_ENTRY_NAME;
import com.l7tech.util.IOUtils;

import java.util.List;
import java.io.ByteArrayInputStream;

/**
 * Configures and adds a "reboot needed" patch task to the patch spec.
 *
 * @author jbufu
 */
public class RebootNeededPatchTaskBuilder extends PatchTaskBuilder {

    public RebootNeededPatchTaskBuilder(PatchSpec spec, List<String> args) {

        Class<? extends PatchTask> taskClass = RebootNeededPatchTask.class;

        addTask(spec, taskClass);

        // add all other required resources to spec
        if (args != null && ! args.isEmpty()) {
            spec.entry(new PatchSpecStreamEntry(REBOOT_MSG_ENTRY_NAME, new ByteArrayInputStream(args.get(0).getBytes())));
        }
        spec.entry(new PatchSpecClassEntry(IOUtils.class));
    }
}
