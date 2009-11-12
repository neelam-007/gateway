package com.l7tech.server.processcontroller.patching.builder;

import static com.l7tech.server.processcontroller.patching.builder.PatchTask.*;
import com.l7tech.util.ExceptionUtils;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
            boolean busywait = true;
            while(busywait) {
                Thread.sleep(2000);
            }
            InputStream tasksIn = PatchMain.class.getResourceAsStream(PatchTask.TASK_FILE);
            if (tasksIn != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(tasksIn));
                String taskClassName = reader.readLine();
                while(taskClassName != null) {
                    PatchTask task = (PatchTask) Class.forName(taskClassName).newInstance();
                    task.runPatch();
                    taskClassName = reader.readLine();
                }
            }
        } catch (Exception e) {
            System.out.println("Error installing patch: " + ExceptionUtils.getMessage(e));
            System.exit(PATCH_TASK_ERROR);
        }

        System.exit(PATCH_TASK_SUCCESS);
    }
}
