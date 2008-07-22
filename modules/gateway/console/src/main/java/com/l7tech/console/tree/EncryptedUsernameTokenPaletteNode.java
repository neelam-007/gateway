package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;


/**
 * Represents the palette node for the EncryptedUsernameToken assertion.
 */
public class EncryptedUsernameTokenPaletteNode extends AbstractLeafPaletteNode {
    public EncryptedUsernameTokenPaletteNode() {
        super("Encrypted UsernameToken", "com/l7tech/console/resources/authentication.gif");
    }

    public Assertion asAssertion() {
        return new EncryptedUsernameTokenAssertion();
    }
}
