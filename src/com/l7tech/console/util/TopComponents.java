package com.l7tech.console.util;

import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.tree.policy.PolicyTree;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * The class is Central UI component registry in the SSM.
 * Provides component unregister/register and access to the top
 * level components such as workspace, top level trees etc.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class TopComponents {
    protected static TopComponents instance = new TopComponents();

    /**
     * protected constructor, this class cannot be instantiated
     */
    protected TopComponents() {}

    public static TopComponents getInstance() {
        return instance;
    }

    /**
     * Get the Main Window of the application.
     * This should be used for:
     * <UL>
     * <LI>using the Main Window as the parent for dialogs</LI>
     * <LI>using the Main Window's position for preplacement of windows</LI>
     * </UL>
     *
     * @return the applicaiton Main Window
     */
    public MainWindow getMainWindow() {
        Frame[] frames = JFrame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            Frame frame = frames[i];
            if (frame instanceof MainWindow) {
                return (MainWindow)frame;
            }
        }
        return null;
    }

    /**
     * Has the Main Window initialized.
     * <p/>
     *
     * @return true if the main  Window has initialized,false otherwise
     */
    public boolean hasMainWindow() {
        Frame[] frames = JFrame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            Frame frame = frames[i];
            if (frame instanceof MainWindow) {
                return true;
            }
        }
        return false;
    }


    /**
     * Current workspace.
     */
    public WorkSpacePanel getCurrentWorkspace() {
        synchronized (componentsRegistry) {
            WorkSpacePanel wp =
              (WorkSpacePanel)getComponent(WorkSpacePanel.NAME);
            if (wp != null) return wp;
            wp = new WorkSpacePanel();
            registerComponent(WorkSpacePanel.NAME, wp);
            return wp;
        }
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
     *
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
     * Unregisters the component with the given name
     */
    public Component unregisterComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference wr = (WeakReference)componentsRegistry.remove(name);
            if (wr == null) return null;
            Component c = (Component)wr.get();
            return c;
        }
    }

    private Map componentsRegistry = new HashMap();

}
