package com.l7tech.console.tree.policy;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The identity load children strategy filters out composite assertion
 * levels from the identity policy view. For example the <code>AllAssertion</code>
 * is not rendered, and it's childrend are added to the policy tree directly.
 * For <code>OneOrmoreAssertion</code> it's children are first checked if they
 * are present in one of identity paths. If node is not present, that node does
 * not belong to this policy, and is not rendered. If the number of childrend
 * after the filtering  is 1 or 0 then the <code>OneOrMoreAssertion</code> is
 * not rendered either.
 *
 * @author emil
 * @version 18-Apr-2004
 */
class IdentityLoadChildrenStrategy extends LoadChildrenStrategy {

    public void loadChildren(AssertionTreeNode receiver, CompositeAssertion assertion) {
        loadChildren(receiver, assertion, 0);
    }

    private int loadChildren(AssertionTreeNode receiver, CompositeAssertion assertion, int index) {
        for (Iterator iterator = assertion.children(); iterator.hasNext();) {
            Assertion a = (Assertion)iterator.next();
            if (a instanceof AllAssertion && skipAllAssertion(receiver, (AllAssertion)a)) {
                index += (loadChildren(receiver, (CompositeAssertion)a, index) - index);
            } else if (a instanceof OneOrMoreAssertion && skipOneOrMoreAssertion(receiver, (OneOrMoreAssertion)a)) {
                index += (loadChildren(receiver, (CompositeAssertion)a, index) - index);
            } else {
                receiver.insert(AssertionTreeNodeFactory.asTreeNode(a), index++);
            }
        }
        return index;
    }

    private boolean skipOneOrMoreAssertion(AssertionTreeNode node, OneOrMoreAssertion oa) {
        if (oa.getChildren().size() <= 1) {
            return true;
        }
        List list = new ArrayList();
        list.addAll(oa.getChildren());
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Assertion a = (Assertion)iterator.next();
            if (!presentInPaths(node, a)) {
                iterator.remove();
            }
        }
        return list.size() <= 1;
    }

    private boolean skipAllAssertion(AssertionTreeNode node, AllAssertion aa) {
        CompositeAssertion parent = aa.getParent();
        if (parent instanceof OneOrMoreAssertion && parent.getChildren().size() > 1) {
            List list = new ArrayList();
            list.addAll(parent.getChildren());
            for (Iterator iterator = list.iterator(); iterator.hasNext();) {
                Assertion a = (Assertion)iterator.next();
                if (!presentInPaths(node, a)) {
                    iterator.remove();
                }
            }

            return list.size() <= 1;
        }
        return true;
    }


    private boolean presentInPaths(AssertionTreeNode node, Assertion toCheck) {
        TreeNode[] path = node.getPath();
        if (path.length < 2) return true;

        IdentityPolicyTreeNode in = (IdentityPolicyTreeNode)path[1];
        IdentityPath ip = in.getIdentityPath();
        Set paths = ip.getPaths();
        for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
            Assertion[] apath = ((AssertionPath)iterator.next()).getPath();
            for (int i = apath.length - 1; i >= 0; i--) {
                Assertion assertion = apath[i];
                if (assertion.equals(toCheck)) return true;
            }
        }
        return false;
    }
}
