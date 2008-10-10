package com.l7tech.console.tree;

import java.awt.datatransfer.*;
import java.util.Arrays;
import javax.swing.tree.TreePath;


/**
 * This represents a set of TreePaths (a node in a JTree) that can be
 * transferred between a drag source and a drop target.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0

 */
public class TransferableTreePaths implements Transferable {
    // The type of DnD object being dragged...
    public static final DataFlavor TREEPATH_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "TreePaths");
    // supported flavors
    private final DataFlavor[] flavors = {TREEPATH_FLAVOR};

    private final TreePath[] paths;

    /**
     * Constructs a transferrable tree path object for the
     * specified paths.
     */
    public TransferableTreePaths(TreePath[] paths) {
        this.paths = paths;
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
            return paths;
        else
            throw new UnsupportedFlavorException(flavor);
    }


}