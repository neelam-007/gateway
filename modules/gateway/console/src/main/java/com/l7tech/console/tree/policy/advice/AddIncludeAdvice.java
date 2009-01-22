package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.IncludeSelectionDialog;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * Advice for selection of policy fragment to include.
 */
public class AddIncludeAdvice implements Advice {

    @Override
    public void proceed( final PolicyChange pc ) {
        Assertion[] assertions = pc.getEvent().getChildren();
        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof Include)) {
            throw new IllegalArgumentException();
        }

        final Include subject = (Include)assertions[0];
        final Frame mw = TopComponents.getInstance().getTopParent();
        final IncludeSelectionDialog includeSelectionDialog = new IncludeSelectionDialog(mw);
        if ( includeSelectionDialog.includeFragmentsAvailable() ) {
            DialogDisplayer.display(includeSelectionDialog, new Runnable() {
                @Override
                public void run() {
                    PolicyHeader header = includeSelectionDialog.getSelectedPolicyFragment();
                    if ( header != null ) {
                        subject.setPolicyName( header.getName() );
                        subject.setPolicyGuid( header.getGuid() );

                        try {
                            // Check for recursion
                            Policy thisPolicy = pc.getPolicyFragment();
                            if ( thisPolicy != null && thisPolicy.getType() == PolicyType.INCLUDE_FRAGMENT && thisPolicy.getOid()>=0 ) {
                                Set<String> policyGuids = new HashSet<String>();
                                policyGuids.add(thisPolicy.getGuid());
                                Registry.getDefault().getPolicyPathBuilderFactory().makePathBuilder().inlineIncludes( subject, policyGuids );
                            }

                            pc.proceed();
                        } catch(PolicyAssertionException e) {
                            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                             "Cannot add Included Policy Fragment due to circular policy include.",
                                                             "Included Policy Fragment Error",
                                                             JOptionPane.WARNING_MESSAGE,
                                                             null );
                        } catch (InterruptedException e) {
                            // don't proceed
                        }
                    }
                }
            });
        } else {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                             "No Policy Include Fragments are currently available.",
                                             "Policy Include Fragments Not Available",
                                             JOptionPane.INFORMATION_MESSAGE,
                                             null );
        }
    }
}
