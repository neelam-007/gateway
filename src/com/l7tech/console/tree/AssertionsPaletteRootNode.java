package com.l7tech.console.tree;

import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;
import com.l7tech.console.logging.ErrorManager;

import java.io.IOException;
import java.util.logging.Level;

/**
 * The class represents an <code>AbstractTreeNode</code>
 * node element that represents the assertions palette root.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AssertionsPaletteRootNode extends AbstractTreeNode {
    /**
     * construct the <CODE>AssertionsPaletteRootNode</CODE> instance
     */
    public AssertionsPaletteRootNode(String title)
      throws IllegalArgumentException {
        super(null);
        if (title == null)
            throw new IllegalArgumentException();
        label = title;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        Registry r = Registry.getDefault();
        long providerId =
          r.getInternalProvider().getConfig().getOid();

        String homePath = null;
        try {
            homePath = Preferences.getPreferences().getHomePath();
        } catch (IOException e) {
            // something happened to the preferences home path
            ErrorManager.
                      getDefault().
                      notify(Level.WARNING, e, "There was an error in retreiving preferences.");
            homePath = System.getProperty("user.home");
        }
        AbstractTreeNode[] nodes =
            new AbstractTreeNode[]{
                new UserFolderNode(r.getInternalUserManager(), providerId),
                new GroupFolderNode(r.getInternalGroupManager(), providerId),
                new ProvidersFolderNode(r.getProviderConfigManager()),
                new PoliciesFolderNode(homePath),
                new AuthMethodFolderNode(),
                new TransportLayerSecurityFolderNode(),
                new XmlSecurityFolderNode(),
                new RoutingFolderNode()
            };

        for (int i = 0; i < nodes.length; i++) {
            insert(nodes[i], i);
        }
    }

    /**
     *
     * @return the root name
     */
    public String getName() {
        return label;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    private String label;
}
