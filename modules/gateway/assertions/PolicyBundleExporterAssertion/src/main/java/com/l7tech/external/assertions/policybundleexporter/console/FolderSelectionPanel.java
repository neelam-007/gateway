package com.l7tech.external.assertions.policybundleexporter.console;

import com.l7tech.console.panels.EditableSearchComboBox;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class FolderSelectionPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(FolderSelectionPanel.class.getName());
    private static final Icon FOLDER_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/folder.gif"));

    @SuppressWarnings("UnusedDeclaration")
    private JPanel contentPanel;
    private EditableSearchComboBox<DefaultMutableTreeNode> folderSearchComboBox;
    private JTree foldersTree;

    public FolderSelectionPanel() {
        super();
    }

    public void populateFolders(Goid topFolderId, boolean selectTopFolder, boolean includeImmediateSubFoldersOnly) {
        Collection<FolderHeader> allFolders;
        try {
            allFolders = Registry.getDefault().getFolderAdmin().findAllFolders();
        } catch (final FindException e) {
            logger.warning("Error");
            return;
        }

        FolderHeader topFolder = null;
        for (FolderHeader folder : allFolders) {
            if (Goid.equals(topFolderId, folder.getGoid())) {
                topFolder = folder;
                break;
            }
        }

        if (topFolder == null) {
            logger.warning("Error");
            return;
        }

        DefaultMutableTreeNode topNode = new DefaultMutableTreeNode(topFolder);
        DefaultTreeModel treeModel = new DefaultTreeModel(topNode);
        foldersTree.setModel(treeModel);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setClosedIcon(FOLDER_ICON);
        renderer.setOpenIcon(FOLDER_ICON);
        renderer.setLeafIcon(FOLDER_ICON);
        foldersTree.setCellRenderer(renderer);

        foldersTree.setRootVisible(true);
        foldersTree.setShowsRootHandles(true);
        foldersTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Populate folders tree
        //
        for (FolderHeader folder : allFolders) {
            if(folder.getParentFolderGoid() != null && Goid.equals(topFolderId, folder.getParentFolderGoid())) {
                DefaultMutableTreeNode childNode = createFolderNode(folder, allFolders, includeImmediateSubFoldersOnly);
                insertNode(topNode, childNode);
            }
        }

        foldersTree.expandPath(new TreePath(topNode.getPath()));

        if (selectTopFolder) {
            setSelectedFolder(topFolder);
        }

        // Configure search combo box.
        //
        List<DefaultMutableTreeNode> searchableItems = new LinkedList<>();
        Enumeration e = topNode.preorderEnumeration();
        while (e.hasMoreElements()) {
            searchableItems.add((DefaultMutableTreeNode) e.nextElement());
        }
        folderSearchComboBox.updateSearchableItems(searchableItems);
    }

    public void setSelectedFolder(FolderHeader folder) {
        DefaultTreeModel treeModel = (DefaultTreeModel) foldersTree.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();

        Enumeration e = rootNode.preorderEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            FolderHeader currentFolder = (FolderHeader) node.getUserObject();
            if (Goid.equals(folder.getGoid(), currentFolder.getGoid())) {
                TreePath treePath = new TreePath(node.getPath());
                foldersTree.setSelectionPath(treePath);
                foldersTree.scrollPathToVisible(treePath);
                break;
            }
        }
    }

    public FolderHeader getSelectedFolder() {
        Object comp = foldersTree.getLastSelectedPathComponent();
        if (comp == null) {
            return null;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) comp;
        return (FolderHeader) node.getUserObject();
    }

    // if invalid, returns error string. otherwise, returns empty string.
    public String isFolderSelected() {
        if (foldersTree.getLastSelectedPathComponent() == null) {
            return "A Folder must be selected.\n";
        }

        return "";
    }

    private DefaultMutableTreeNode createFolderNode(FolderHeader folder, Collection<FolderHeader> allFolders, boolean includeImmediateSubFoldersOnly) {
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder);

        for (FolderHeader childFolder : allFolders) {
            if(childFolder.getParentFolderGoid() != null && Goid.equals(folder.getGoid(), childFolder.getParentFolderGoid()) ) {
                if (!includeImmediateSubFoldersOnly) {
                    DefaultMutableTreeNode childNode = createFolderNode(childFolder, allFolders, false);
                    insertNode(folderNode, childNode);
                }
            }
        }

        return folderNode;
    }

    private void createUIComponents() {
        // Create and initialize folder search combo box.
        //
        EditableSearchComboBox.Filter filter = new EditableSearchComboBox.Filter() {
            @Override
            public boolean accept(Object obj) {
                if (obj == null) {
                    return false;
                }

                if (!(obj instanceof DefaultMutableTreeNode)) {
                    return false;
                }

                String filterText = this.getFilterText().toLowerCase();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
                FolderHeader folder  = (FolderHeader) node.getUserObject();
                return folder.getPath().toLowerCase().contains(filterText);
            }
        };

        folderSearchComboBox = new EditableSearchComboBox<DefaultMutableTreeNode>(filter) {};

        // Create a renderer and configure it to clip. Text which is too large will automatically get '...' added to it
        // and the jlabel will not grow to accommodate it, if it is larger than the size of the combo box component
        //
        Functions.Unary<String, DefaultMutableTreeNode> accessorFunction = new Functions.Unary<String, DefaultMutableTreeNode>() {
            @Override
            public String call(DefaultMutableTreeNode treeNode) {
                FolderHeader folder = (FolderHeader) treeNode.getUserObject();
                return folder.getPath();
            }
        };

        TextListCellRenderer<DefaultMutableTreeNode> renderer = new TextListCellRenderer<>(accessorFunction);
        renderer.setRenderClipped(true);
        //noinspection unchecked
        folderSearchComboBox.setRenderer(renderer);

        folderSearchComboBox.setComparator(new Comparator<DefaultMutableTreeNode>() {
            @Override
            public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
                return ((FolderHeader) o1.getUserObject()).getPath().compareTo(((FolderHeader) o2.getUserObject()).getPath());
            }
        });

        folderSearchComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) folderSearchComboBox.getSelectedItem();
                if (node == null) {
                    return;
                }

                TreePath treePath = new TreePath(node.getPath());
                foldersTree.setSelectionPath(treePath);
                foldersTree.scrollPathToVisible(treePath);
            }
        });
    }

    private static void insertNode(DefaultMutableTreeNode targetNode, DefaultMutableTreeNode insertNode) {
        if (targetNode.getChildCount() == 0) {
            targetNode.add(insertNode);
        }

        FolderHeader insertFolder = (FolderHeader) insertNode.getUserObject();
        for (int index = 0; index < targetNode.getChildCount(); index++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) targetNode.getChildAt(index);
            FolderHeader folder = (FolderHeader) node.getUserObject();
            if (insertFolder.getName().compareTo(folder.getName()) <= 0) {
                targetNode.insert(insertNode, index);
                return;
            }
        }

        targetNode.add(insertNode);
    }
}