package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.email.EmailListenerAdmin;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Displays a list of folders from an IMAP account so the user can choose which folder to receive mail from.
 */
public class EmailListenerFolderList extends JDialog {
    public static final String TITLE = "Email Listener Properties";

    private JButton okButton;
    private JButton cancelButton;
    private JScrollPane scrollPane;
    private JPanel contentPane;
    private JTree folderTree;
    private DefaultTreeModel treeModel;

    private boolean confirmed = false;
    private TreePath selectedFolderPath = null;

    /**
     * Creates a new instance of EmailListenerFolderList. The selected folder is returned
     * if the user clicks on the OK button.
     *
     * @param owner The owner of this dialog window
     * @param rootFolder The nested folder structure
     */
    public EmailListenerFolderList(Dialog owner, EmailListenerAdmin.IMAPFolder rootFolder) throws HeadlessException {
        super(owner, TITLE, true);
        initialize(rootFolder);
    }

    /**
     * Creates a new instance of EmailListenerFolderList. The selected folder is returned
     * if the user clicks on the OK button.
     *
     * @param owner The owner of this dialog window
     * @param rootFolder The nested folder structure
     */
    public EmailListenerFolderList(Frame owner, EmailListenerAdmin.IMAPFolder rootFolder) throws HeadlessException {
        super(owner, TITLE, true);
        initialize(rootFolder);
    }

    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void initialize(EmailListenerAdmin.IMAPFolder rootFolder) {
        setContentPane(contentPane);
        pack();
        setModal(true);

        treeModel = new DefaultTreeModel(createTreeNode(rootFolder, null));
        treeModel.reload();
        folderTree = new JTree(treeModel);
        folderTree.setRootVisible(false);
        folderTree.setShowsRootHandles(true);
        folderTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        folderTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selectedFolderPath = e.getNewLeadSelectionPath();
            }
        });
        scrollPane.setViewportView(folderTree);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onOk();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });
        Utilities.centerOnScreen(this);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getSelectedFolderPath() {
        if(selectedFolderPath == null) {
            return null;
        } else {
            return ((IMAPFolderTreeNode)selectedFolderPath.getLastPathComponent()).path;
        }
    }

    private void onOk() {
        confirmed = true;
        setVisible(false);
    }

    private String getFolderPathForTreePath(TreePath treePath, int index, IMAPFolderTreeNode folderNode) {
        Object pc = treePath.getPathComponent(index);
        for(IMAPFolderTreeNode child : folderNode.children) {
            if(child.name.equals(treePath.getPathComponent(index))) {
                if(index == treePath.getPathCount() - 1) {
                    return child.path;
                } else {
                    return getFolderPathForTreePath(treePath, index + 1, child);
                }
            }
        }

        System.out.println("*** Returning null");
        return null;
    }

    private IMAPFolderTreeNode createTreeNode(EmailListenerAdmin.IMAPFolder folder, IMAPFolderTreeNode parent) {
        IMAPFolderTreeNode node = new IMAPFolderTreeNode(folder, parent);
        for(EmailListenerAdmin.IMAPFolder childFolder : folder.getChildren()) {
            if(childFolder.getName() != null && childFolder.getName().length() > 0) {
                node.addChild(createTreeNode(childFolder, node));
            }
        }

        return node;
    }

    private static class IMAPFolderTreeNode implements TreeNode {
        private String name;
        private String path;
        private IMAPFolderTreeNode parent;
        private java.util.List<IMAPFolderTreeNode> children = new ArrayList<IMAPFolderTreeNode>();

        public IMAPFolderTreeNode(EmailListenerAdmin.IMAPFolder folder, IMAPFolderTreeNode parent) {
            name = folder.getName();
            path = folder.getPath();
            this.parent = parent;
        }

        public Enumeration children() {
            return Collections.enumeration(children);
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public TreeNode getChildAt(int childIndex) {
            if(childIndex < 0 || childIndex >= children.size()) {
                return null;
            }
            return children.get(childIndex);
        }

        public int getChildCount() {
            return children.size();
        }

        public int getIndex(TreeNode node) {
            return children.indexOf(node);
        }

        public TreeNode getParent() {
            return parent;
        }

        public boolean isLeaf() {
            return children.size() == 0;
        }

        public void addChild(IMAPFolderTreeNode child) {
            children.add(child);
        }

        public String toString() {
            return name;
        }
    }
}
