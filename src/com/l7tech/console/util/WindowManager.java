package com.l7tech.console.util;

import com.l7tech.console.panels.WorkSpacePanel;

/**
 * Central manager of windows in the Policy editor.
 * Handles the work with workspaces, trees etc.
 *
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 
 */
public class WindowManager {
    protected static WindowManager instance = new WindowManager();
    /**
     * protected constructor, this class cannot be instantiated
     */
    protected WindowManager() {
    }

    public static WindowManager getInstance() {
        return instance;
    }
    /** Current workspace. Can be changed by calling Workspace.activate ()
    */
    public WorkSpacePanel getCurrentWorkspace() {
        return workSpacePanel;
    }

    private WorkSpacePanel workSpacePanel = new WorkSpacePanel();
}
