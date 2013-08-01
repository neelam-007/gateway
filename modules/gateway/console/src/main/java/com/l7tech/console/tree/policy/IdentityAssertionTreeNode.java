package com.l7tech.console.tree.policy;

import com.l7tech.console.action.AddIdentityAssertionAction;
import com.l7tech.console.util.Registry;
import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.console.action.SelectIdentityTagAction;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

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
    }

    protected String decorateName( final String name ) {
        return AssertionUtils.decorateName(assertion, name);
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
            } catch (final PermissionDeniedException e) {
                provName = NA;
                log.log(Level.WARNING, "User does not have permission to look up the IdentityAdmin", ExceptionUtils.getDebugException(e));
            } catch (Exception e) {
                provName = NA;
                log.log(Level.SEVERE, "could not lookup the IdentityAdmin", e);
            }
        }
        return provName;
    }

    public void clearCache(){
        provName = null;    
    }

    @Override
    public Action[] getActions() {
        List<Action> actions = new ArrayList<Action>( Arrays.asList(super.getActions()) );

        int insertPosition = 1;
        if ( getPreferredAction()==null ) {
            insertPosition = 0;
        }

        actions.add( insertPosition++, new SelectMessageTargetAction(this));
        actions.add( insertPosition, new SelectIdentityTagAction(this));

        return actions.toArray(new Action[actions.size()]);
    }

    private IdentityAdmin getIdentityAdmin() throws RuntimeException {
        return Registry.getDefault().getIdentityAdmin();
    }

    @Override
    public Action getPreferredAction() {
        return new AddIdentityAssertionAction(this);
    }

    private static final Logger log = Logger.getLogger(IdentityAssertionTreeNode.class.getName());
    protected String provName = null;
    public static final String NA = "provider not available";
}
