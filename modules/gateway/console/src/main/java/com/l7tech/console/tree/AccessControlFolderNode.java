package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml2;
import com.l7tech.policy.assertion.credential.wss.WssBasic;

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
    @Override
    protected void doLoadChildren() {
        int index = 0;
        children = null;
        insert(new IdentityNode(), index++);
        insert(new HttpBasicAuthNode(), index++);
        insert(new HttpDigestAuthNode(), index++);
        insert(new CookieCredentialSourceAssertionPaletteNode(), index++);
        insert(new HttpNegotiateAuthNode(), index++);
        insert(new SslTransportNode(true), index++);
        index = insertModularAssertionByType(index, WssBasic.class);
        index = insertModularAssertionByType(index, RequestWssX509Cert.class);
        index = insertModularAssertionByType(index, RequestWssSaml2.class); //SAML2 since that is the subclass
        insert(new WsTrustCredentialExchangePaletteNode(), index++);
        insert(new WsFederationPassiveTokenRequestPaletteNode(), index++);
        insert(new XpathCredentialSourcePaletteNode(), index++);
        insert(new SamlBrowserArtifactPaletteNode(), index++);
        index = insertMatchingModularAssertions(index);
        insertMatchingCustomAssertions(index, Category.ACCESS_CONTROL);        
    }
}
