package com.l7tech.console.tree;

import com.l7tech.common.gui.util.SwingWorker;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * The class performs a JTree loading on a dedicated thread.
 * It is used for async node loading, with the goal of making
 * the UI more responsive.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.5
 */
public class TreeWorker extends SwingWorker {
    // current worker
    private static TreeWorker worker;
    private TreePath path;
    private AbstractTreeNode dnode;
    private JTree jTree;
    private DefaultTreeModel model;

    /**
     * start retrieving children for this node on the dedicated
     * thread
     *
     * @param path the tree path
     * @param tree   JTree that displays the nodes
     */
    public static
      synchronized void startWorker(TreePath path, JTree tree) {
        //log.debug("startWorker "+Thread.currentThread());
        stopWorker();
        worker = new TreeWorker(path, tree);
        worker.start();
        worker.startProgressIndicator();
    }

    /**
     *
     * @return true of there is active worker, false otherwise
     */
    public static boolean active() {
        return worker != null;
    }

    /**
     * Stop the <code>TreeWorker</code> if it has been started
     */
    public static synchronized void stopWorker() {
        // log.debug("stopWorker "+Thread.currentThread());
        if (worker != null) {
            worker.interrupt();
            worker.stopProgressIndicator();
            worker = null;
        }
    }

    /**
     * private constructor, the instances are created using
     * startWorker method.
     *
     * @param path  the TreePath for this TreeWorker
     * @param tree   the JTree thet node belongs to
     */
    private TreeWorker(TreePath path, JTree tree) {
        this.path = path;
        dnode = (AbstractTreeNode) path.getLastPathComponent();
        this.jTree = tree;
        this.model = (DefaultTreeModel) jTree.getModel();
    }

    /**
     * Compute the value to be returned by the <code>get</code> method.
     */
    public Object construct() {
        dnode.removeAllChildren();
        dnode.loadChildren();
        return dnode.children();
    }

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    public void finished() {
        // log.debug("finished "+Thread.currentThread());
        /* tell the tree the structure has been changed
           Set the worker to null and stop the animation,
           but only if we are the active worker. */
        if (worker == this) {
            worker = null;
            model.nodeStructureChanged(dnode);
            jTree.expandPath(path);
            stopProgressIndicator();
        }
    }

    /**
     * invoke start progress indicator on the Swing Event
     * dispatching thread
     */
    private void startProgressIndicator() {
        final Runnable start = new Runnable() {
            public void run() {
//        MainWindow.startProgressIndicator();
            }
        };
        SwingUtilities.invokeLater(start);
    }

    /**
     * invoke stop progress indicator on the Swing Event
     * dispatching thread
     */
    private void stopProgressIndicator() {
        final Runnable stop = new Runnable() {
            public void run() {
                //      MainWindow.stopProgressIndicator();
            }
        };
        SwingUtilities.invokeLater(stop);
    }
}
