package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;

import java.util.Collections;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the published service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class ServiceNode extends EntityHeaderNode {
    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e  the EntityHeader instance, must represent published service
     * @exception IllegalArgumentException
     *                   thrown if unexpected type
     */
    public ServiceNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }

}
