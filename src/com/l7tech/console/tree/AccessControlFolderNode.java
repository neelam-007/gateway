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
 * It represents the folder with authenticaiton and authorization
 * access control.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AccessControlFolderNode extends AbstractPaletteFolderNode {
    static final Logger log = Logger.getLogger(AccessControlFolderNode.class.getName());

    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     */
    public AccessControlFolderNode() {
        super("Access Control");
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
        insert(new HttpNegotiateAuthNode(), index++);
        insert(new SslTransportNode(true), index++);
        insert(new WsTokenBasicAuthNode(), index++);
        insert(new EncryptedUsernameTokenPaletteNode(), index++);
        insert(new RequestWssX509Node(), index++);
        insert(new SecureConversationNode(), index++);
        insert(new RequestWssSamlNode(), index++);
        insert(new WsTrustCredentialExchangePaletteNode(), index++);
        insert(new WsFederationPassiveTokenRequestPaletteNode(), index++);
        insert(new WsFederationPassiveTokenExchangePaletteNode(), index++);
        insert(new XpathCredentialSourcePaletteNode(), index++);
        insert(new SamlBrowserArtifactPaletteNode(), index++);
        insert(new KerberosPaletteNode(), index++);
        insert(new MappingAssertionPaletteNode(), index++);

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

    protected String getOpenIconResource() {
        return "com/l7tech/console/resources/folderOpen.gif";
    }

    protected String getClosedIconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

}
