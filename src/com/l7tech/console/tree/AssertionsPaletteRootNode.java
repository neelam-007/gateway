package com.l7tech.console.tree;

import com.l7tech.console.util.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * The class represents an <code>AbstractTreeNode</code> specialization
 * element that represents the assertions palette root.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AssertionsPaletteRootNode extends AbstractPaletteFolderNode {
    /**
     * construct the <CODE>AssertionsPaletteRootNode</CODE> instance
     */
    public AssertionsPaletteRootNode(String title)
      throws IllegalArgumentException {
        super(title);
        if (title == null)
            throw new IllegalArgumentException();
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        String homePath = null;
        homePath = Preferences.getPreferences().getHomePath();
        List nodeList = new ArrayList();
        nodeList.add(new AccessControlFolderNode());
        nodeList.add(new TransportLayerSecurityFolderNode());
        nodeList.add(new XmlSecurityFolderNode());
        nodeList.add(new XmlFolderNode());
        nodeList.add(new RoutingFolderNode());
        nodeList.add(new MiscFolderNode());
        nodeList.add(new AuditFolderNode());
        nodeList.add(new PolicyLogicFolderNode());
        nodeList.add(new ThreatProtectionFolderNode());
        nodeList.add(new PoliciesFolderNode(homePath));

        AbstractTreeNode[] nodes = (AbstractTreeNode[])nodeList.toArray(new AbstractTreeNode[]{});

        children = null;
        for (int i = 0; i < nodes.length; i++) {
            insert(nodes[i], i);
        }
    }

    protected String getOpenIconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }

    protected String getClosedIconResource() {
        return "com/l7tech/console/resources/policy16.gif";
    }
}
