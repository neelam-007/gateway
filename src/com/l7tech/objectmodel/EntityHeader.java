/*
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 * @version $Revision$
 * @author flascelles
 */
public interface EntityHeader extends NamedEntity {
    public Class getType();
    public void setType(Class type);
}
