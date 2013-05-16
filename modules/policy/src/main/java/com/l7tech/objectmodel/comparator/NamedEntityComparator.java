package com.l7tech.objectmodel.comparator;

import com.l7tech.objectmodel.NamedEntity;

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
        } else if (ne1 == null) {
            c = -1;
        } else if (ne2 == null) {
            c = 1;
        } else {
            if (ne1.getName() == null && ne2.getName() == null) {
                c = 0;
            } else if (ne1.getName() == null) {
                c = -1;
            } else if (ne2.getName() == null) {
                c = 1;
            } else {
                return ne1.getName().compareToIgnoreCase(ne2.getName());
            }
        }
        return c;
    }
}
