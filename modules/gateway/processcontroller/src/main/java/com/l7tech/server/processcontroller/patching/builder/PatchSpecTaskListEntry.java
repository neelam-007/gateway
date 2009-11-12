package com.l7tech.server.processcontroller.patching.builder;

import static com.l7tech.server.processcontroller.patching.builder.PatchTask.TASK_LIST_ENTRY_NAME;

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Patch entry specification for the special file that lists the patch tasks included in the patch.
 *
 * @author jbufu
 */
public class PatchSpecTaskListEntry implements PatchSpecEntry {

    // - PUBLIC

    @Override
    public String getEntryName() {
        return TASK_LIST_ENTRY_NAME;
    }

    @Override
    public void toJar(JarOutputStream jos) throws IOException {
        jos.putNextEntry(new ZipEntry(TASK_LIST_ENTRY_NAME));
        for (Class<? extends PatchTask> taskClass : taskList) {
            jos.write(taskClass.getName().getBytes());
            jos.write("\n".getBytes());
        }
        jos.closeEntry();
    }

    public void addTask(Class<? extends PatchTask> taskClass) {
        taskList.add(taskClass);
    }

    // - PRIVATE

    private List<Class<? extends PatchTask>> taskList = new ArrayList<Class<? extends PatchTask>>();
}
