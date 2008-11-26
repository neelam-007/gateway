package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.action.BaseAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.security.LogonListener;
import com.l7tech.gateway.common.audit.LogonEvent;

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
public class AlterDefaultSortAction extends BaseAction implements LogonListener {
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

    /**
     * Intialize based on the sort type.
     * @param sortType
     */
    public AlterDefaultSortAction(SortType sortType) {
        super(true);
        this.sortType = sortType;
        //Calling as not using the default constructor
        setActionValues();
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "";  //must be overridden by subclass, would prefer to be abstract
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
                if (isAscending() == comparator.isNameAscending()) return;
                comparator.setNameAscending(isAscending());
                break;
            case TYPE:
                if (!isAscending() == comparator.isTypeDescending()) return;
                comparator.setTypeDescending(!isAscending());
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

    public void onLogon(LogonEvent e) {
        setEnabled(true);
    }

    public void onLogoff(LogonEvent e) {
        setEnabled(false);
    }

    public boolean isAscending() {
        return false;   //must override, prefer to make this abstract
    }
}
