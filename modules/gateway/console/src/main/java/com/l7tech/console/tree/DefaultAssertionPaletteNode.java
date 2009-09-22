package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.Functions;

/**
 * Palette node used for modular assertions that do not specify a custom palette node factory in their
 * metadata.
 */
public class DefaultAssertionPaletteNode<AT extends Assertion> extends AbstractLeafPaletteNode {
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
}
