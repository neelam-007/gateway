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

    private RootNode rootNode;
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

        public static final EnumSet<SortType> ALL_SORT_TYPES = EnumSet.of(NAME, TYPE);

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


    public AlterDefaultSortAction(RootNode rootNode, SortType sortType) {
        super(true);
        this.rootNode = rootNode;
        this.sortType = sortType;
        //Calling as not using the default constructor
        setActionValues();
    }

    /**
     * @return the action name
     */
    public String getName() {
        if(sortType == SortType.NAME){
           if(defaultOrder){
               return "Sort " + sortType+ " desc";
           }else{
                return "Sort " + sortType+ " asc";               
           }
        }else if(sortType == SortType.TYPE){
            if(defaultOrder){
                return "Sort " + sortType+ " asc";
            }else{
                 return "Sort " + sortType+ " desc";
            }
        }
        log.log(Level.INFO,"Unexpected SortType found");
        throw new IllegalStateException("Unexpected SortType found");
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

        if(sortType.toString().equals(SortType.NAME.toString())){
           if(defaultOrder){
               comparator.setNameAscending(false);
               defaultOrder = false;
           }else{
                comparator.setNameAscending(true);
               defaultOrder = true;
           }
        }else if(sortType.toString().equals(SortType.TYPE.toString())){
            if(defaultOrder){
                comparator.setTypeDescending(false);
                defaultOrder = false;
            }else{
                comparator.setTypeDescending(true);
                defaultOrder = true;
            }
        }

        final JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();

        TreePath rootPath = tree.getPathForRow(0);
        final Enumeration pathEnum = tree.getExpandedDescendants(rootPath);

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
            node.insert(atn, node.getInsertPosition(atn, rootNode.getComparator()));
        }
    }
}
