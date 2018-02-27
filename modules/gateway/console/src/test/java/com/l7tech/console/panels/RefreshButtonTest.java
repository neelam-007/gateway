package com.l7tech.console.panels;


import com.l7tech.console.MainWindow;
import com.l7tech.console.action.RefreshAction;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefreshButtonTest {
    @Mock
    private MainWindow mainWindow;

    @Mock
    private AbstractTreeNode serviceNode;

    @Spy
    private ServicesAndPoliciesTree servicesAndPoliciesTree;

    @Before
    public void setup() {
        //mock refresh ConnectionListener
        Whitebox.setInternalState(mainWindow, "listenerList", new WeakEventListenerList());

        //mock class loader
        Whitebox.setInternalState(mainWindow, "cl", MainWindow.class.getClassLoader());

        //prevent HeadlessException thrown
        doNothing().when(servicesAndPoliciesTree).setDragEnabled(true);

        //create and add servicesAndPoliciesTree to main window
        servicesAndPoliciesTree = new ServicesAndPoliciesTree((DefaultTreeModel) new JTree(serviceNode).getModel());
        Whitebox.setInternalState(mainWindow, "servicesAndPoliciesTree", servicesAndPoliciesTree);

        //mock refresh action
        when(serviceNode.getActions(any())).thenReturn(new RefreshAction[]{});
    }

    @Test
    public void testNoSelectedNodesAfterRefresh() throws Exception {
        //select a node from servicesAndPoliciesTree
        servicesAndPoliciesTree.setSelectionPath(new TreePath(serviceNode));
        Assert.assertEquals("One node should be selected", 1, servicesAndPoliciesTree.getSelectionCount());

        //init and perform refresh action
        Action refreshAction = Whitebox.invokeMethod(mainWindow, "getRefreshAction");
        refreshAction.actionPerformed(new ActionEvent(new Object(), 0, "Refresh"));

        Assert.assertEquals("No node should be selected", 0, servicesAndPoliciesTree.getSelectionCount());
    }

}
