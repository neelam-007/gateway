package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AssertionMoveUpAction;
import com.l7tech.console.action.AssertionMoveDownAction;
import com.l7tech.console.action.AddAssertionAction;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The policy toolbar with toolbar actions and listeners.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyToolBar extends JToolBar {
    static Logger log = Logger.getLogger(PolicyToolBar.class.getName());
    public static final String NAME = "policy.toolbar";
    private AssertionMoveUpAction assertionMoveUpAction;
    private AssertionMoveDownAction assertionMoveDownAction;
    private AddAssertionAction addAssertionAction;
    private DeleteAssertionAction deleteAssertionAction;

    public PolicyToolBar() {
        initialize();
    }

    /**
     * register the toolbar with the palette assertion tree.
     *
     * @param tree the assertion tree
     */
    public void registerPaletteTree(JTree tree) {
        tree.addTreeSelectionListener(assertionPaletteListener);
    }

    /**
     * register the toolbar with the policy editor tree.
     *
     * @param tree the assertion tree
     */
    public void registerPolicyTree(JTree tree) {
        tree.addTreeSelectionListener(policyTreeListener);
    }

    /**
     * initialize the toolbar
     */
    private void initialize() {
        addSeparator();
        setOrientation(JToolBar.VERTICAL);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        setFloatable(false);

        JButton b = add(getAddAssertionAction());
        b.setEnabled(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getAssertionMoveUpAction());
        b.setEnabled(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getAssertionMoveDownAction());
        b.setEnabled(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();

        b = add(getDeleteAssertionAction());
        b.setEnabled(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        addSeparator();
    }

    private Action getAssertionMoveUpAction() {
        if (assertionMoveUpAction != null)
            return assertionMoveUpAction;
        assertionMoveUpAction = new AssertionMoveUpAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
            }
        };

        return assertionMoveUpAction;

    }

    private Action getAssertionMoveDownAction() {
        if (assertionMoveDownAction != null)
            return assertionMoveDownAction;
        assertionMoveDownAction = new AssertionMoveDownAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
            }
        };

        return assertionMoveDownAction;

    }

    private Action getAddAssertionAction() {
        if (addAssertionAction != null)
            return addAssertionAction;
        addAssertionAction = new AddAssertionAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
                this.paletteNode = lastPaletteNode;
                this.assertionNode = lastAssertionNode;
                super.performAction();
                lastAssertionNode = null;
                updateActions();
            }
        };
        return addAssertionAction;
    }


    private Action getDeleteAssertionAction() {
        if (deleteAssertionAction != null)
            return deleteAssertionAction;
        deleteAssertionAction = new DeleteAssertionAction() {
            /**
             * Invoked when an action occurs.
             */
            public void performAction() {
               this.node = lastAssertionNode;
                super.performAction();
                this.node = null;
                updateActions();
            }
        };
        return deleteAssertionAction;
    }

    private void updateActions() {
        if (lastAssertionNode == null) {
            disableAll();
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getAddAssertionAction().setEnabled(lastAssertionNode.accept(lastPaletteNode));
                getDeleteAssertionAction().setEnabled(lastAssertionNode.canDelete());
            }
        });
    }

    private void disableAll() {
        Component[] components = getComponents();
        for (int i = components.length -1; i >=0; i--) {
            if (components[i] instanceof JButton) {
                components[i].setEnabled(false);
            }
        }
    }

    private TreeSelectionListener
      assertionPaletteListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
            try {
                TreePath path = e.getPath();
                lastPaletteNode = (AbstractTreeNode)path.getLastPathComponent();
                updateActions();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    };

    private TreeSelectionListener
      policyTreeListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
            try {
                TreePath path = e.getPath();
                lastAssertionNode = (AssertionTreeNode)path.getLastPathComponent();
                updateActions();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    };

    private AbstractTreeNode lastPaletteNode;
    private AssertionTreeNode lastAssertionNode;

}
