package com.l7tech.console.panels;

import com.l7tech.console.action.SavePolicyAction;
import com.l7tech.console.action.ValidatePolicyAction;
import com.l7tech.console.action.PolicyIdentityViewAction;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.text.EditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;

/**
 * The class represnts the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel {
    private PublishedService service;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private JTree policyTree;

    public PolicyEditorPanel(PublishedService svc) {
        this.service = svc;
        layoutComponents();
        setName(svc.getName());
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        WindowManager windowManager =
          Registry.getDefault().getWindowManager();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        splitPane.add(getPolicyTreePane(windowManager), "top");
        splitPane.add(getMessagePane(), "bottom");
        splitPane.setDividerSize(2);
        splitPane.setName(service.getName());
        add(getToolBar(), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }


    private JComponent getPolicyTreePane(WindowManager windowManager) {
        policyTree = windowManager.getPolicyTree();
        PolicyTreeModel model = PolicyTreeModel.make(service);
        rootAssertion = (AssertionTreeNode)model.getRoot();
        policyTree.setModel(new FilteredTreeModel((TreeNode)model.getRoot()));
        policyTree.setName(service.getName());
        JScrollPane scrollPane = new JScrollPane(policyTree);
        return scrollPane;

    }

    private JComponent getMessagePane() {
        messagesTextPane = new JTextPane();
        messagesTextPane.setText("");
        messagesTextPane.setEditable(false);
        JTabbedPane tabbedPane = new JTabbedPane();
        JScrollPane scrollPane = new JScrollPane(messagesTextPane);
        tabbedPane.addTab("Messages", scrollPane);
        return tabbedPane;
    }


    /**
     * Return the ToolBarForTable instance for a given node or null.
     * @return ToolBarForTable
     */
    private PolicyEditToolBar getToolBar() {
        PolicyEditToolBar tb = new PolicyEditToolBar();
        tb.setFloatable(false);
        return tb;
    }

    /**
     */
    final class PolicyEditToolBar extends JToolBar {
        JButton buttonSave;
        JButton buttonValidate;
        JToggleButton identityViewButton;

        public PolicyEditToolBar() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonSave = new JButton(new SavePolicyAction(rootAssertion));
            this.add(buttonSave);

            buttonValidate = new JButton(new ValidatePolicyAction());
            this.add(buttonValidate);
            buttonValidate.addActionListener(
              new ActionListener() {
                  /** Invoked when an action occurs.*/
                  public void actionPerformed(ActionEvent e) {
                      PolicyValidatorResult result
                        = PolicyValidator.getDefault().
                        validate(rootAssertion.asAssertion());
                      for (Iterator iterator = result.getErrors().iterator();
                           iterator.hasNext();) {
                          PolicyValidatorResult.Error pe =
                            (PolicyValidatorResult.Error)iterator.next();
                          appendToMessageArea("Assertion : " +
                            pe.getAssertion().getClass() + " Error :" + pe.getMessage());
                      }
                      if (result.getErrors().isEmpty()) {
                          appendToMessageArea("Policy validated ok.");
                      }
                  }
              });
            identityViewButton = new JToggleButton(new PolicyIdentityViewAction());
            identityViewButton.addActionListener( new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean selected = identityViewButton.isSelected();
                    PolicyTreeModel model = PolicyTreeModel.make(service);
                    FilteredTreeModel fm = new FilteredTreeModel((TreeNode)model.getRoot());
                    policyTree.setModel(fm);
                    if (selected) {
                        fm.setFilter(new NodeFilter() {
                            /**
                             * @param node  the <code>TreeNode</code> to examine
                             * @return  true if filter accepts the node, false otherwise
                             */
                            public boolean accept(TreeNode node) {
                                if (node instanceof MemberOfGroupAssertionTreeNode ||
                                    node instanceof SpecificUserAssertionTreeNode) {
                                         return false;
                                }
                                if (node instanceof CompositeAssertionTreeNode) {
                                    return ((CompositeAssertionTreeNode)node).getChildCount(this) >0;
                                }
                                return true;
                            }


                        });
                    } else {
                        fm.clearFilter();
                    }
                }
            });
            this.add(identityViewButton);
            Utilities.
              equalizeComponentSizes(
                new JComponent[]{
                    buttonSave, buttonValidate, identityViewButton
                });
        }
    }

    private void appendToMessageArea(String s) {
        try {
            int pos = messagesTextPane.getText().length();
            if (pos > 0) s = "\n" + s;
            StringReader sr = new StringReader(s);
            EditorKit editorKit = messagesTextPane.getEditorKit();
            editorKit.read(sr, messagesTextPane.getDocument(), pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void overWriteMessageArea(String s) {
        messagesTextPane.setText(s);
    }

}
