package com.l7tech.server.processcontroller.patching.builder;

import java.io.File;


/**
 * Interface for launching patch tasks.
 *
 * Implementations are instantiated reflectively and must provide a default constructor.
 * All resources that are needed at runtime are loaded from the patch JAR classpath,
 * where a PatchTaskBuilder has packaged them.
 *
 * The ordered list of patch tasks to be executed is loaded from the TASK_LIST_ENTRY_NAME
 * JAR entry.
 */
public interface PatchTask {

    public static final int PATCH_TASK_SUCCESS = 0;
    public static final int PATCH_TASK_ERROR = 1;

    public static final String TASK_FILE = "patch_tasks.txt";
    public static final String TASK_LIST_ENTRY_NAME = PatchMain.class.getPackage().getName().replace(".", File.separator) + "/" + TASK_FILE;

    void runPatch() throws Exception;

}
