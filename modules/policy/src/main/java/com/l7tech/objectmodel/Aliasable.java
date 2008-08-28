package com.l7tech.objectmodel;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 6, 2008
 * Time: 8:56:14 AM
 *
 * Implemented by an alias which has the ability to be aliased
 * Defines the property aliases, which is every alias of an entity, which implements
 * this interface
 */
public interface Aliasable{

    public boolean isAlias();

    public void setAlias(boolean isAlias);
}