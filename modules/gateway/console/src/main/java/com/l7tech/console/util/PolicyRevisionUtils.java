package com.l7tech.console.util;

import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utilities for working with policy revisions.
 */
public class PolicyRevisionUtils {

    /**
     * Select a policy revision for use.
     *
     * @param policyOid The policy
     * @param action The action (e.g. "edit", "copy")
     * @return The selected policy version or none
     * @throws FindException If an error occurs
     */
    public static Option<PolicyVersion> selectRevision( final long policyOid,
                                                        @NotNull final String action ) throws FindException {
        final List<PolicyVersion> versions = Registry.getDefault().getPolicyAdmin().findPolicyVersionHeadersByPolicy(policyOid);

        // Sort more recent revisions to the top
        Collections.sort( versions, new Comparator<PolicyVersion>() {
            @Override
            public int compare( PolicyVersion o1, PolicyVersion o2 ) {
                return Long.valueOf( o2.getOrdinal() ).compareTo( o1.getOrdinal() );
            }
        } );

        List<Nullary<Option<PolicyVersion>>> options = new ArrayList<Nullary<Option<PolicyVersion>>>();

        final Nullary<Option<PolicyVersion>> none = new Nullary<Option<PolicyVersion>>() {
            @Override
            public Option<PolicyVersion> call() {
                return none();
            }

            public String toString() {
                return "No selected revision (empty policy)";
            }
        };
        options.add(none);

        final DateFormat dateFormat = DateFormat.getInstance();
        for ( final PolicyVersion policyVersion : versions ) {
            final long ordinal = policyVersion.getOrdinal();
            final String date = dateFormat.format(policyVersion.getTime());
            String name = policyVersion.getName();
            name = name == null ? "" : " (" + name + ')';
            final String displayString = ordinal + " " + date + name;

            options.add(new Nullary<Option<PolicyVersion>>() {
                @Override
                public Option<PolicyVersion> call() {
                    return some( policyVersion );
                }

                public String toString() {
                    return displayString;
                }
            });
        }

        Object result = JOptionPane.showInputDialog(TopComponents.getInstance().getTopParent(),
                                                    "This policy has been disabled by having its active revision revoked.\n" +
                                                    "Please choose a revision on which to base your "+action+".",
                                                    "Choose Policy Revision",
                                                    JOptionPane.QUESTION_MESSAGE,
                                                    null,
                                                    options.toArray(new Nullary[options.size()]),
                                                    none );

        if (result == null)
            return null;

        //noinspection unchecked
        return ((Nullary<Option<PolicyVersion>>)result).call();
    }

}
