package com.l7tech.console.tree;

/**
 * Base class for leaf nodes (no children) with icons.
 *
 * @author $Author$
 * @version $Revision$
 */
public abstract class AbstractLeafPaletteNode extends AbstractAssertionPaletteNode {

    //- PUBLIC

    /**
     *
     */
    public AbstractLeafPaletteNode(String name, String iconResource) {
        super(null);
        this.name = name != null ? name : "NAME NOT SET";
        this.iconResource = iconResource;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true as this is a leaf
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     *
     * @return false since this is a leaf
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * The name of this node
     *
     * @return the node name that is displayed
     */
    public String getName() {
        return name;
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return iconResource;
    }

    //- PRIVATE

    private final String name;
    private final String iconResource;
}
