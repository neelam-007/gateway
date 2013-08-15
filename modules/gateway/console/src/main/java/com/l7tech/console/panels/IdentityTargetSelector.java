package com.l7tech.console.panels;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestIdentityTargetable;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;

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

        if ( assertion instanceof RequestIdentityTargetable ) {
            // then the request is the target
        } else if ( assertion instanceof MessageTargetable ) {
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
        final TreeSet<IdentityTarget> targetOptions = new TreeSet<IdentityTarget>();
        final Iterator<Assertion> assertionIterator = inlineIncludes(policy).preorderIterator();
        final Map<Goid,String> identityProviderNameMap = new HashMap<Goid,String>();

        while( assertionIterator.hasNext() ){
            Assertion assertion = assertionIterator.next();
            if ( assertion == identityTargetableAssertion ) {
                break;
            }

            if ( !assertion.isEnabled() ) {
                continue; 
            }

            if ( assertion instanceof IdentityTagable &&
                 AssertionUtils.isSameTargetMessage( assertion, messageTargetable ) &&
                 ((IdentityTagable)assertion).getIdentityTag() != null ) {
                targetOptions.add( new IdentityTarget(((IdentityTagable)assertion).getIdentityTag()) );
            } else if ( assertion instanceof IdentityAssertion &&
                 AssertionUtils.isSameTargetMessage( assertion, messageTargetable ) ) {
                targetOptions.add( providerName(
                        ((IdentityAssertion)assertion).getIdentityTarget(),
                        identityProviderNameMap) );
            }
        }

        return targetOptions.toArray(new IdentityTarget[targetOptions.size()]);
    }

    /**
     * Load provider name if required 
     */
    private static IdentityTarget providerName( final IdentityTarget identityTarget,
                                                final Map<Goid,String> identityProviderNameMap ) {
        if ( identityTarget.needsIdentityProviderName() ) {
            Goid providerOid = identityTarget.getIdentityProviderOid();
            String name = identityProviderNameMap.get( providerOid );
            if ( name == null ) {
                try {
                    IdentityAdmin ia = Registry.getDefault().getIdentityAdmin();
                    IdentityProviderConfig config
                            = ia.findIdentityProviderConfigByID( providerOid );
                    if ( config != null ) {
                        name = config.getName();
                        identityProviderNameMap.put( providerOid, name );
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Error loading provider name for '#"+providerOid+"'.", e);
                } catch (IllegalStateException ise) {
                    logger.log(Level.WARNING, "Identity admin not available when loading provider name for '#"+providerOid+"'.");
                }
            }
            if ( name != null ) {
                identityTarget.setIdentityProviderName( name );
            }
        }

        return identityTarget;
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
