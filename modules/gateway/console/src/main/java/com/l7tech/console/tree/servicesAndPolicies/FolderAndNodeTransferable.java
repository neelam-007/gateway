package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.AbstractTreeNode;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 13, 2008
 * Time: 7:04:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FolderAndNodeTransferable implements Transferable {
    protected static DataFlavor ALLOWED_DATA_FLAVOR = null;
    static {
        try {
            ALLOWED_DATA_FLAVOR = new DataFlavor("X-transferable/nodebase-treenodes; class=java.util.List");
        } catch(ClassNotFoundException e) {
        }
    }

    private final List<AbstractTreeNode> nodes;

    public FolderAndNodeTransferable(List<AbstractTreeNode> nodes) {
        this.nodes = nodes;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if(ALLOWED_DATA_FLAVOR.equals(flavor)) {
            return nodes;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {ALLOWED_DATA_FLAVOR};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ALLOWED_DATA_FLAVOR.equals(flavor);
    }
}
