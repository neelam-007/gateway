package com.l7tech.objectmodel;

import java.util.Comparator;

/**
 * Used by sorted sets containing EntityHeaders.
 * Could be extended to compare on properties other than name.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Jul 21, 2003<br/>
 */
public class EntityHeaderComparator implements Comparator {
    public int compare(Object o1, Object o2) {
        EntityHeader h1 = (EntityHeader)o1;
        EntityHeader h2 = (EntityHeader)o2;
        if (h1.getName() == null && h2.getName() == null) {
            return h1.getType().getVal() - h2.getType().getVal();
        }
        else if (h1.getName() == null) return 1;
        else if (h2.getName() == null) return -1;
        int res = h1.getName().compareToIgnoreCase(h2.getName());
        if (res == 0) {
            res = h1.getName().compareTo(h2.getName());
        }
        if (res == 0) {
            return h1.getType().getVal() - h2.getType().getVal();
        }
        else return res;
    }
}
