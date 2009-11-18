package com.l7tech.server.processcontroller.patching.builder;

/**
 * Utility class for constructing patch tasks.
 *
 * Task builders must add all dependencies of their corresponding tasks to the patch JAR.
 * Separate from the patch tasks to avoid including unnecessary dependencies into the patch.
 *
 * @author jbufu
 */
public class PatchTaskBuilderUtils {

    // - PUBLIC

    /** Adds a PatchTask to the list of tasks to be executed by the patch described by the provided spec. */
    public static void addTask(PatchSpec spec, Class<? extends PatchTask> taskClass, PatchSpecEntry... resourceSpecEntries) {
        addTask(spec, taskClass, PatchSpecTaskListEntry.Position.MAIN, resourceSpecEntries);
    }

    public static void addTask(PatchSpec spec, Class<? extends PatchTask> taskClass, PatchSpecTaskListEntry.Position pos, PatchSpecEntry... resourceSpecEntries) {
        PatchSpecEntry tasks = spec.getEntries().get(PatchTask.TASK_LIST_ENTRY_NAME);

        if (tasks == null) {
            tasks = new PatchSpecTaskListEntry();
            spec.entry(tasks);
        }

        if(! (tasks instanceof PatchSpecTaskListEntry))
            throw new IllegalArgumentException(PatchTask.TASK_LIST_ENTRY_NAME + " entry already defined, cannot add patch tasks.");

        if (resourceSpecEntries == null || resourceSpecEntries.length == 0)
            throw new IllegalArgumentException("No resource file specified for patch task: " + taskClass.getName());

        String resourceDir = PatchTaskBuilderUtils.generateResourceDirectory(taskClass);
        // update resource entry names
        for (PatchSpecEntry entry : resourceSpecEntries) {
            entry.setEntryName(resourceDir + entry.getEntryName());
        }

        ((PatchSpecTaskListEntry)tasks).addTask(taskClass, resourceDir, pos);
        spec.entry(new PatchSpecClassEntry(PatchTask.class), false);
        spec.entry(new PatchSpecClassEntry(taskClass), false);
        for (PatchSpecEntry entry : resourceSpecEntries) {
            spec.entry(entry);
        }

        try {
            addClassDependencies(spec, taskClass.newInstance().getClassDependencies());
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding class dependencies for: " + taskClass);
        }
    }

    public static void addClassDependencies(PatchSpec spec, String[] classDependencies) throws ClassNotFoundException {
        for (String className : classDependencies) {
            spec.entry(new PatchSpecClassEntry(Class.forName(className)), false);
        }
    }

    /** Resource (file) name, included in the patch JAR, holding PatchTask parameters */
    public static String generateResourceDirectory(Class<?> taskClass) {
        return taskClass.getPackage().getName().replace(".", "/") + "/" + taskClass.getSimpleName() + getPropertiesSuffix() + "/";
    }

    // - PRIVATE

    private static synchronized String getPropertiesSuffix() {
        ++propertiesSuffix;
        return "_" + Integer.toString(propertiesSuffix);
    }

    public static int propertiesSuffix = 0;
}
