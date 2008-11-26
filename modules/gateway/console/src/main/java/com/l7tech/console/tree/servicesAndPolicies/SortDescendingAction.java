package com.l7tech.console.tree.servicesAndPolicies;

/**
 * Descending action class
 *
 * User: dlee
 * Date: Nov 24, 2008
 */
public class SortDescendingAction extends AlterDefaultSortAction {

    public SortDescendingAction(SortType sortType) {
        super(sortType);
    }

    @Override
    public String getName() {
        return "Descending"; 
    }

    @Override
    public boolean isAscending() {

        return false;
    }
}
