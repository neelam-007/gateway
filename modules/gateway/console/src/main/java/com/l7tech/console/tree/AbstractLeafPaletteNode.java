package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

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
    public AbstractLeafPaletteNode(final String name, final String iconResource) {
        super(null);
        this.name = name != null ? name : "NAME NOT SET";
        this.iconResource = iconResource;
    }

    public AbstractLeafPaletteNode(final Assertion assertion){
        super(null);
        final AssertionMetadata meta = assertion.meta();
        this.name = meta.get(PALETTE_NODE_NAME).toString();
        this.iconResource = meta.get(PALETTE_NODE_ICON).toString();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true as this is a leaf
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     *
     * @return false since this is a leaf
     */
    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * The name of this node
     *
     * @return the node name that is displayed
     */
    @Override
    public String getName() {
        return name;
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open not applicable to palette nodes
     */
    @Override
    protected String iconResource(boolean open) {
        return iconResource;
    }

    //- PRIVATE

    private final String name;
    private final String iconResource;
}
