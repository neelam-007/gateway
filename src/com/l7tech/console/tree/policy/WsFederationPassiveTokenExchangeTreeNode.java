package com.l7tech.console.tree.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import com.l7tech.console.action.EditWsFederationPassiveTokenExchangeAction;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;

/**
 * Tree node for WsFederationPassiveTokenExchange ....
 *
 * @author $Author$
 * @version: $Revision$
 */
public class WsFederationPassiveTokenExchangeTreeNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsFederationPassiveTokenExchangeTreeNode( WsFederationPassiveTokenExchange assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public Action[] getActions() {
        List actions = new ArrayList();
        actions.add(editAction);
        actions.addAll(Arrays.asList(super.getActions()));
        return (Action[])actions.toArray(new Action[0]);
    }

    public boolean canDelete() {
        return true;
    }

    public String getName() {
        String name = null;
        if((_assertion.getIpStsUrl()!=null && _assertion.getIpStsUrl().length()>0) || !_assertion.isAuthenticate()) {
            name = "Exchange credentials using WS-Federation request to " + _assertion.getIpStsUrl();
        }
        else {
            name = "Authenticate with WS-Federation protected service at " + _assertion.getReplyUrl();
        }
        return name;
    }

    //- PROTECTED

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    //- PRIVATE

    private WsFederationPassiveTokenExchange _assertion;
    private EditWsFederationPassiveTokenExchangeAction editAction = new EditWsFederationPassiveTokenExchangeAction(this);

}
