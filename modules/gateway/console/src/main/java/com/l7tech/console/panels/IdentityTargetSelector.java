package com.l7tech.console.panels;

import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.console.util.Registry;

import java.awt.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for selection of a target identity.
 */
public class IdentityTargetSelector extends OkCancelDialog<IdentityTarget> {

    //- PUBLIC

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion  ) {
        this( owner, readOnly, identityTargetableAssertion, identityTargetableAssertion.getPath()[0], getMessageTargetable(identityTargetableAssertion) );
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion,
                                   final MessageTargetable messageTargetable ) {
        this( owner, readOnly, identityTargetableAssertion, identityTargetableAssertion.getPath()[0], messageTargetable );
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion identityTargetableAssertion,
                                   final Assertion policy,
                                   final MessageTargetable messageTargetable ) {
        this( owner,
              readOnly,
              (IdentityTargetable)identityTargetableAssertion,
              getIdentityTargetOptions(policy, identityTargetableAssertion, messageTargetable));
    }

    public IdentityTargetSelector( final Window owner,
                                   final boolean readOnly,
                                   final Assertion context,
                                   final IdentityTargetable identityTargetable ) {
        this( owner,
              readOnly,
              identityTargetable,
              getIdentityTargetOptions(context.getPath()[0], context, getMessageTargetable(context)));
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

    private static final Logger logger = Logger.getLogger( IdentityTargetSelector.class.getName() );
    private static final ResourceBundle bundle = ResourceBundle.getBundle(IdentityTargetSelector.class.getName());
    private final IdentityTargetable identityTargetable;
    private final IdentityTarget originalIdentityTarget;

    /**
     *
     */
    private static MessageTargetable getMessageTargetable( final Assertion assertion ) {
        MessageTargetable messageTargetable = new MessageTargetableSupport(TargetMessageType.REQUEST);

        if ( assertion instanceof MessageTargetable ) {
            messageTargetable = (MessageTargetable) assertion;
        } else {
            if ( Assertion.isResponse( assertion ) ) {
                messageTargetable.setTarget( TargetMessageType.RESPONSE );
            }
        }

        return messageTargetable;
    }

    /**
     * Find options for the identity that preceed the given assertion in the
     * policy for the same target message.
     */
    private static IdentityTarget[] getIdentityTargetOptions( final Assertion policy,
                                                              final Assertion identityTargetableAssertion,
                                                              final MessageTargetable messageTargetable  ) {
        TreeSet<IdentityTarget> targetOptions = new TreeSet<IdentityTarget>();
        Iterator<Assertion> assertionIterator = inlineIncludes(policy).preorderIterator();

        while( assertionIterator.hasNext() ){
            Assertion assertion = assertionIterator.next();
            if ( assertion == identityTargetableAssertion ) {
                break;
            }

            if ( !assertion.isEnabled() ) {
                continue; 
            }

            if ( assertion instanceof IdentityAssertion &&
                 AssertionUtils.isSameTargetMessage( identityTargetableAssertion, messageTargetable ) ) {
                targetOptions.add( ((IdentityAssertion)assertion).getIdentityTarget() );
            }

            if ( assertion instanceof IdentityTagable &&
                 AssertionUtils.isSameTargetMessage( identityTargetableAssertion, messageTargetable ) &&
                 ((IdentityTagable)assertion).getIdentityTag() != null ) {
                targetOptions.add( new IdentityTarget(((IdentityTagable)assertion).getIdentityTag()) );
            }
        }

        return targetOptions.toArray(new IdentityTarget[targetOptions.size()]);
    }

    /**
     *  
     */
    private static Assertion inlineIncludes( final Assertion subject ) {
        Assertion assertion = subject;

        try {
            assertion = Registry.getDefault().getPolicyPathBuilderFactory().makePathBuilder().inlineIncludes( subject, new HashSet<String>(), false );
        } catch (InterruptedException e) {
            // fallback to policy without includes
            logger.log( Level.WARNING, "Error inlining inlcluded policy fragments.", e );
        } catch (PolicyAssertionException e) {
            // fallback to policy without includes
            logger.log( Level.WARNING, "Error inlining inlcluded policy fragments.", e );
        }

        return assertion;
    }

}
