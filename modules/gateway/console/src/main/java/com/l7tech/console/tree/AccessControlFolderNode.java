package com.l7tech.console.tree;

import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;

import java.util.logging.Logger;


/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with authentication and authorization
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
        insertModularAssertionByType(WssBasic.class); // also inserts EncryptedUsernameTokenAssertion
        insertModularAssertionByType(RequireWssX509Cert.class);
        insertModularAssertionByType(RequireWssSaml2.class); //SAML2 since that is the subclass

        //Explicitly add the WsFederationPassiveTokenRequestPaletteNode instead of relying on a default palette node
        //This is done as the assertion palette node 'Use WS-Federation Credential' can result in 1 of 2 subclasses
        //of WsFederationPassiveTokenAssertion being added to the policy XML. Which subclass is chosen depends
        //on how the user configures the assertion. Simplest thing to do was to leave it as it and not use the
        //meta data for the palette folder. If the palette folder meta data is used, then this assertion will appear
        //twice as both subclasses share the same meta. To remove this issue at least the following needs to be done:
        // * modify existing property dialog / add new dialog to create only one assertion type and then modify
        // the PROPERTIES_ACTION_NAME and PROPERTIES_ACTION_CLASSNAME appropriately.
        // * Modify Advices (responsible for getting the properties dialog to show).
        // * Add the PALETTE_FOLDERS meta data to each class's meta data.
        insert(new WsFederationPassiveTokenRequestPaletteNode());
        insertMatchingModularAssertions();
        insertMatchingCustomAssertions(Category.ACCESS_CONTROL);
        insertMatchingEncapsulatedAssertions();
    }
}
