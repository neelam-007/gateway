package com.l7tech.console.panels.policydiff;

import com.l7tech.console.tree.policy.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BlankAssertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.Triple;

import java.util.*;

import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.*;

/**
 * Compare two policy trees by assertions and generate diff results.
 */
public class TreeDiff {
    /**
     * Compare two policy trees by assertions and generate diff results.
     * At the end of computation, the results (contained in a map) will be assigned to the policy tree.
     *
     * @param policyTree1: the left policy tree holds assertions to be diffed.
     * @param policyTree2: the right policy tree holds assertions to be diffed.
     */
    public TreeDiff(final PolicyDiffTree policyTree1, final PolicyDiffTree policyTree2) {
        final Map<Integer, DiffType> diffResultMap1 = new TreeMap<>(); // The left policy tree diff result map, where each pair consists of assertion node row number and its DiffType object.
        final Map<Integer, DiffType> diffResultMap2 = new TreeMap<>(); // The right policy tree diff result map, where each pair consists of assertion node row number and its DiffType object.
        final Map<AssertionTreeNode, AssertionTreeNode> matchedParents = new HashMap<>();

        final List<AssertionTreeNode> nodeList1 = new ArrayList<>(policyTree1.getRowCount());
        final List<AssertionTreeNode> nodeList2 = new ArrayList<>(policyTree2.getRowCount());

        final PolicyTreeModel policyTreeModel1 = (PolicyTreeModel) policyTree1.getModel();
        final PolicyTreeModel policyTreeModel2 = (PolicyTreeModel) policyTree2.getModel();

        // Expand two policy trees
        Utilities.expandTree(policyTree1);
        Utilities.expandTree(policyTree2);

        // Retrieve all assertion nodes
        for (int row = 0; row < policyTree1.getRowCount(); row++) {
            nodeList1.add((AssertionTreeNode) policyTree1.getPathForRow(row).getLastPathComponent());
        }
        for (int row = 0; row < policyTree2.getRowCount(); row++) {
            nodeList2.add((AssertionTreeNode) policyTree2.getPathForRow(row).getLastPathComponent());
        }

        // Diff two policy trees and get the assertion diff results
        AssertionTreeNode node1, node2;
        int index1 = 0; // The index is used to traverse nodeList1.  The tree traversing starts from the first node.
        int index2 = 0; // The index is used to traverse nodeList2.  The tree traversing starts from the first node.
        int row1 = 0, row2 = 0; // Row number starts 0 (not 1).  row1 and row2 are row numbers of the modified policyTree1 or the modified policyTree2,
        // and not the same as index1 and index2, which are row numbers of the original policyTree1 and the original policyTree2.
        while (index1 < nodeList1.size() && index2 < nodeList2.size()) {
            // Find node1's match and its searching depth
            node1 = nodeList1.get(index1);
            Pair<Integer, Integer> searchResults = findMatchAndDepth(true, node1, index2, nodeList2, matchedParents);
            int idxMatched1 = searchResults.left; // The index of a policyTree2's node, which is matched to node1
            int depth1 = searchResults.right;     // The depth of the policyTree2's node from the search-starting point

            // Find node2's match and its searching depth
            node2 = nodeList2.get(index2);
            searchResults = findMatchAndDepth(false, node2, index1, nodeList1, matchedParents);
            int idxMatched2 = searchResults.left; // The index of a policyTree1's node, which is matched to node2
            int depth2 = searchResults.right;     // The depth of the policyTree1's node from the search-starting point

            // Compare the depths and get the better match result.  There are 5 cases.
            if ((idxMatched1 >= index2 && idxMatched2 >= index1 && depth1 <= depth2) ||       // Case 1: Both nodes have their matches, but node1 has a smaller search depth than node2.
                    (idxMatched1 >= index2 && idxMatched2 == -1)) {                           // Case 2: node1 has a match, but node2 does not have a match.

                // Generate diff results, where two same nodes are marked as DiffType.IDENTICAL, or two matched nodes with different
                // properties are marked as DiffType.MATCHED_WITH_DIFFERENCES.  Other nodes above these nodes are marked as DiffType.DELETED.
                int[] results = processMatches(
                        true, index1, index2, idxMatched1, row1, row2, policyTreeModel1, node1,
                        nodeList1, nodeList2, diffResultMap1, diffResultMap2, matchedParents
                );
                if (results == null || results.length != 4)
                    throw new RuntimeException("Processing matches returns wrong results.  Should not happen here!  Just for DEV debug.");

                // Update index pointers and row numbers after the process
                index1 = results[0];
                index2 = results[1];
                row1 = results[2];
                row2 = results[3];
            } else if ((idxMatched1 >= index2 && idxMatched2 >= index1 && depth1 > depth2) || // Case 3: Both nodes have their matches, but node2's match has less search depth than node1's match.
                            (idxMatched1 == -1 && idxMatched2 >= index1)) {                   // Case 4: node2 has a match, but node1 does not have a match.

                // Generate diff results, where two same nodes are marked as DiffType.IDENTICAL, or two matched nodes with different
                // properties are marked as DiffType.MATCHED_WITH_DIFFERENCES.  Other nodes above these nodes are marked as DiffType.INSERTED.
                int[] results = processMatches(
                        false, index2, index1, idxMatched2, row2, row1, policyTreeModel2, node2,
                        nodeList2, nodeList1, diffResultMap2, diffResultMap1, matchedParents
                );
                if (results == null || results.length != 4)
                    throw new RuntimeException("Processing matches returns wrong results.  Should not happen here!  Just for DEV debug.");

                // Update index pointers and row numbers after the process
                index2 = results[0];
                index1 = results[1];
                row2 = results[2];
                row1 = results[3];
            } else {                                                                          // Case 5: Both nodes do not have any match.
                // Generate diff results, where left nodes marked as DiffType.DELETED.
                Triple<Integer, Integer, Integer> results = processMismatches(true, index1, index2, row1, row2, node1, node2, policyTreeModel2, nodeList2, diffResultMap1, diffResultMap2);

                // Update index pointers and row numbers after the process
                index1 = results.left;
                row1 = results.middle;
                row2 = results.right;

                // Update node1 since index1 is changed and node1 will be used in the next processMismatches method
                node1 = nodeList1.get((index1 == nodeList1.size())? index1 - 1 : index1);

                // Generate diff results, where right nodes marked as DiffType.INSERTED.
                results = processMismatches(false, index2, index1, row2, row1, node2, node1, policyTreeModel1, nodeList1, diffResultMap2, diffResultMap1);

                // Update index pointers and row numbers after the process
                index2 = results.left;
                row2 = results.middle;
                row1 = results.right;
            }
        }

        // Check if nodeList1 or nodeList2 has some assertion nodes left and not compared yet.
        if (index1 < nodeList1.size()) {
            processRestAssertions(true, index1, row1, row2, nodeList1, nodeList2, policyTreeModel2, diffResultMap1, diffResultMap2);
        } else if (index2 < nodeList2.size()) {
            processRestAssertions(false, index2, row2, row1, nodeList2, nodeList1, policyTreeModel1, diffResultMap2, diffResultMap1);
        }

        policyTree1.setDiffResultMap(diffResultMap1);
        policyTree2.setDiffResultMap(diffResultMap2);
    }

    /**
     * A helper method finds the index of a policyTree2's node, which is identical or matched to node1, a policyTree1 node.
     * Also find the depth of the policyTree2's node from the search-starting point.
     *
     * @param fromLeftToRight: true if the comparison order is from the left policy tree to the right policy tree.
     *                         Otherwise, false if the comparison order is from right to left.
     * @param node1: the policyTree1's node, for which this method will find a match index and depth from policyTree2
     * @param idxPtr2: the search-starting index in the original policyTree2
     * @param nodeList2: holds all policyTree2 nodes
     * @param matchedParents: holds all matched or identical parents' mappings
     *
     * @return two results: the matching index and the searching depth
     */
    private Pair<Integer, Integer> findMatchAndDepth(boolean fromLeftToRight, AssertionTreeNode node1, int idxPtr2,
                                                     List<AssertionTreeNode> nodeList2,
                                                     Map<AssertionTreeNode, AssertionTreeNode> matchedParents) {
        final int maxSearchDepth = SyspropUtil.getInteger(SYSTEM_PROPERTY_MAX_SEARCH_DEPTH, DEFAULT_MAX_SEARCH_DEPTH);
        int idxMatched = -1;  // -1 is a default value, which means no match.
        int depth = 0;
        AssertionTreeNode parent1, parent2, node2;
        Assertion assertion1, assertion2;

        for (int i = idxPtr2; i < nodeList2.size() && depth < maxSearchDepth; i++) {
            node2 = nodeList2.get(i);
            // First check if they have a same type of assertion class
            assertion1 = node1.asAssertion();
            assertion2 = node2.asAssertion();
            if (assertion1.getClass() == assertion2.getClass()) {
                // Second check if they share a same parent
                parent1 = (AssertionTreeNode) node1.getParent();
                parent2 = (AssertionTreeNode) node2.getParent();
                if ((parent1 == null && parent2 == null) || (fromLeftToRight ? matchedParents.get(parent1) == parent2 : matchedParents.get(parent2) == parent1)) {
                    // Last check: break to 3 cases to check if two assertions are matched or not
                    if (// Case 1: if they are Include assertions, then check if they have a same policy fragment.
                            (assertion1 instanceof Include && ((Include) assertion1).getPolicyGuid().equals(((Include) assertion2).getPolicyGuid())) ||
                                    // Case 2: if they are custom assertions, then check if they have a same CustomAssertion type.
                                    (assertion1 instanceof CustomAssertionHolder && ((CustomAssertionHolder) assertion1).getCustomAssertion().getClass() == ((CustomAssertionHolder) assertion2).getCustomAssertion().getClass()) ||
                                    // Case 3: they are neither Include assertions nor custom assertions.
                                    (!(assertion1 instanceof Include) && !(assertion1 instanceof CustomAssertionHolder))) {
                        idxMatched = i;
                        break;
                    }
                }
            }

            // If tempNode2 does not match node1, but tempNode2 is an IncludePolicyNode or a CompositeAssertionTreeNode, then escape tempNode2's all descendant nodes.
            if (node2 instanceof IncludeAssertionPolicyNode || node2 instanceof CompositeAssertionTreeNode) {
                i += node2.getDescendantCount();
            }

            depth++;
        }

        return new Pair<>(idxMatched, depth);
    }

    /**
     * A helper method generates diff results for two nodes, which are identical or matched with property differences.
     * IDENTICAL or MATCHED_WITH_DIFFERENCES will be recorded in the result map1 for those identical/matched nodes,
     * while INSERTED or DELETED will be recorded in the result map2 for those not-matched nodes above the identical/matched node.
     *
     * @param fromLeftToRight: true if the comparison order is from the left policy tree to the right policy tree.
     *                         Otherwise, false if the comparison order is from right to left.
     * @param idxPtr1: the row number of node1 in the original policyTree1
     * @param idxPtr2: the row number of node2 in the original policyTree2
     * @param idxMatched1: the index of a policyTree2's node, which is matched to node1
     * @param row1: the current row number of node in the modified policyTree1
     * @param row2: the current row number of node in the modified policyTree2
     * @param policyTreeModel1: the policyTree1's tree model
     * @param node1: the node in policyTree1 has been indicated as identical or matched with one node in policyTree2
     * @param nodeList1: hold all nodes in policyTree1
     * @param nodeList2: hold all nodes in policyTree2
     * @param diffResultMap1: the diff result map for policyTree1
     * @param diffResultMap2: the diff result map for policyTree2
     * @param matchedParents: holds all matched or identical parents' mappings
     *
     * @return an integer array holds updated idxPtr1, idxPtr2, row1 and row2.
     */
    private int[] processMatches(boolean fromLeftToRight, int idxPtr1, int idxPtr2, int idxMatched1, int row1, int row2,
                                        PolicyTreeModel policyTreeModel1, AssertionTreeNode node1,
                                        List<AssertionTreeNode> nodeList1, List<AssertionTreeNode> nodeList2,
                                        Map<Integer, DiffType> diffResultMap1, Map<Integer, DiffType> diffResultMap2,
                                        Map<AssertionTreeNode, AssertionTreeNode> matchedParents) {
        // Step1: Process the not-matched nodes, whose position is above the matched node in policyTreeB
        Pair<AssertionTreeNode, Integer> insertionInfo1 = getInsertionInfo(policyTreeModel1, node1, nodeList1, idxPtr1);

        for (int m = idxPtr2; m < idxMatched1; m++) {
            policyTreeModel1.insertNodeInto(new DefaultAssertionPolicyNode<>(new BlankAssertion()), insertionInfo1.left, insertionInfo1.right);
            diffResultMap1.put(row1++, null); // "null" refers to a Blank Assertion.

            diffResultMap2.put(row2++, fromLeftToRight ? DiffType.INSERTED : DiffType.DELETED);
            idxPtr2++;
        }
        // Step 2: Process two identical or matched-with-differences nodes
        AssertionTreeNode node2 = nodeList2.get(idxPtr2);
        String assertion1Xml = WspWriter.getPolicyXml(node1.asAssertion());
        String assertion2Xml = WspWriter.getPolicyXml(node2.asAssertion());

        // If two assertions are same, their assertion-enabling status must be same and also one of two below conditions must be satisfied.
        // (1) These assertions are composite assertions, since a composite assertion xml includes all children's assertion xml, so don't compare two composite assertions' xml.
        // (2) If they are not composite assertions and both XML contents of two assertion are identical, then two assertions are identical.
        if ((node1.isAssertionEnabled() == node2.isAssertionEnabled()) &&
                ((node1 instanceof CompositeAssertionTreeNode && node2 instanceof CompositeAssertionTreeNode) || assertion1Xml.equals(assertion2Xml))) {
            diffResultMap1.put(row1++, DiffType.IDENTICAL);
            diffResultMap2.put(row2++, DiffType.IDENTICAL);
        } else {
            diffResultMap1.put(row1++, DiffType.MATCHED_WITH_DIFFERENCES);
            diffResultMap2.put(row2++, DiffType.MATCHED_WITH_DIFFERENCES);
        }
        // Save their parent information
        if (node1 instanceof CompositeAssertionTreeNode && node2 instanceof CompositeAssertionTreeNode) {
            if (fromLeftToRight)
                matchedParents.put(node1, node2);
            else
                matchedParents.put(node2, node1);
        }
        idxPtr1++;
        idxPtr2++;

        // If nodeA and node2 are the same IncludeAssertionNode, then mark all their descendants as matched.
        if (node1 instanceof IncludeAssertionPolicyNode && node2 instanceof IncludeAssertionPolicyNode) {
            int descendantCount1 = node1.getDescendantCount();
            int descendantCount2 = node2.getDescendantCount();
            if (descendantCount1 != descendantCount2) {
                // Should happen here!  Just for DEV check.
                throw new RuntimeException("Two matched IncludePolicyNodes must be identical.");
            }

            for (int i = 0; i < descendantCount1; i++) {
                diffResultMap1.put(row1++, DiffType.IDENTICAL);
                diffResultMap2.put(row2++, DiffType.IDENTICAL);
            }

            idxPtr1 += descendantCount1;
            idxPtr2 += descendantCount1;
        }

        return new int[] {idxPtr1, idxPtr2, row1, row2};
    }

    /**
     * A helper method generates diff results for a policyTree1's node, which is not identical and matched to any nodes in policyTree2.
     * DELETED or INSERTED will be recorded in the result map1, while null will be saved in the result map2.
     *
     * @param fromLeftToRight: true if the comparison order is from the left policy tree to the right policy tree.
     *                         Otherwise, false if the comparison order is from right to left.
     * @param idxPtr1: the row number of node1 in the original policyTree1
     * @param idxPtr2: the row number of node2 in the original  policyTree2
     * @param row1: the current row number of node in the modified policyTree1
     * @param row2: the current row number of node in the modified policyTree2
     * @param node1: the node in policyTree1 has been indicated as not identical and matched with one node in policyTree2
     * @param node2: the node in policyTree2 has been indicated as not identical and matched with one node in policyTree1
     * @param policyTreeModel2: the policyTree2's tree model
     * @param nodeList2: hold all nodes in policyTree2
     * @param diffResultMap1: the diff result map for policyTree1
     * @param diffResultMap2: the diff result map for policyTree2
     *
     * @return an integer array holds updated idxPtr1, row1, and row2.
     */
    private Triple<Integer, Integer, Integer> processMismatches(
            boolean fromLeftToRight, int idxPtr1, int idxPtr2, int row1, int row2,
            AssertionTreeNode node1, AssertionTreeNode node2,
            PolicyTreeModel policyTreeModel2, List<AssertionTreeNode> nodeList2,
            Map<Integer, DiffType> diffResultMap1, Map<Integer, DiffType> diffResultMap2) {

        // Find where a new inserted blank assertion will be inserted and its parent used for insertion.
        Pair<AssertionTreeNode, Integer> insertionInfo2 = getInsertionInfo(policyTreeModel2, node2, nodeList2, idxPtr2);

        // If node1 is an Include assertion or a composite assertion, count how many children it has,
        // so DELETED or INSERTED will be recorded in "repeatCount" times.
        int repeatCount = 1;
        if (node1 instanceof IncludeAssertionPolicyNode || node1 instanceof CompositeAssertionTreeNode) {
            repeatCount += node1.getDescendantCount();
        }
        for (int i = 0; i < repeatCount; i++) {
            diffResultMap1.put(row1++, fromLeftToRight? DiffType.DELETED : DiffType.INSERTED);

            diffResultMap2.put(row2++, null);  // "null" refers to a Blank Assertion.
            policyTreeModel2.insertNodeInto(new DefaultAssertionPolicyNode<>(new BlankAssertion()), insertionInfo2.left, insertionInfo2.right);

            idxPtr1++;
        }

        return new Triple<>(idxPtr1, row1, row2);
    }

    /**
     * A helper method generates diff results for those nodes left and not diffed.
     *
     * @param fromLeftToRight: true if the comparison order is from the left policy tree to the right policy tree.
     *                         Otherwise, false if the comparison order is from right to left.
     * @param idxPtr1: the row number of node1 in the original  policyTree1
     * @param row1: the current row number of node in the modified policyTree1
     * @param row2: the current row number of node in the modified policyTree2
     * @param nodeList1: hold all nodes in policyTree1
     * @param nodeList2: hold all nodes in policyTree2
     * @param policyTreeModel2: the policyTree2's tree model
     * @param diffResultMap1: the diff result map for policyTree1
     * @param diffResultMap2: the diff result map for policyTree2
     */
    private void processRestAssertions(boolean fromLeftToRight, int idxPtr1, int row1, int row2,
                                       List<AssertionTreeNode> nodeList1, List<AssertionTreeNode> nodeList2,
                                       PolicyTreeModel policyTreeModel2, Map<Integer, DiffType> diffResultMap1,
                                       Map<Integer, DiffType> diffResultMap2) {
        // Find where a new inserted blank assertion will be inserted and its parent used for insertion.
        AssertionTreeNode lastNode2 = nodeList2.get(nodeList2.size() - 1);
        IncludeAssertionPolicyNode includeParent = lastNode2.findTopmostIncludeAncestor();

        AssertionTreeNode parent;
        if (includeParent != null) {
            lastNode2 = includeParent;
            parent = (AssertionTreeNode) includeParent.getParent();
        } else {
            parent = (AssertionTreeNode) lastNode2.getParent();
        }

        if (parent == null) {
            parent = (AssertionTreeNode) policyTreeModel2.getRoot();
        }

        int newNodeIdx = policyTreeModel2.getIndexOfChild(parent, lastNode2) + 1;

        for (int i = idxPtr1; i < nodeList1.size(); i++) {
            // Mark the assertion status in the policy tree1 as DELETED or the policy tree2 as INSERTED
            diffResultMap1.put(row1++, fromLeftToRight? DiffType.DELETED: DiffType.INSERTED);

            // Add a blank assertion in the policy tree 2
            policyTreeModel2.insertNodeInto(new DefaultAssertionPolicyNode<>(new BlankAssertion()), parent, newNodeIdx);
            diffResultMap2.put(row2++, null);
        }
    }

    /**
     * A helper method gets the insertion information of a new blank assertion, such as the new assertion's parent and the index where to be inserted.
     *
     * @param policyTreeModel: the policy tree model
     * @param node: the node, before which a new blank assertion node will be inserted
     * @param nodeList: holds all nodes of the policy tree
     * @param idxPtr: the row number of the node in the original policy tree
     *
     * @return the parent of the new inserted blank assertion node, and the insertion index
     */
    private Pair<AssertionTreeNode, Integer> getInsertionInfo(PolicyTreeModel policyTreeModel, AssertionTreeNode node, List<AssertionTreeNode> nodeList, int idxPtr) {
        AssertionTreeNode precedingNode = idxPtr == 0 ? (AssertionTreeNode) policyTreeModel.getRoot() : nodeList.get(idxPtr - 1);
        final IncludeAssertionPolicyNode includeParent = precedingNode.findTopmostIncludeAncestor();

        AssertionTreeNode parent;
        int newNodeIdx;

        if (includeParent != null) {
            precedingNode = includeParent;
            parent = (AssertionTreeNode) includeParent.getParent();
            newNodeIdx = policyTreeModel.getIndexOfChild(parent, precedingNode) + 1;
        } else if (precedingNode == node.getParent()) { // In this case, the preceding node of node1 is the node's parent.
            parent = precedingNode;
            newNodeIdx = 0;
        } else {
            parent = (AssertionTreeNode) precedingNode.getParent();
            if (parent == null) parent = (AssertionTreeNode) policyTreeModel.getRoot();
            newNodeIdx = policyTreeModel.getIndexOfChild(parent, precedingNode) + 1;
        }

        return new Pair<>(parent, newNodeIdx);
    }
}