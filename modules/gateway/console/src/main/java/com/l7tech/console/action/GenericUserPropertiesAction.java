package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.UserNode;

/**
 * The <code>GenericUserPropertiesAction</code> edits the user entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class GenericUserPropertiesAction extends UserPropertiesAction {

    public GenericUserPropertiesAction(UserNode node) {
        super(node);
    }

    /**
      * @return the action name
      */
     public String getName() {
         return "Internal User Properties";
     }

}
