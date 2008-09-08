package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.util.TopComponents;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 15, 2008
 * Time: 9:53:47 AM
 */
public class AlterDefaultSortAction extends BaseAction {
    static Logger log = Logger.getLogger(AlterDefaultSortAction.class.getName());

    /**
     * Default order represents the default order for the specified SortType
     * For name this means asc, for type it means desc
     */
    private boolean defaultOrder = true;
    private final SortType sortType;

    public static enum SortType {
        NAME("Name"),
        TYPE("Type"),
        ;

        private SortType(String name) {
            this.sortName = name;
        }

        private final String sortName;

        public String getName() {
            return sortName;
        }

        public String toString() {
            return sortName;
        }
    }


    private static AlterDefaultSortAction nameSort = new AlterDefaultSortAction(AlterDefaultSortAction.SortType.NAME);
    private static AlterDefaultSortAction typeSort = new AlterDefaultSortAction(AlterDefaultSortAction.SortType.TYPE);

    public static AlterDefaultSortAction getSortAction(SortType sortType){
        switch(sortType){
            case NAME:
                return nameSort;
            case TYPE:
                return typeSort;
            default:
                log.log(Level.INFO,"Unexpected SortType found");
                throw new IllegalArgumentException("Illegal SortType");
        }
    }

    /**
     * An instance of AlterDefaultSortAction maintains state regarding what the current sort is for it's type.
     * Across the SSM for each sort type we only want one instance of each sort type. As a result the constructor is
     * private. Use the getSortAction static method to get an instance
     * @param sortType
     */
    private AlterDefaultSortAction(SortType sortType) {
        super(true);
        this.sortType = sortType;
        //Calling as not using the default constructor
        setActionValues();
    }

    /**
     * @return the action name
     */
    public String getName() {

        switch(sortType){
            case NAME:
                if(defaultOrder){
                    return "Sort " + sortType+ " desc";
                }else{
                     return "Sort " + sortType+ " asc";
                }
            case TYPE:
                if(defaultOrder){
                    return "Sort " + sortType+ " asc";
                }else{
                     return "Sort " + sortType+ " desc";
                }
            default:
                log.log(Level.INFO,"Unexpected SortType found");
                throw new IllegalArgumentException("Illegal SortType");
        }
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
        return null;
    }

    /**
     * The job of performAction is to update the Comparator used by nodes in the tree
     * The comparator used by each node in this tree should be RootNode.getComparator()
     */
    protected void performAction() {
        RootNode.ServicesAndPoliciesNodeComparator comparator =  RootNode.getComparator();

        switch (sortType){
            case NAME:
                if(defaultOrder){
                    comparator.setNameAscending(false);
                    defaultOrder = false;
                }else{
                     comparator.setNameAscending(true);
                    defaultOrder = true;
                }
                break;
            case TYPE:
                if(defaultOrder){
                    comparator.setTypeDescending(false);
                    defaultOrder = false;
                }else{
                    comparator.setTypeDescending(true);
                    defaultOrder = true;
                }
                break;
            default:
                log.log(Level.INFO,"Unexpected SortType found");
                throw new IllegalArgumentException("Illegal SortType");

        }

        final JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

        TreePath rootPath = tree.getPathForRow(0);
        final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

        RootNode rootNode = (RootNode) model.getRoot();
        sortChildren(rootNode);
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

        //Update the menu item text for next time getName() is called        
        setActionValues();
    }

    private void sortChildren(AbstractTreeNode node){
        if(!(node instanceof FolderNode)){
            return;
        }
        
        List<AbstractTreeNode> childNodes = new ArrayList<AbstractTreeNode>();
        for(int i = 0; i < node.getChildCount(); i++){
            AbstractTreeNode childNode = (AbstractTreeNode)node.getChildAt(i);
            childNodes.add(childNode);            
            if(childNode instanceof FolderNode){
                sortChildren(childNode);
            }
        }

        //Detach all children
        node.removeAllChildren();
        for(AbstractTreeNode atn: childNodes){
            node.insert(atn, node.getInsertPosition(atn, RootNode.getComparator()));
        }
    }
}
