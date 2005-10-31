package com.l7tech.console.tree.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import com.l7tech.console.action.EditWsFederationPassiveTokenRequestAction;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;

/**
 * Tree node for WsFederationPassiveTokenRequest ....
 *
 * @author $Author$
 * @version: $Revision$
 */
public class WsFederationPassiveTokenRequestTreeNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenRequest assertion ) {
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
        return "Obtain credentials using WS-Federation request to " + _assertion.getIpStsUrl();
    }

    //- PROTECTED

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    //- PRIVATE

    private WsFederationPassiveTokenRequest _assertion;
    private EditWsFederationPassiveTokenRequestAction editAction = new EditWsFederationPassiveTokenRequestAction(this);

}
