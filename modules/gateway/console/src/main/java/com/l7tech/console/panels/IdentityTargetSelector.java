package com.l7tech.console.panels;

import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.gui.widgets.OkCancelDialog;

import java.awt.*;
import java.util.*;

/**
 * Dialog for selection of a target identity.
 */
public class IdentityTargetSelector extends OkCancelDialog<IdentityTarget> {

    //- PUBLIC

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion  ) {
        this( owner, readOnly, identityTargetableAssertion, identityTargetableAssertion.getPath()[0], getTargetMessageType(identityTargetableAssertion) );
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion,
                                   final TargetMessageType targetMessageType ) {
        this( owner, readOnly, identityTargetableAssertion, identityTargetableAssertion.getPath()[0], targetMessageType );
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion,
                                   final Assertion policy,
                                   final TargetMessageType targetMessageType ) {
        this( owner,
              readOnly,
              (IdentityTargetable)identityTargetableAssertion,
              getIdentityTargetOptions(policy, identityTargetableAssertion, targetMessageType));
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final IdentityTargetable identityTargetable,
                                   final IdentityTarget[] identityTargetOptions ) {
        super( owner,
               bundle.getString("dialog.title"),
               true,
               new IdentityTargetSelectorValidatedPanel(identityTargetable, identityTargetOptions),
               readOnly );
        this.identityTargetable = identityTargetable;
        this.originalIdentityTarget = new IdentityTarget( identityTargetable.getIdentityTarget() );
    }

    public boolean hasAssertionChanged() {
        return !originalIdentityTarget.equals( new IdentityTarget( identityTargetable.getIdentityTarget() ) );    
    }

    //- PRIVATE

    private static final ResourceBundle bundle = ResourceBundle.getBundle(IdentityTargetSelector.class.getName());
    private final IdentityTargetable identityTargetable;
    private final IdentityTarget originalIdentityTarget;

    /**
     *
     */
    private static TargetMessageType getTargetMessageType( final Assertion assertion ) {
        TargetMessageType targetMessageType = TargetMessageType.REQUEST;

        if ( assertion instanceof MessageTargetable ) {
            targetMessageType = ((MessageTargetable)assertion).getTarget();
        } else {
            if ( Assertion.isResponse( assertion ) ) {
                targetMessageType = TargetMessageType.RESPONSE;
            }
        }

        return targetMessageType;
    }

    /**
     * 
     */
    private static IdentityTarget[] getIdentityTargetOptions( final Assertion policy,
                                                              final Assertion identityTargetableAssertion,
                                                              final TargetMessageType targetMessageType  ) {
        TreeSet<IdentityTarget> targetOptions = new TreeSet<IdentityTarget>();
        Iterator<Assertion> assertionIterator = policy.preorderIterator();

        while( assertionIterator.hasNext() ){
            Assertion assertion = assertionIterator.next();
            if ( assertion == identityTargetableAssertion ) {
                break;
            }
            if ( assertion instanceof IdentityAssertion &&
                 ((IdentityAssertion)assertion).getTarget()==targetMessageType ) {
                targetOptions.add( ((IdentityAssertion)assertion).getIdentityTarget() );
            }
        }

        return targetOptions.toArray(new IdentityTarget[targetOptions.size()]);
    }

}
