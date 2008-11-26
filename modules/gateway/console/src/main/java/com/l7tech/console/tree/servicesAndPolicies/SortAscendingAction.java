package com.l7tech.console.tree.servicesAndPolicies;

/**
 * Ascending action class
 * 
 * User: dlee
 * Date: Nov 24, 2008
 */
public class SortAscendingAction extends AlterDefaultSortAction {

    public SortAscendingAction(SortType sortType) {
        super(sortType);
    }

    @Override
    public String getName() {
        return "Ascending";   
    }

    @Override
    public boolean isAscending() {
        return true;
    }
}
