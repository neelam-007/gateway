package com.l7tech.console.tree;

import java.awt.datatransfer.*;
import java.util.Arrays;
import javax.swing.tree.TreePath;


/**
 * This represents a TreePath (a node in a JTree) that can be
 * transferred between a drag source and a drop target.
 * 
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0

 */
public class TransferableTreePath implements Transferable {
    // The type of DnD object being dragged...
    public static final DataFlavor TREEPATH_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "TreePath");
    // supported flavors
    private final DataFlavor[] flavors = {TREEPATH_FLAVOR};

    private final TreePath path;

    /**
     * Constructs a transferrable tree path object for the
     * specified path.
     */
    public TransferableTreePath(TreePath path) {
        this.path = path;
    }

    // Transferable interface methods...
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arrays.asList(flavors).contains(flavor);
    }

    public synchronized Object getTransferData(DataFlavor flavor)
      throws UnsupportedFlavorException {
        if (flavor.isMimeTypeEqual(TREEPATH_FLAVOR.getMimeType()))
            return path;
        else
            throw new UnsupportedFlavorException(flavor);
    }


}
	
