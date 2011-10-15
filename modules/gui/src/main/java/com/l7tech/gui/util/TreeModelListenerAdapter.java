package com.l7tech.gui.util;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * There doesn't seem to be one of these in the JDK.
 */
public class TreeModelListenerAdapter implements TreeModelListener {
    @Override
    public void treeNodesChanged( final TreeModelEvent e ) {
    }

    @Override
    public void treeNodesInserted( final TreeModelEvent e ) {
    }

    @Override
    public void treeNodesRemoved( final TreeModelEvent e ) {
    }

    @Override
    public void treeStructureChanged( final TreeModelEvent e ) {
    }
}
