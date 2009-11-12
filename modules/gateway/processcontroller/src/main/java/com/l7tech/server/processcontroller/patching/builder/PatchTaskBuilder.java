package com.l7tech.server.processcontroller.patching.builder;

/**
 * Base / utility class for constructing patch tasks.
 * Task builders must add all dependencies of their corresponding tasks to the patch JAR.
 * Separate from the patch tasks to avoid including unnecessary dependencies into the patch.
 *
 * @author jbufu
 */
public class PatchTaskBuilder {

    /** Adds a PatchTask to the list of tasks to be executed by the patch described by the provided spec. */
    protected static void addTask(PatchSpec spec, Class<? extends PatchTask> taskClass) {
        PatchSpecEntry tasks = spec.getEntries().get(PatchTask.TASK_LIST_ENTRY_NAME);

        if (tasks == null) {
            tasks = new PatchSpecTaskListEntry();
            spec.entry(tasks);
        }

        if(! (tasks instanceof PatchSpecTaskListEntry))
            throw new IllegalArgumentException(PatchTask.TASK_LIST_ENTRY_NAME + " entry already defined, cannot add patch tasks.");

        ((PatchSpecTaskListEntry)tasks).addTask(taskClass);

        spec.entry(new PatchSpecClassEntry(PatchTask.class));
        spec.entry(new PatchSpecClassEntry(taskClass));
    }
}
