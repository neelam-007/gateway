package com.l7tech.console.tree.servicesAndPolicies;

import com.l7tech.console.tree.AbstractTreeNode;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

/**
 *
 */
public class FolderAndNodeTransferable implements Transferable {
    public static final DataFlavor ALLOWED_DATA_FLAVOR;
    static {
        DataFlavor df = null;
        try {
            df = new DataFlavor("X-transferable/nodebase-treenodes; class=java.util.List");
        } catch(ClassNotFoundException ignored ) {
        }
        ALLOWED_DATA_FLAVOR = df;
    }

    private final List<AbstractTreeNode> nodes;

    public FolderAndNodeTransferable(List<AbstractTreeNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public List<AbstractTreeNode> getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if(ALLOWED_DATA_FLAVOR.equals(flavor)) {
            return nodes;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {ALLOWED_DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ALLOWED_DATA_FLAVOR.equals(flavor);
    }
}
