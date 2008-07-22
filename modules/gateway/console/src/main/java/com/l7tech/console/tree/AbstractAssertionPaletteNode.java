package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.Descriptions;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.tree.TreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.*;

/**
 * User: megery
 * Date: Mar 8, 2006
 * Time: 4:23:22 PM
 */
public abstract class AbstractAssertionPaletteNode extends AbstractTreeNode {
    protected String descriptionText = null;
    private boolean checkedDescriptionText = false;

    public AbstractAssertionPaletteNode(Object object) {
        super(object);
    }

    protected AbstractAssertionPaletteNode(Object object, Comparator<? super TreeNode> c) {
        super(object, c);
    }

    public String getDescriptionText() {
        if (!checkedDescriptionText) {
            Assertion assn = asAssertion();
            if (assn != null) {
                String desc = Descriptions.getDescription(assn).getDescriptionText();
                if (desc != null) {
                    descriptionText= desc;
                }
            }
            checkedDescriptionText = true;
        }
        return descriptionText;
    }

    protected boolean isHiddenIfNoChildren() {
        return true;
    }

    protected boolean isEnabledByLicense() {
        int numKids = getChildCount();

        if (numKids == 0) {
            // It's a leaf.
            Assertion ass = asAssertion();
            // TODO Currently, non-Assertion palette nodes are shown regardless of license
            return ass == null || Registry.getDefault().getLicenseManager().isAssertionEnabled(ass);
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
        List<TreeNode> keepKids = new ArrayList<TreeNode>();

        int numKids = getChildCount();
        for (int i = 0; i < numKids; ++i) {
            TreeNode kid = getChildAt(i);
            if (kid instanceof CustomAccessControlNode) {
                // Have to check for CustomAccessControlNode specially, since it doesn't extend AbstractAssertionPaletteNode.
                Assertion ass = ((CustomAccessControlNode)kid).asAssertion();
                if (ass != null && !Registry.getDefault().getLicenseManager().isAssertionEnabled(ass))
                    continue; // Skip adding this kid
            } else if (kid instanceof AbstractAssertionPaletteNode) {
                AbstractAssertionPaletteNode palKid = (AbstractAssertionPaletteNode)kid;
                if (!palKid.isEnabledByLicense()) continue; // Skip adding this kid
            }
            keepKids.add(kid);
        }

        int index = 0;
        children = null;
        for (TreeNode keepKid : keepKids) {
            MutableTreeNode treeNode = (MutableTreeNode)keepKid;
            treeNode.setParent(null);
            insert(treeNode, index++);
        }
    }

    abstract public String getName();

    abstract protected String iconResource(boolean open);
}
