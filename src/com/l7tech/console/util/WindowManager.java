package com.l7tech.console.util;

import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.MainWindow;

import javax.swing.*;
import java.util.Map;
import java.util.HashMap;
import java.lang.ref.WeakReference;
import java.awt.*;
import java.io.IOException;

/**
 * Central manager of windows in the Policy editor.
 *
 * Handles the work with workspaces, trees amd other reusable top
 * level components.
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

    /** Get the Main Window of the application.
     * This should be used for:
     * <UL>
     *   <LI>using the Main Window as the parent for dialogs</LI>
     *   <LI>using the Main Window's position for preplacement of windows</LI>
     * </UL>
     * @return the applicaiton Main Window
     */
    public JFrame getMainWindow() {
          synchronized (componentsRegistry) {
            JFrame main = (JFrame)getComponent(MainWindow.NAME);
            if (main != null) return main;
              try {
                  MainWindow m = new MainWindow();
                  registerComponent(MainWindow.NAME, m);
                  return m;
              } catch (IOException e) {
                  throw new RuntimeException("Faile to initialize main window", e);
              }
        }
    }

    /**
     * Current workspace.
     */
    public WorkSpacePanel getCurrentWorkspace() {
        return workSpacePanel;
    }

    /**
     * Returns the default policy tree component from component registry.
     */
    public JTree getPolicyTree() {
        synchronized (componentsRegistry) {
            JTree tree = (JTree)getComponent(PolicyTree.NAME);
            if (tree != null) return tree;
            PolicyTree policyTree = new PolicyTree();
            registerComponent(PolicyTree.NAME, policyTree);
            return policyTree;
        }
    }

    /**
     * Returns the component with the given name or <b>null</b> if none
     * found.
     */
    public Component getComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference wr = (WeakReference)componentsRegistry.get(name);
            if (wr == null) return null;
            Component c = (Component)wr.get();
            if (c == null) componentsRegistry.remove(name);
            return c;
        }
    }

    /**
     * Registers the component with the given name
     * @param name the component name
     */
    public void registerComponent(String name, Component component) {
        synchronized (componentsRegistry) {
            Component c = getComponent(name);
            if (c != null)
                throw new RuntimeException("There is an active component by name '" + name + "'");
            componentsRegistry.put(name, new WeakReference(component));
        }
    }

    /**
     * Returns the component with the given name or <b>null</b> if none
     * found.
     */
    public Component unregisterComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference wr = (WeakReference)componentsRegistry.get(name);
            if (wr == null) return null;
            Component c = (Component)wr.get();
            if (c == null) componentsRegistry.remove(name);
            return c;
        }
    }

    private Map componentsRegistry = new HashMap();
    private WorkSpacePanel workSpacePanel = new WorkSpacePanel();

}
