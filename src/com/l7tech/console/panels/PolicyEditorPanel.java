package com.l7tech.console.panels;

import com.l7tech.console.action.SavePolicyAction;
import com.l7tech.console.action.ValidatePolicyAction;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.WindowManager;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.text.EditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;

/**
 * The class represnts the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel {
    private PublishedService service;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;

    public PolicyEditorPanel(PublishedService svc) {
        this.service = svc;
        layoutComponents();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        WindowManager windowManager =
          Registry.getDefault().getWindowManager();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        splitPane.add(getPolicyTreePane(windowManager, service), "top");
        splitPane.add(getMessagePane(), "bottom");
        splitPane.setDividerSize(2);
        splitPane.setName(service.getName());
        add(getToolBar(), BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }


    private JComponent getPolicyTreePane(WindowManager windowManager, PublishedService svc) {
        JTree tree = windowManager.getPolicyTree();
        PolicyTreeModel model = PolicyTreeModel.make(svc);
        rootAssertion = (AssertionTreeNode)model.getRoot();
        tree.setModel(model);
        tree.setName(svc.getName());
        JScrollPane scrollPane = new JScrollPane(tree);
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
    final class PolicyEditToolBar  extends JToolBar {
        JButton buttonSave;
        JButton buttonValidate;

        public PolicyEditToolBar() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonSave = new JButton(new SavePolicyAction());
            this.add(buttonSave);
            buttonSave.addActionListener(
                    new ActionListener() {
                        /** Invoked when an action occurs.*/
                        public void actionPerformed(ActionEvent e) {

                        }
                    });

            buttonValidate = new JButton(new ValidatePolicyAction());
                        this.add(buttonValidate);
                        buttonValidate.addActionListener(
                                new ActionListener() {
                                    /** Invoked when an action occurs.*/
                                    public void actionPerformed(ActionEvent e) {
                                        PolicyPathResult result =
                                          PolicyPathBuilder.getDefault().
                                          generate(rootAssertion.asAssertion());
                                        appendToMessageArea("Paths :"+result.getPathCount());
                                    }
                                });

            Utilities.
                    equalizeComponentSizes(
                            new JComponent[]{
                                buttonSave, buttonValidate
                            });
        }
    }

    private void appendToMessageArea(String s) {
        try {
            int pos = messagesTextPane.getText().length();
            if (pos >0) s= "\n"+s;
            StringReader sr = new StringReader(s);
            EditorKit editorKit = messagesTextPane.getEditorKit();
            editorKit.read(sr, messagesTextPane.getDocument(), pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
