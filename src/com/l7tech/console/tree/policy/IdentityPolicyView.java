package com.l7tech.console.tree.policy;

import com.l7tech.console.panels.IdentityPolicyPanel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.action.Actions;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;

/**
 * The identities policy view
 *
 * @author Emil Marceta
 * @version 1.2
 */
public class IdentityPolicyView extends JDialog {
    private JPanel windowContentPane = null;
    private IdentityAssertionTreeNode idAssertion;
    private JPanel idPanel;
    private ServiceNode serviceNode;

    public IdentityPolicyView(Frame owner, IdentityAssertionTreeNode ida)
      throws FindException, IOException {
        super(owner, false);
        idAssertion = ida;
        serviceNode = getServiceNodeCookie();
        if (serviceNode == null) {
            throw new IllegalStateException("No service node found in the policy tree");
        }
        initialize();
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

    /**
     * Return the JWindowContentPane property value.
     * @return javax.swing.JPanel
     */
    private JPanel getWindowContentPane() throws FindException, IOException {
        if (windowContentPane == null) {
            windowContentPane = new JPanel();
            //windowContentPane.setBorder(new javax.swing.border.EtchedBorder());
            windowContentPane.setLayout(new BorderLayout());
            getWindowContentPane().add(getIdentityPolicyPanel(), "Center");
        }
        return windowContentPane;
    }

    private JPanel getIdentityPolicyPanel()
      throws FindException, IOException {
        if (idPanel != null) {
            return idPanel;
        }
        JTree tree =
          (JTree)TopComponents.
          getInstance().getComponent(PolicyTree.NAME);
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            idPanel = new IdentityPolicyPanel(serviceNode.getPublishedService(), model, idAssertion);
            return idPanel;
        }
        throw new IllegalStateException("Internal error: Could not get the policy tree.");
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
    private void initialize() throws FindException, IOException {
        setTitle("Identity Policy - " + extractName((IdentityAssertion)idAssertion.asAssertion()));
        setContentPane(getWindowContentPane());
        Actions.setEscKeyStrokeDisposes(this);
        initializeListeners();
    }


    /**
     * @return the published service cookie or null if not founds
     */
    private ServiceNode getServiceNodeCookie() {
        for (Iterator i = ((AbstractTreeNode)idAssertion.getRoot()).cookies(); i.hasNext();) {
            Object value = ((Cookie)i.next()).getValue();
            if (value instanceof ServiceNode) return (ServiceNode)value;
        }
        return null;
    }

}