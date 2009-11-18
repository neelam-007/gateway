package com.l7tech.server.processcontroller.patching.builder;

import static com.l7tech.server.processcontroller.patching.builder.PatchTask.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.*;

/**
 * The Main-Class entry point for patch / JAR file.
 * Loads and runs all PatchTasks included with the patch.
 *
 * @author jbufu
 */
public class PatchMain {

    // - PUBLIC

    public static void main(String[] args) {

        try {
            InputStream tasksIn = PatchMain.class.getResourceAsStream(PatchTask.TASK_FILE);
            if (tasksIn != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(tasksIn));
                String taskEntry = reader.readLine();
                while(taskEntry != null) {
                    String[] splitEntry = taskEntry.split(" ");
                    PatchTask task = (PatchTask) Class.forName(splitEntry[0]).newInstance();
                    String resourceDirEntry = splitEntry.length > 1 ? splitEntry[1] : null;
                    task.runPatch(resourceDirEntry);
                    taskEntry = reader.readLine();
                }
            }
        } catch (Exception e) {
            System.out.println("Error installing patch: " + ExceptionUtils.getMessage(e));
            System.exit(PATCH_TASK_ERROR);
        }

        System.exit(PATCH_TASK_SUCCESS);
    }

    public static String[] getClassDependencies() {
        return new String[] {
            PatchMain.class.getName(),
            PatchTask.class.getName(),
            "com.l7tech.util.ResourceUtils",
            "com.l7tech.util.ExceptionUtils",
            "com.l7tech.util.IOUtils",
            "com.l7tech.util.BufferPool",
            "com.l7tech.util.BufferPool$Pool",
            "com.l7tech.util.BufferPool$HugePool",
            "com.l7tech.util.BufferPoolByteArrayOutputStream",
            "com.l7tech.util.BufferPoolByteArrayOutputStream$1",
        };
    }

    public static InputStream getResourceStream(Class<?> taskClass, String resourceName) throws IOException {
        String pkgDir = taskClass.getPackage().getName().replace(".", "/") + "/";
        return taskClass.getResourceAsStream(resourceName.startsWith(pkgDir) && resourceName.length() > pkgDir.length() ? resourceName.substring(pkgDir.length()) : resourceName);
    }

    public static String readResource(Class<?> taskClass, String resourceName) throws IOException {
        String result = null;
        InputStream is = getResourceStream(taskClass, resourceName);
        if (is != null) {
            try {
                result = new String(IOUtils.slurpStream(is));
            } finally {
                ResourceUtils.closeQuietly(is);
            }
        }
        return result;
    }
}
