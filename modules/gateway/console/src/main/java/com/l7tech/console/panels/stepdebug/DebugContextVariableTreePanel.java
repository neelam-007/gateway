package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A panel that contains context variables tree, user context variable combo box, and add button.
 */
public class DebugContextVariableTreePanel extends JPanel {
    private static final Icon DELETE_USER_CONTEXT_VARIABLE_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/delete.gif"));

    private static final Set<String> sortedBuiltInVars;
    static {
        sortedBuiltInVars = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        @SuppressWarnings("unchecked")
        Map<String,VariableMetadata> builtInVars = BuiltinVariables.getAllMetadata();
        for (VariableMetadata meta : builtInVars.values()) {
            if (meta.isPrefixed()) {
                sortedBuiltInVars.add(meta.getName()+".<suffix>");
            } else {
                sortedBuiltInVars.add(meta.getName());
            }
        }
    }

    @SuppressWarnings("unused")
    private JPanel mainForm;
    private JTree contextVariablesTree;
    private JComboBox<String> contextVariableComboBox;
    private JButton addContextVariableButton;

    private final PolicyStepDebugDialog policyStepDebugDialog;

    private PopUpMouseListener contextVariablesTreePopUpMenuListener = new PopUpMouseListener() {
        @Override
        protected void popUpMenuHandler(MouseEvent mouseEvent) {
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                int row = contextVariablesTree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (row == -1) {
                    return;
                }

                contextVariablesTree.setSelectionRow(row);
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) contextVariablesTree.getLastSelectedPathComponent();
                int length = treeNode.getPath().length;
                if (length == 2) {
                    // Display "Delete" option only when the parent node of the context variable is selected.
                    //
                    DebugContextVariableData data = (DebugContextVariableData) treeNode.getUserObject();
                    if (data.getIsUserAdded()) {
                        JPopupMenu menu = new JPopupMenu();
                        menu.add(deleteContextVariableMenuAction);
                        menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    };

    private Action deleteContextVariableMenuAction = new AbstractAction("Delete", DELETE_USER_CONTEXT_VARIABLE_ICON) {
        @Override
        public void actionPerformed(ActionEvent e) {
            onRemoveUserContextVariable();
        }
    };

    /**
     * Creates <code>DebugContextVariableTreePanel</code>.
     *
     * @param policyStepDebugDialog the policy step debug dialog
     */
    DebugContextVariableTreePanel(@NotNull PolicyStepDebugDialog policyStepDebugDialog) {
        super();
        this.policyStepDebugDialog = policyStepDebugDialog;
        this.initialize();
    }

    /**
     * Updates the panel.
     *
     * @param contextVariables the context variables that are currently set
     */
    void update(@NotNull Set<DebugContextVariableData> contextVariables) {
        DefaultTreeModel model = (DefaultTreeModel) this.contextVariablesTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

        // Find all nodes that are currently expanded.
        //
        Set<DebugContextVariableData> expanded = new TreeSet<>();
        Enumeration e = root.preorderEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.isRoot()) {
                continue;
            }

            if (this.contextVariablesTree.isExpanded(new TreePath(node.getPath()))) {
                expanded.add((DebugContextVariableData) node.getUserObject());
            }
        }

        // Find currently selected node.
        //
        DebugContextVariableData selected = null;
        Object comp = this.contextVariablesTree.getLastSelectedPathComponent();
        if (comp != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) comp;
            selected = (DebugContextVariableData) node.getUserObject();
        }

        // Update tree
        //
        root.removeAllChildren();
        for (DebugContextVariableData contextVariable : contextVariables) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(contextVariable);
            this.addChildrenContextVariables(node, contextVariable);
            root.add(node);
        }
        model.reload(root);

        // Expanded nodes and set selected node.
        //
        e = root.preorderEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if (node.isRoot()) {
                continue;
            }

            DebugContextVariableData data = (DebugContextVariableData) node.getUserObject();
            if (expanded.contains(data)) {
                contextVariablesTree.expandPath(new TreePath(node.getPath()));
            }

            if (selected != null && selected.compareTo(data) == 0) {
                contextVariablesTree.setSelectionPath(new TreePath(node.getPath()));
            }
        }
    }

    /**
     * Disable UI components.
     */
    void disableComponents() {
        contextVariablesTree.setEnabled(false);
        contextVariableComboBox.setEnabled(false);
        addContextVariableButton.setEnabled(false);
    }

    private void initialize() {
        contextVariablesTree.setCellRenderer(new DebugContextVariableTreeCellRenderer());
        contextVariablesTree.setRootVisible(false);
        contextVariablesTree.setShowsRootHandles(true);
        contextVariablesTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        contextVariablesTree.addMouseListener(contextVariablesTreePopUpMenuListener);
        // Remove key binding for F2 key in JTree.
        contextVariablesTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "none");

        contextVariableComboBox.setModel(new DefaultComboBoxModel<>(new String[]{}));
        contextVariableComboBox.setEditable(true);
        contextVariableComboBox.getEditor().getEditorComponent().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Do nothing.
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Do nothing.
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    onAddUserContextVariable();
                }
            }
        });

        for (String var : sortedBuiltInVars) {
            contextVariableComboBox.addItem(var);
        }

        addContextVariableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAddUserContextVariable();
            }
        });
    }

    private void onAddUserContextVariable() {
        String name = ((String) contextVariableComboBox.getSelectedItem());
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "The Name field must not be empty.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } else {
            policyStepDebugDialog.addUserContextVariable(name.trim());
        }
    }


    private void addChildrenContextVariables(DefaultMutableTreeNode node, DebugContextVariableData contextVariable) {
        for (DebugContextVariableData child : contextVariable.getChildren()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            this.addChildrenContextVariables(childNode, child);
            node.add(childNode);
        }
    }

    private void onRemoveUserContextVariable() {
        Object comp = contextVariablesTree.getLastSelectedPathComponent();
        if (comp == null) {
            JOptionPane.showMessageDialog(this,
                "Context variable is not selected.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        } else {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) comp;
            DebugContextVariableData data = (DebugContextVariableData) treeNode.getUserObject();
            policyStepDebugDialog.removeUserContextVariable(data.getName());
        }
    }
}