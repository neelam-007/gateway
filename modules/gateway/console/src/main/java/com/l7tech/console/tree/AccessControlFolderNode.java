package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

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
        super("Access Control", "accessControl");
    }

    /**
     * subclasses override this method
     */
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert(new IdentityNode(), index++);
        insert(new HttpBasicAuthNode(), index++);
        insert(new HttpDigestAuthNode(), index++);
        insert(new CookieCredentialSourceAssertionPaletteNode(), index++);
        insert(new HttpNegotiateAuthNode(), index++);
        insert(new SslTransportNode(true), index++);
        insert(new WsTokenBasicAuthNode(), index++);
        insert(new EncryptedUsernameTokenPaletteNode(), index++);
        insert(new RequestWssX509Node(), index++);
        insert(new SecureConversationNode(), index++);
        insert(new RequestWssSamlNode(), index++);
        insert(new WsTrustCredentialExchangePaletteNode(), index++);
        insert(new WsFederationPassiveTokenRequestPaletteNode(), index++);
        insert(new XpathCredentialSourcePaletteNode(), index++);
        insert(new SamlBrowserArtifactPaletteNode(), index++);
        insert(new KerberosPaletteNode(), index++);
//        insert(new MappingAssertionPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.ACCESS_CONTROL);        
    }
}
