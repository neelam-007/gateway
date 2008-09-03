package com.l7tech.console.tree.servicesAndPolicies;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.EnumSet;
import java.util.Enumeration;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.NodeFilter;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.MainWindow;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 27, 2008
 * Time: 1:45:06 PM
 *
 * Filters the entities in the Services and Policies tree
 */
public class AlterFilterAction extends BaseAction {
    static Logger log = Logger.getLogger(AlterDefaultSortAction.class.getName());

    private final FilterType filterType;
    private final ServiceNodeFilter serviceNodeFilter = new ServiceNodeFilter();
    private final PolicyNodeFilter policyNodeFilter = new PolicyNodeFilter();

    public static enum FilterType {
        ALL("All"),
        SERVICES("Services"),
        POLICY_FRAGMENT("Policy Fragment"),
        ;

        private FilterType(String name) {
            this.filterName = name;
        }

        public String getName() {
            return filterName;
        }

        public String toString() {
            return filterName;
        }

        public boolean equals(FilterType obj){
            if(this.getName().equals(obj.getName()))
                return true;
            else
                return false;
        }

        public static final EnumSet<FilterType> ALL_FILTER_TYPEs = EnumSet.of(ALL, SERVICES, POLICY_FRAGMENT);
        private final String filterName;
        
    }


    public AlterFilterAction(FilterType filterType) {
        super(true);
        this.filterType = filterType;
        setActionValues();
    }

    /**
     * @return the action name
     */
    public String getName() {
        return filterType.getName();
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return getName();
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        if(filterType.equals(FilterType.SERVICES)){
            return "com/l7tech/console/resources/services16.png";    
        }else if (filterType.equals(FilterType.POLICY_FRAGMENT)){
            return "com/l7tech/console/resources/include_soap16.png";
        }

        return null;
    }

    protected void performAction() {
        final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

        if(!(model instanceof FilteredTreeModel)) return;

        FilteredTreeModel ftm = (FilteredTreeModel) model;

        final JLabel filterStatusLabel = (JLabel) TopComponents.getInstance().getComponent(MainWindow.FILTER_STATUS_LABEL);
        if(filterStatusLabel == null) return;

        NodeFilter nodeFilter = ftm.getFilter();
        //use nodeFilter below to determine if we actually need to take any action
        //if the filter is the same, do nothing
        if(filterType.equals(FilterType.SERVICES)){
            if(nodeFilter instanceof ServiceNodeFilter) return;
            ftm.setFilter(serviceNodeFilter);
            filterStatusLabel.setText(MainWindow.FILTER_STATUS_SERVICES);
        }else if (filterType.equals(FilterType.POLICY_FRAGMENT)){
            if(nodeFilter instanceof PolicyNodeFilter) return;
            ftm.setFilter(policyNodeFilter);
            filterStatusLabel.setText(MainWindow.FILTER_STATUS_POLICY_FRAGMENTS);
        }else{
            if(nodeFilter == null) return;
            ftm.setFilter(null);
            filterStatusLabel.setText(MainWindow.FILTER_STATUS_NONE);
        }

        TreePath rootPath = tree.getPathForRow(0);
        final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

        RootNode rootNode = (RootNode) model.getRoot();
        model.nodeStructureChanged(rootNode);

        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                while(pathEnum.hasMoreElements()){
                    Object pathObj = pathEnum.nextElement();
                    TreePath tp = (TreePath)pathObj;
                    tree.expandPath(tp);
                }
            }
        });
    }
}
