package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.*;

/**
 * The policy validator that analyzes the policy assertion tree
 * and collects the errors.
 *
 * It analyzes the policy using the following :
 * <p>
 * <ul>
 * <li>Given the assertion tree, generate the list of possible policy
 * paths. Policy path is a sequence of assertions that is processed
 * by the policy enforcemment point (<i>message processor</i> in L7
 * parlance). The <i>and</i> composite assertion results in a single
 * path, and <i>or</i> composite asseriotn generates number of paths
 * that is equal to a number of children. This is done by preorder
 * traversal of the policy tree.
 * <li>Processes each policy path and determine if the sequence of
 * assertions is correct.
 * </ul>
 * The correct sequence of assertions is following:
 * <ul>
 * <li><i>Pre conditions</i> that is
 * </ul>
 * <p>
 *
 * Errors are collected in the PolicyValidatorResult instance.
 * <p>
 * The class methods are not synchronized.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidator extends PolicyValidator {
    /**
     * Validates the specified assertion tree.
     *
     * @param assertion the assertion tree to be validated.
     * @return the result of the validation
     */
    public PolicyValidatorResult validate(Assertion assertion) {
        if (isValidRoot(assertion)) {
            generatePaths(assertion);
        } else {
            result.addError(new PolicyValidatorResult.Error(assertion, "Invalid root", null));
        }
        return result;
    }

    /**
     * generate possible assertion paths
     * @param assertion the root assertion
     */
    private void generatePaths(Assertion assertion) {
        assertionPaths.add(new AssertionPath(assertion));
        for (Iterator preorder = assertion.preorderIterator(); preorder.hasNext();) {
            Assertion anext = (Assertion)preorder.next();
            if (createsNewPaths(anext)) {
                CompositeAssertion ca = (CompositeAssertion)assertion;
                Iterator children = ca.getChildren().iterator();
                Set newPaths = new HashSet();
                Set removePaths = new HashSet();
                for (; children.hasNext();) {
                    Assertion child = (Assertion)children.next();
                    for (Iterator ipaths = assertionPaths.iterator(); ipaths.hasNext();) {
                        AssertionPath parentPath = (AssertionPath)ipaths.next();
                        AssertionPath newPath = new AssertionPath(parentPath);
                        newPath.addAssertion(child);
                        newPaths.add(newPath);
                        removePaths.add(parentPath);
                    }
                }
                assertionPaths.addAll(newPaths);
                assertionPaths.removeAll(removePaths);
            } else {
                for (Iterator ipaths = assertionPaths.iterator(); ipaths.hasNext();) {
                    AssertionPath one = (AssertionPath)ipaths.next();
                    one.addAssertion(anext);
                }
            }
        }
    }

    private boolean createsNewPaths(Assertion a) {
        return
          a instanceof ExactlyOneAssertion ||
          a instanceof OneOrMoreAssertion;
    }

    private boolean isValidRoot(Assertion a) {
        return
          a instanceof AllAssertion ||
          a instanceof OneOrMoreAssertion;
    }

    /**
     * Inner class that collects the assertion path.
     */
    protected class AssertionPath {

        public AssertionPath(Assertion a) {
            if (!isValidRoot(a)) {
                addError(new PolicyValidatorResult.Error(a, "Invalid root", null));
            }
            path.add(a);
        }

        /** copy constructor */
        public AssertionPath(AssertionPath ap) {
            if (ap.path.size() == 0) {
                throw new IllegalArgumentException();
            }
            path.addAll(ap.path);
            pathErrors.addAll(ap.pathErrors);
        }

        public void addAssertion(Assertion a) {
            path.add(a);
        }

        public void addError(PolicyValidatorResult.Error err) {
            pathErrors.add(err);
        }

        public Assertion lastAssertion() {
            return (Assertion)path.get(path.size() - 1);
        }

        public List assertions() {
            return Collections.unmodifiableList(path);
        }

        private List path = new ArrayList();
        private List pathErrors = new ArrayList();
    }


    protected Set assertionPaths = new HashSet();
    protected PolicyValidatorResult result = new PolicyValidatorResult();
}
