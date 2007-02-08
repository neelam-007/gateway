package com.l7tech.console.util;

import com.l7tech.common.gui.util.SheetHolder;
import com.l7tech.console.MainWindow;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.security.PermissionRefreshListener;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.policy.AssertionFinder;
import com.l7tech.policy.AssertionRegistry;

import javax.swing.*;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

/**
 * The class is Central UI component registry in the SSM.
 * Provides component unregister/register and access to the top
 * level components such as workspace, top level trees etc.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class TopComponents {
    private static final TopComponents instance = new TopComponents();

    public void showHelpTopics() {
        getMainWindow().showHelpTopicsRoot();
    }

    public void updateLastActivityTime() {
        getMainWindow().updateLastActivityTime();
    }

    public void firePolicyEditDone() {
        getMainWindow().firePolicyEditDone();
    }

    public void firePolicyEdit(PolicyEditorPanel pep) {
        getMainWindow().firePolicyEdit(pep);
    }

    public String ssgURL() {
        return getMainWindow().ssgURL();
    }

    public SsmPreferences getPreferences() {
        return getMainWindow().getPreferences();
    }

    public void disconnectFromGateway() {
        MainWindow mw = getMainWindow();
        if (mw != null)
            mw.disconnectFromGateway();
    }

    public void updateNodeNameInStatusMessage(String oldGatewayName, String newName) {
        getMainWindow().updateNodeNameInStatusMessage(oldGatewayName, newName);
    }

    public void addPermissionRefreshListener(PermissionRefreshListener listener) {
        getMainWindow().addPermissionRefreshListener(listener);
    }

    public void firePermissionRefresh() {
        getMainWindow().firePermissionRefresh();
    }

    public void setInactivitiyTimeout(int timeout) {
        MainWindow mw = getMainWindow();
        if (mw != null) mw.setInactivitiyTimeout(timeout);
    }

    public PolicyToolBar getPolicyToolBar() {
        return getMainWindow().getPolicyToolBar();
    }

    public boolean isDisconnected() {
        return getMainWindow().isDisconnected();
    }

    public void showNoPrivilegesErrorMessage() {
        getMainWindow().showNoPrivilegesErrorMessage();
    }

    public AssertionRegistry getAssertionRegistry() {
        return (AssertionRegistry)getApplicationContext().getBean("assertionRegistry", AssertionRegistry.class);
    }

    /** Interface implemented by lazy component finders. */
    public static interface ComponentFinder {
        Component findComponent();
    }

    /**
     * protected constructor, this class cannot be instantiated
     */
    protected TopComponents() {}

    public static TopComponents getInstance() {
        return instance;
    }

    /**
     * Get the Main Window of the application.
     * <p/>
     * This should NOT be used for a dialog parent -- use getTopParent() instead, which works both in and
     * out of applet mode.
     *
     * @return the applicaiton Main Window
     */
    private MainWindow getMainWindow() {
        Component c = getComponent("mainWindow");
        if (c instanceof MainWindow) return (MainWindow)c;

        Frame[] frames = JFrame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            Frame frame = frames[i];
            if (frame instanceof MainWindow) {
                return (MainWindow)frame;
            }
        }
        return null;
    }

    /** @return the last active applet holding our main UI panel, or null if none is registered. */
    private JApplet getMainApplet() {
        Component c = getComponent("appletMain");
        if (c instanceof JApplet) return (JApplet)c;
        return null;
    }

    /**
     * Get the top level parent.
     * @return the parent frame to use for dialogs that don't have any other parent to use
     */
    public Frame getTopParent() {
        Component c = getComponent("topLevelParent");
        if (c instanceof Frame) return (Frame)c;

        return getMainWindow();
    }

    /**
     * Same as getTopParent(), but returns as a RootPaneContainer instead.  This is a convenience method
     * to prevent having to do your own downcasting.
     *
     * @return  the RootPaneContainer of the parent frame of the application (either standalone or applet)
     */
    public SheetHolder getRootSheetHolder() {
        JApplet applet = getMainApplet();
        if (applet instanceof SheetHolder)
            return (SheetHolder)applet;

        Frame frame = getTopParent();
        if (frame instanceof SheetHolder)
            return (SheetHolder)frame;

        return getMainWindow();
    }

    /**
     * Has the Main Window initialized.
     * <p/>
     *
     * @return true if the main  Window has initialized,false otherwise
     */
    public boolean hasMainWindow() {
        Component c = getComponent("mainWindow");
        if (c instanceof MainWindow) return true;

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
     * @return the workspace panel.  never null
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
     * @return  the ApplicationContext for which the current MainWindow was created.
     */
    public ApplicationContext getApplicationContext() {
        return getMainWindow().getSsmApplication().getApplicationContext();
    }

    /**
     * Returns the default policy tree component from component registry.
     * @return the PolicyTree.  never null
     */
    public JTree getPolicyTree() {
        synchronized (componentsRegistry) {
            JTree tree = (JTree)getComponent(PolicyTree.NAME);
            if (tree != null) return tree;
            PolicyTree policyTree = new PolicyTree(getApplicationContext());
            registerComponent(PolicyTree.NAME, policyTree);
            return policyTree;
        }
    }

    /**
     * Returns the component with the given name or <b>null</b> if none
     * found.  May invoke a registered componentFinder to lazily locate the component.
     *
     * @param name  the component name
     * @return  the specified component, or null if there wasn't one.
     */
    public Component getComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference<Component> wr = componentsRegistry.get(name);
            if (wr == null) return null;
            Component c = wr.get();
            if (c == null) {
                componentsRegistry.remove(name);
                ComponentFinder cf = componentFinderRegistry.get(name);
                if (cf == null) return null;
                c = cf.findComponent();
                if (c == null) return null;
                componentsRegistry.put(name, new WeakReference<Component>(c));
            }
            return c;
        }
    }

    /**
     * Registers the component with the given name
     *
     * @param name the component name
     * @param component  the component to register
     */
    public void registerComponent(String name, Component component) {
        synchronized (componentsRegistry) {
            Component c = getComponent(name);
            if (c != null)
                throw new RuntimeException("There is an active component by name '" + name + "'");
            componentsRegistry.put(name, new WeakReference<Component>(component));
        }
    }

    /**
     * Registers a component finder with the given name.  It will be invoked
     * the first time a request is made for the named component.
     *
     * @param name   the component name
     * @param componentFinder   a finder that will lazily locate the component when needed for the first time
     */
    public void registerComponent(String name, ComponentFinder componentFinder) {
        synchronized (componentsRegistry) {
            Component c = getComponent(name);
            if (c != null)
                throw new RuntimeException("There is an active component by name '" + name + "'");
            componentFinderRegistry.put(name, componentFinder);
        }
    }

    /**
     * Unregisters the component with the given name
     * @param name the name of the component to unregister
     * @return the previous active component with that name, or null if there wan't one
     */
    public Component unregisterComponent(String name) {
        synchronized (componentsRegistry) {
            WeakReference<Component> wr = componentsRegistry.remove(name);
            if (wr == null) return null;
            return wr.get();
        }
    }

    /**
     * @return  True if the SSM is currently running as an applet.
     */
    public boolean isApplet() {
        return getMainWindow().isApplet();
    }

    private final Map<String, WeakReference<Component>> componentsRegistry = new HashMap<String, WeakReference<Component>>();
    private final Map<String, ComponentFinder> componentFinderRegistry = new HashMap<String, ComponentFinder>();
}
