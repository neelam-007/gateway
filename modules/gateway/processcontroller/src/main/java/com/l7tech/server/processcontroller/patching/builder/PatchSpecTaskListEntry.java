package com.l7tech.server.processcontroller.patching.builder;

import static com.l7tech.server.processcontroller.patching.builder.PatchTask.TASK_LIST_ENTRY_NAME;
import com.l7tech.util.Pair;

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.*;
import java.io.IOException;

/**
 * Patch entry specification for the special file that lists the patch tasks included in the patch.
 *
 * @author jbufu
 */
public class PatchSpecTaskListEntry implements PatchSpecEntry {

    // - PUBLIC

    public enum Position { PRE, MAIN, POST }

    @Override
    public String getEntryName() {
        return TASK_LIST_ENTRY_NAME;
    }

    @Override
    public void setEntryName(String entryName) {
        throw new IllegalStateException(TASK_LIST_ENTRY_NAME + " entry name cannot be modified");
    }

    @Override
    public void toJar(JarOutputStream jos) throws IOException {
        jos.putNextEntry(new ZipEntry(TASK_LIST_ENTRY_NAME));
        for (Position pos : Position.values()) {
            for (Pair<Class<? extends PatchTask>, String> taskEntry : taskLists.get(pos)) {
                jos.write(taskEntry.left.getName().getBytes());
                String resourceDir = taskEntry.right;
                if (resourceDir != null) {
                    jos.write(" ".getBytes());
                    jos.write(resourceDir.getBytes());
                }
                jos.write("\n".getBytes());
            }
        }
        jos.closeEntry();
    }

    public void addTask(Class<? extends PatchTask> taskClass, String resourceDirEntry) {
        addTask(taskClass, resourceDirEntry, Position.MAIN);
    }

    public void addTask(Class<? extends PatchTask> taskClass, String resourceDirEntry, Position position) {
        taskLists.get(position).add(new Pair<Class<? extends PatchTask>,String>(taskClass, resourceDirEntry));
    }

    // - PRIVATE

    private Map<Position,List<Pair<Class<? extends PatchTask>,String>>> taskLists =
        new HashMap<Position, List<Pair<Class<? extends PatchTask>, String>>>() {{
            put(Position.PRE, new ArrayList<Pair<Class<? extends PatchTask>, String>>());
            put(Position.MAIN, new ArrayList<Pair<Class<? extends PatchTask>, String>>());
            put(Position.POST, new ArrayList<Pair<Class<? extends PatchTask>, String>>());
        }};
}
