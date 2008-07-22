package com.l7tech.console.action;

import com.l7tech.console.tree.UserNode;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedUserPropertiesAction extends UserPropertiesAction {

    public FederatedUserPropertiesAction(UserNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Federated User Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Federated User Properties";
    }

}

