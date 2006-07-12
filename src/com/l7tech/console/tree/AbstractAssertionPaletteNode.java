package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.Descriptions;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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

    protected boolean isEnabledByLicense() {
        int numKids = getChildCount();

        if (numKids == 0) {
            // It's a leaf.
            Assertion ass = asAssertion();
            if (ass == null) return true; // TODO Currently, non-Assertion palette nodes are shown regardless of license
            return Registry.getDefault().getLicenseManager().isAssertionEnabled(ass);
        }

        // Otherwise, we're enabled if at least one kid is enabled.
        for (int i = 0; i < numKids; ++i) {
            TreeNode kid = getChildAt(i);
            if (kid instanceof AbstractAssertionPaletteNode) {
                AbstractAssertionPaletteNode palKid = (AbstractAssertionPaletteNode)kid;
                if (palKid.isEnabledByLicense()) return true;
            }
        }
        return false;
    }

    protected void filterChildren() {
        List keepKids = new ArrayList();

        int numKids = getChildCount();
        for (int i = 0; i < numKids; ++i) {
            TreeNode kid = getChildAt(i);
            if (kid instanceof AbstractAssertionPaletteNode) {
                AbstractAssertionPaletteNode palKid = (AbstractAssertionPaletteNode)kid;
                if (!palKid.isEnabledByLicense()) continue; // Skip adding this kid
            }
            keepKids.add(kid);
        }

        int index = 0;
        children = null;
        for (Iterator i = keepKids.iterator(); i.hasNext();) {
            MutableTreeNode treeNode = (MutableTreeNode)i.next();
            treeNode.setParent(null);
            insert(treeNode, index++);
        }
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
