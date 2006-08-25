package com.l7tech.objectmodel;

import java.util.Comparator;

/**
 * Used by sorted sets containing EntityHeaders.
 * Could be extended to compare on properties other than name.
 *
 * @deprecated EntityHeader already implements comparable. there is no need for this
 * <br/><br/>
 * User: flascell<br/>
 * Date: Jul 21, 2003<br/>
 */
public class EntityHeaderComparator implements Comparator<EntityHeader> {
    public int compare(EntityHeader h1, EntityHeader h2) {
        return h1.compareTo(h2);
    }
}
