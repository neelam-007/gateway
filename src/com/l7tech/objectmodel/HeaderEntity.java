package com.l7tech.objectmodel;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 9, 2003
 *
 * Header objects are used to refer to objects in find methods
 * of all managers
 */
public interface HeaderEntity extends NamedEntity{
    public Class getType();
    public void setType(Class type);
}
