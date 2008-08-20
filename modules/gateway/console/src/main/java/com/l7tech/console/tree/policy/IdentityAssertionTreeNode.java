package com.l7tech.console.tree.policy;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An assertion node in an assertion tree that refers to a user or group.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Oct 7, 2003<br/>
 * Time: 2:49:00 PM<br/>
 */
public abstract class IdentityAssertionTreeNode<AT extends IdentityAssertion> extends LeafAssertionTreeNode<AT> {

    public IdentityAssertionTreeNode(AT idass) {
        super(idass);
        assertion = idass;
    }

    protected String idProviderName() {
        if (provName == null) {
            long providerid = assertion.getIdentityProviderOid();
            IdentityProviderConfig cfg;
            try {
                if (providerid == IdentityProviderConfig.DEFAULT_OID)
                    provName = NA;
                else {
                    cfg = getIdentityAdmin().findIdentityProviderConfigByID(providerid);
                    if (cfg == null)
                        provName = NA;
                    else
                        provName = cfg.getName();
                }
            } catch (FindException e) {
                provName = NA;
                log.log(Level.SEVERE, "could not find provider associated with this assertion", e);
            } catch (Exception e) {
                provName = NA;
                log.log(Level.SEVERE, "could not loookup the IdentityAdmin", e);
            }
        }
        return provName;
    }

    private IdentityAdmin getIdentityAdmin() throws RuntimeException {
        return Registry.getDefault().getIdentityAdmin();
    }

    private static final Logger log = Logger.getLogger(IdentityAssertionTreeNode.class.getName());
    protected AT assertion;
    protected String provName = null;
    public static final String NA = "provider not available";
}
