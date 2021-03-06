package com.l7tech.console.tree;

import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.ext.Category;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * Keeps track of which palette folders are available within the SSM.
 */
public class PaletteFolderRegistry {
    private final LinkedHashMap<String, AbstractPaletteFolderNode> foldersById = new LinkedHashMap<String, AbstractPaletteFolderNode>();

    public PaletteFolderRegistry() {
    }

    /**
     * @return a list of available palette folder IDs on the current SSM.
     */
    public List<String> getPaletteFolderIds() {
        return new ArrayList<String>(foldersById.keySet());
    }

    /**
     * @return a list of available palette folder IDs that can contain assertions.
     */
    public List<String> getAssertionPaletteFolderIds() {
        final List<String> assertionFolderIds = new ArrayList<String>();
        for (final Map.Entry<String, AbstractPaletteFolderNode> entry : foldersById.entrySet()) {
            if (entry.getValue() instanceof DefaultAssertionPaletteFolderNode) {
                assertionFolderIds.add(entry.getKey());
            }
        }
        return assertionFolderIds;
    }

    /**
     * Look up a palette folder display name from its ID.
     *
     * @param id id to look up, eg "xmlSecurity".
     * @return the display name for the specified ID, eg "XML Security", or null if it wasn't recognized.
     */
    public String getPaletteFolderName(String id) {
        AbstractPaletteFolderNode got = foldersById.get(id);
        return got == null ? null : got.getName();
    }

    /**
     * Re-load palette folders.
     */
    public void refreshPaletteFolders() {
        foldersById.clear();
        List<AbstractPaletteFolderNode> folders = createPaletteFolderNodes();
        for (AbstractPaletteFolderNode folder : folders) {
            foldersById.put(folder.getFolderId(), folder);
        }
        notifyPaletteFoldersChanged();
    }

    private void notifyPaletteFoldersChanged() {
        // TODO replace with proper event publish/subscribe mechanism.  For now we will just hardcode what
        // the listener would have done in response
        reloadPaletteRootNode();
    }

    private void reloadPaletteRootNode() {
        JTree tree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree == null || tree.getModel() == null)
            return;
        AbstractTreeNode root = (AbstractTreeNode) tree.getModel().getRoot();

        // Find which ones were open before
        Set<String> opened = findOpenedPaletteFolderIds(tree);

        root.removeAllChildren();
        root.reloadChildren();

        ((DefaultTreeModel) (tree.getModel())).nodeStructureChanged(root);

        reopenOpenedPaletteFolders(tree, opened);

        tree.validate();
        tree.repaint();

    }

    private Set<String> findOpenedPaletteFolderIds(JTree tree) {
        Set<String> ret = new HashSet<String>();

        int rowCount = tree.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            TreePath path = tree.getPathForRow(i);
            Object obj = path.getLastPathComponent();
            if (obj instanceof AbstractPaletteFolderNode) {
                AbstractPaletteFolderNode node = (AbstractPaletteFolderNode) obj;
                if (tree.isExpanded(path))
                    ret.add(node.getFolderId());
            }
        }

        return ret;
    }

    private void reopenOpenedPaletteFolders(JTree tree, Set<String> folderIdsToExpand) {
        Set<String> idsToExpand = new HashSet<String>(folderIdsToExpand);

        boolean expandedSomething;
        do {
            expandedSomething = false;
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                Object obj = path.getLastPathComponent();
                if (obj instanceof AbstractPaletteFolderNode) {
                    AbstractPaletteFolderNode node = (AbstractPaletteFolderNode) obj;
                    final String folderId = node.getFolderId();
                    if (idsToExpand.contains(folderId)) {
                        idsToExpand.remove(folderId);
                        expandedSomething = true;
                        tree.expandPath(path);
                        break;
                    }
                }
            }
        } while (expandedSomething);
    }

    /**
     * Create a new set of (non-root) assertion palette folder nodes, based on the current configuration.
     *
     * @return an ordered list of new AbstractPaletteFolderNode instances representing the palette folders
     *         that should exist under the root folder.
     */
    public List<AbstractPaletteFolderNode> createPaletteFolderNodes() {
        List<AbstractPaletteFolderNode> nodeList = new LinkedList<AbstractPaletteFolderNode>();

        nodeList.add(new DefaultAssertionPaletteFolderNode("Access Control", "accessControl", Category.ACCESS_CONTROL));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Transport Layer Security (TLS)", "transportLayerSecurity", Category.TRANSPORT_SEC));
        nodeList.add(new DefaultAssertionPaletteFolderNode("XML Security", "xmlSecurity", Category.XML_SEC));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Message Validation/Transformation", "xml", Category.MESSAGE, Category.MSG_VAL_XSLT));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Message Routing", "routing", Category.ROUTING));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Service Availability", "misc", Category.AVAILABILITY));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Logging, Auditing and Alerts", "audit", Category.AUDIT_ALERT));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Policy Logic", "policyLogic", Category.LOGIC));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Threat Protection", "threatProtection", Category.THREAT_PROT));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Internal Assertions", "internalAssertions"));
        nodeList.add(new DefaultAssertionPaletteFolderNode("Custom Assertions", "customAssertions", Category.CUSTOM_ASSERTIONS));

        for (Iterator i = nodeList.iterator(); i.hasNext();) {
            AbstractPaletteFolderNode node = (AbstractPaletteFolderNode)i.next();
            if (!node.isEnabledByLicense() || (node.getChildCount() < 1 && node.isHiddenIfNoChildren())) {
                i.remove();
            }
        }

        // include the policy templates even if empty
        if (!TopComponents.getInstance().isApplet())
            nodeList.add(new PolicyTemplatesFolderNode());

        // TODO mix in any user-created palette folders, when supported

        return nodeList;
    }
}
