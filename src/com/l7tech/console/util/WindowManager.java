package com.l7tech.console.util;

import com.l7tech.console.panels.WorkSpacePanel;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;

/**
 * Central manager of windows in the Policy editor.
 * Handles the work with workspaces, trees etc.
 *
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
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

    /** Get the Main Window of the applicaiton.
     * This should ONLY be used for:
     * <UL>
     *   <LI>using the Main Window as the parent for dialogs</LI>
     *   <LI>using the Main Window's position for preplacement of windows</LI>
     * </UL>
     * @return the applicaiton Main Window
     */
    public JFrame getMainWindow() {
        return null;
    }

    /**
     * Current workspace.
     */
    public WorkSpacePanel getCurrentWorkspace() {
        return workSpacePanel;
    }

    /**
     * Returns the component with the given name or <b>null</b> if none
     * found.
     */
    public JComponent getComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference wr = (WeakReference)componentsRegistry.get(name);
            if (wr == null) return null;
            JComponent jc = (JComponent)wr.get();
            if (jc == null) componentsRegistry.remove(name);
            return jc;
        }
    }

    /**
     * Registers the component with the given name
     * @param name the component name
     */
    public void registerComponent(String name, JComponent component) {
        synchronized (componentsRegistry) {
            JComponent jc = getComponent(name);
            if (jc != null)
                throw new RuntimeException("There is an active component by name '" + name + "'");
            componentsRegistry.put(name, new WeakReference(component));
        }
    }

    /**
     * Returns the component with the given name or <b>null</b> if none
     * found.
     */
    public JComponent unregisterComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference wr = (WeakReference)componentsRegistry.get(name);
            if (wr == null) return null;
            JComponent jc = (JComponent)wr.get();
            if (jc == null) componentsRegistry.remove(name);
            return jc;
        }
    }

    private Map componentsRegistry = new HashMap();
    private WorkSpacePanel workSpacePanel = new WorkSpacePanel();

}
