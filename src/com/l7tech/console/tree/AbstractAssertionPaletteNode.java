package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.tree.policy.Descriptions;

import java.util.Comparator;

/**
 * User: megery
 * Date: Mar 8, 2006
 * Time: 4:23:22 PM
 */
public abstract class AbstractAssertionPaletteNode extends AbstractTreeNode {
    protected String descriptionText = null;

    public AbstractAssertionPaletteNode(Object object) {
        super(object);
        init();
    }

    protected AbstractAssertionPaletteNode(Object object, Comparator c) {
        super(object, c);
        init();
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    abstract public String getName();

    abstract protected String iconResource(boolean open);

    private void init() {
        Assertion assn = asAssertion();
        if (assn != null) {
            String desc = Descriptions.getDescription(asAssertion()).getDescriptionText();
            if (desc != null) {
                descriptionText= desc;
            }
        }
    }
}
