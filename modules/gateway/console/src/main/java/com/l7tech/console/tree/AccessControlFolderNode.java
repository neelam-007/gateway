package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
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
        children = null;
        insert(new IdentityNode());
        insert(new SslTransportNode(true));
        insertModularAssertionByType(WssBasic.class);
        insertModularAssertionByType(RequireWssX509Cert.class);
        insertModularAssertionByType(RequireWssSaml2.class); //SAML2 since that is the subclass

        insert(new WsFederationPassiveTokenRequestPaletteNode());
        insertMatchingModularAssertions();
        insertMatchingCustomAssertions(Category.ACCESS_CONTROL);
    }
}
