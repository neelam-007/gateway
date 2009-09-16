package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.Functions;

/**
 * Palette node used for modular assertions that do not specify a custom palette node factory in their
 * metadata.
 */
public class DefaultAssertionPaletteNode<AT extends Assertion> extends AbstractLeafPaletteNode implements Comparable<AbstractTreeNode> {
    final AT prototype;

    public DefaultAssertionPaletteNode(AT prototype) {
        super(prototype);
        this.prototype = prototype;
    }

    @Override
    protected ClassLoader iconClassLoader() {
        return prototype.getClass().getClassLoader();
    }

    @Override
    public Assertion asAssertion() {
        //noinspection unchecked
        Functions.Unary<AT, AT> factory =
                (Functions.Unary<AT, AT>)
                        prototype.meta().get(AssertionMetadata.ASSERTION_FACTORY);
        if (factory != null)
            return factory.call(prototype);

        // no factory -- try to just newInstance off the prototype
        try {
            return prototype.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo( final AbstractTreeNode treeNode ) {
        Integer priority = getSortPriority(this);
        Integer otherPriority = getSortPriority(treeNode);

        int result = otherPriority.compareTo(priority);
        if ( result == 0 ) {
            result = String.CASE_INSENSITIVE_ORDER.compare(getName(),treeNode.getName());
        }

        return result;
    }

    private static Integer getSortPriority( final AbstractTreeNode treeNode ) {
        Integer sortPriority = treeNode.asAssertion().meta().get(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY);
        if ( sortPriority == null ) {
            sortPriority = Integer.MIN_VALUE;
        }
        return sortPriority;
    }
}
