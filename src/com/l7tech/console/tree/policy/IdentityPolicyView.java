package com.l7tech.console.tree.policy;

import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.Set;

/**
 * The identities policy view
 *
 * @author Emil Marceta
 * @version 1.2
 */
public class IdentityPolicyView extends JDialog {
    private JPanel windowContentPane = null;
    private JScrollPane scrollPane = null;
    private JTree policyTree = null;
    private IdentityAssertion idAssertion;

    /** * SplashScreen constructor comment. * @param owner java.awt.Frame */
    public IdentityPolicyView(Frame owner, IdentityAssertion ida) {
        super(owner, true);
        idAssertion = ida;
        initialize();
    }

    /**
     * Return the JScrollPane property value.
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getScrollPane() {
        if (scrollPane != null) return scrollPane;
        scrollPane = new JScrollPane(getPolicyTree());
        return scrollPane;
    }

    private JTree getPolicyTree() {
        if (policyTree != null) return policyTree;
        policyTree = new JTree();
        policyTree.setCellRenderer(new EntityTreeCellRenderer());
        Assertion root = idAssertion.getPath()[0];
        Set paths = IdentityPath.getPaths(root);
        for (Iterator i = paths.iterator(); i.hasNext();) {
            IdentityPath ip = (IdentityPath)i.next();
            if (ip.getPrincipal().getName().equals(extractName(idAssertion))) {
                IdentityPolicyTreeNode n = new IdentityPolicyTreeNode(ip, root);
                PolicyTreeModel model = new PolicyTreeModel(n);
                FilteredTreeModel fm = new FilteredTreeModel((TreeNode)model.getRoot());
                fm.setFilter(new IdentityNodeFilter());
                policyTree.setModel(fm);
                break;
            }
        }
        return policyTree;
    }

    private String extractName(IdentityAssertion ida) {
        if (ida instanceof MemberOfGroup) {
            MemberOfGroup mg = (MemberOfGroup)ida;
            return mg.getGroupName();
        } else if (ida instanceof SpecificUser) {
            SpecificUser su = (SpecificUser)ida;
            return su.getUserLogin();
        }
        throw new IllegalArgumentException("Don't know how to handle class " + ida.getClass());
    }

    /** * Return the JWindowContentPane property value. * @return javax.swing.JPanel */
    private JPanel getWindowContentPane() {
        if (windowContentPane == null) {
            windowContentPane = new JPanel();
            //windowContentPane.setBorder(new javax.swing.border.EtchedBorder());
            windowContentPane.setLayout(new BorderLayout());
            getWindowContentPane().add(getScrollPane(), "Center");
        }
        return windowContentPane;
    }

    /** * @param e java.awt.event.WindowEvent */
    private void windowClosingHandler(WindowEvent e) {
        this.dispose();
    }

    /** * Initializes listeners */
    private void initializeListeners() {
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                windowClosingHandler(e);
            }
        });
    }

    /** * Initialize the class. */
    private void initialize() {
        setTitle("Identity Policy - " + extractName(idAssertion));
        FontMetrics metrics = getFontMetrics(getFont());
        Graphics g = getGraphics();
        Rectangle2D textBounds = metrics.getStringBounds(getTitle(), g);
        setSize((int)textBounds.getWidth() + 50, 200);
        setContentPane(getWindowContentPane());
        initializeListeners();
    }

    private static class IdentityNodeFilter implements NodeFilter {
        /**
         * @param node the TreeNode to examine
         * @return true if filter accepts the node, false otherwise
         */
        public boolean accept(TreeNode node) {
            if (node instanceof SpecificUserAssertionTreeNode || node instanceof MemberOfGroupAssertionTreeNode) return false;
            if (node instanceof CompositeAssertionTreeNode) {
                if (((CompositeAssertionTreeNode)node).getChildCount(this) == 0) return false;
            }
            TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
            IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[0];
            AssertionTreeNode an = (AssertionTreeNode)node;
            IdentityPath ip = in.getIdentityPath();
            Set paths = ip.getPaths();
            for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
                Assertion[] apath = (Assertion[])iterator.next();
                for (int i = apath.length - 1; i >= 0; i--) {
                    Assertion assertion = apath[i];
                    if (assertion.equals(an.asAssertion())) return true;
                }
            }
            return false;
        }
    }
}