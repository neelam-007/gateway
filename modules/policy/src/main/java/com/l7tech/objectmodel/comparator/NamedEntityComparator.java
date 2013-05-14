package com.l7tech.objectmodel.comparator;

import com.l7tech.objectmodel.NamedEntity;
import org.apache.commons.collections.ComparatorUtils;

import java.util.Comparator;

/**
 * Comparator for NamedEntity which considers null to be less than non-null.
 */
public class NamedEntityComparator implements Comparator<NamedEntity> {
    @Override
    public int compare(final NamedEntity ne1, final NamedEntity ne2) {
        int c;
        if (ne1 == null && ne2 == null) {
            c = 0;
        } else if (ne1 == null && ne2 != null) {
            c = -1;
        } else if (ne1 != null && ne2 == null) {
            c = 1;
        } else {
            c = ComparatorUtils.nullLowComparator(ComparatorUtils.naturalComparator()).compare(ne1.getName(), ne2.getName());
        }
        return c;
    }
}
