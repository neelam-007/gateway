package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 7, 2003
 * Time: 2:49:00 PM
 * $Id$
 *
 *
 */
public abstract class IdentityAssertionTreeNode extends LeafAssertionTreeNode {
    public IdentityAssertionTreeNode(IdentityAssertion idass) {
        super(idass);
        assertion = idass;
    }

    protected String idProviderName() {
        if (provName == null) {
            long providerid = assertion.getIdentityProviderOid();
            IdentityProviderConfig cfg = null;
            try {
                cfg = getProviderConfigManager().findByPrimaryKey(providerid);
                provName = cfg.getName();
            } catch (FindException e) {
                provName = "provider not available";
                log.log(Level.SEVERE, "could not find provider associated with this assertion", e);
            } catch (RuntimeException e) {
                provName = "provider not available";
                log.log(Level.SEVERE, "could not loookup the IdentityProviderConfigManager", e);
            }
        }
        return provName;
    }

    private IdentityProviderConfigManager getProviderConfigManager() throws RuntimeException {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

    private static final Logger log = Logger.getLogger(LeafAssertionTreeNode .class.getName());
    private IdentityAssertion assertion;
    private String provName = null;
}
