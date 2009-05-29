package com.l7tech.console.tree;

import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.ext.Category;

import java.util.Iterator;
import java.util.LinkedList;
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
        super(title, "_root");
        if (title == null)
            throw new IllegalArgumentException();
    }

    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        // We don't allow modular assertions to invite themselves into this folder, so we don't call
        // insertMatchingModularAssertions here.

        List<AbstractPaletteFolderNode> nodeList = new LinkedList<AbstractPaletteFolderNode>();
        nodeList.add(new AccessControlFolderNode());
        nodeList.add(new TransportLayerSecurityFolderNode());
        nodeList.add(new DefaultAssertionPaletteFolderNode("XML Security", "xmlSecurity", Category.XML_SEC));
        nodeList.add(new XmlFolderNode());
        nodeList.add(new RoutingFolderNode());
        nodeList.add(new MiscFolderNode());
        nodeList.add(new AuditFolderNode());
        nodeList.add(new PolicyLogicFolderNode());
        nodeList.add(new ThreatProtectionFolderNode());

        for (Iterator i = nodeList.iterator(); i.hasNext();) {
            AbstractPaletteFolderNode node = (AbstractPaletteFolderNode)i.next();
            if (!node.isEnabledByLicense() || (node.getChildCount() < 1 && node.isHiddenIfNoChildren())) {
                i.remove();
            }
        }

        // include the policy templates even if empty
        if (!TopComponents.getInstance().isApplet())
            nodeList.add(new PolicyTemplatesFolderNode());

        AbstractTreeNode[] nodes = nodeList.toArray(new AbstractTreeNode[]{});

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
