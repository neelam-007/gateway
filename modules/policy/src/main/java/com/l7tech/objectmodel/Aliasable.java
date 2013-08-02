package com.l7tech.objectmodel;

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

    public void setAliasGoid(Goid aliasGoid);
}