package com.l7tech.console.tree;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with transport layer securitry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AuthenticationFolderNode extends AbstractTreeNode {
    static final Logger log = Logger.getLogger(AuthenticationFolderNode.class.getName());

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     */
    public AuthenticationFolderNode() {
        super(null);
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
        int index = 0;
        children = null;
        insert(new IdentityNode(), index++);
        insert(new HttpBasicAuthNode(), index++);
        insert(new HttpDigestAuthNode(), index++);
        insert(new HttpClientCertificateAuthNode(), index++);
        insert(new WsTokenBasicAuthNode(), index++);
        // insert(new WsTokenDigestAuthNode(), index++);
        //insert(new XmlRequestSecurityNode("XML Digital Signature"), index++);
        insert(new RequestWssX509Node(), index++);

        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        try {
            Iterator it = cr.getAssertions(Category.ACCESS_CONTROL).iterator();
            while (it.hasNext()) {
                CustomAssertionHolder a = (CustomAssertionHolder)it.next();
                insert(new CustomAccessControlNode(a), index++);
            }
        } catch (RemoteException e1) {
            log.log(Level.WARNING, "Unable to retrieve custom assertions", e1);
        }

    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "Authentication";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

}
