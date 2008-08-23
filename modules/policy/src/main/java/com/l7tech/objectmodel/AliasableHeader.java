package com.l7tech.objectmodel;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 19, 2008
 * Time: 4:43:41 PM
 * Implemented by entity headers whose entities implement {@link Aliasable}
 */
public interface AliasableHeader {
    /**
     *
     * @return true if this entity header represents an alias
     */
    public boolean isAlias();

    public void setIsAlias(boolean isAlias);
}
