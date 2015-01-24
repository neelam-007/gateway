package com.l7tech.policy.validator;

import com.l7tech.objectmodel.GuidBasedEntityManager;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;
import com.l7tech.policy.assertion.CurrentInterfaceDescription;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.wsdl.WsdlUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * The policy validator that analyzes the policy assertion tree
 * and collects the errors.
 * <p/>
 * Errors are collected in the PolicyValidatorResult instance.
 * <p/>
 * The expected order is:
 * <ul>
 * <li><i>Pre conditions</i> such as ssl, and ip address range (optional)
 * <li><i>Credential location</i> such as ssl, and ip address range (optional)
 * <li><i>Access control, identity, group membership</i> (optional), if present
 * expects the credential finder precondition
 * <li><i>Routing</i> (optional), if present expects the credential finder
 * precondition
 * </ul>
 * <p/>
 * The class methods are not synchronized.
 * <p/>
 * 
 * @author Emil Marceta
 */
public class DefaultPolicyValidator extends AbstractPolicyValidator {
    static Logger log = Logger.getLogger(DefaultPolicyValidator.class.getName());

    public DefaultPolicyValidator(GuidBasedEntityManager<Policy> policyFinder, PolicyPathBuilderFactory pathBuilderFactory) {
        super(policyFinder, pathBuilderFactory);
    }

    @Override
    public PolicyValidatorResult validate(@Nullable Assertion assertion, PolicyValidationContext pvc, AssertionLicense assertionLicense) throws InterruptedException {
        PolicyValidatorResult r = super.validate(assertion, pvc, assertionLicense);

        if (pvc.isSoap() && Assertion.contains(assertion, XpathBasedAssertion.class, true) && WsdlUtil.isRPCWithNoSchema(pvc.getWsdl())) {
            Assertion lastAssertion = assertion;
            if (assertion instanceof CompositeAssertion) {
                List children = ((CompositeAssertion) assertion).getChildren();
                if (children != null && !children.isEmpty()) {
                    lastAssertion = (Assertion) children.get(children.size()-1);
                }
            }

            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, "Assertions that use XPaths may not work as expected with RPC services.", null));
        }

        return r;
    }

    @Override
    public void validatePath(final AssertionPath ap,
                             final PolicyValidationContext pvc,
                             final AssertionLicense assertionLicense,
                             final PolicyValidatorResult r)
            throws InterruptedException
    {
        try {
            CurrentInterfaceDescription.doWithInterfaceDescription( pvc.getInterfaceDescription(), new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    doValidatePath( ap, pvc, assertionLicense, r );
                    return null;
                }
            } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    public void doValidatePath(final AssertionPath ap,
                             final PolicyValidationContext pvc,
                             final AssertionLicense assertionLicense,
                             final PolicyValidatorResult r)
            throws InterruptedException
    {
        Assertion[] path = ap.getPath();

        // paths that have the pattern "OR, Comment" should be ignored completely (Bugzilla #2449)
        for (Assertion assertion: path) {
            if (assertion instanceof CommentAssertion || !assertion.isEnabled()) {
                if (assertion.getParent() instanceof OneOrMoreAssertion) {
                    // if the parent OR has something else than comments as children, the this path should be ignored
                    OneOrMoreAssertion parent = (OneOrMoreAssertion) assertion.getParent();
                    boolean onlyItInOR = true;
                    if (Thread.interrupted())
                        throw new InterruptedException();
                    for (Iterator parit = parent.children(); parit.hasNext();) {
                        Assertion child = (Assertion)parit.next();
                        if (!(child instanceof CommentAssertion) && child.isEnabled()) {
                            onlyItInOR = false;
                            break;
                        }
                    }
                    if (!onlyItInOR) {
                        log.fine("Path " + ap + " ignored for validation purposes");
                        return;
                    }
                }
            }
        }

        PathValidator pv = new PathValidator(ap, pvc, assertionLicense, r);
        for (Assertion assertion : path) {
            if (assertion instanceof CommentAssertion || !assertion.isEnabled()) continue;
            pv.validate(assertion);
        }

        // deferred validations
        for (DeferredValidate dv : pv.getDeferredValidators()) {
            if (Thread.interrupted()) throw new InterruptedException();
            dv.validate(pv, path);
        }

        // last assertion should be last non-comment and enabled assertion
        Assertion lastAssertion = ap.lastAssertion();
        for (int i = path.length-1; i >= 0; i--) {
            Assertion ass = path[i];
            if (!(ass instanceof CommentAssertion) && ass.isEnabled()) {
                lastAssertion = ass;
                break;
            }
        }

        // If there is an interface and it requires output variables, complain if they are not set or are explicitly the wrong type
        EncapsulatedAssertionConfig idesc = pvc.getInterfaceDescription();
        if ( null != idesc ) {
            Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf( path[0] );
            for ( EncapsulatedAssertionResultDescriptor result : idesc.getResultDescriptors() ) {
                VariableMetadata vm = varsSet.get( result.getResultName() );
                if ( null == vm ) {
                    r.addWarning( new PolicyValidatorResult.Warning( lastAssertion, "Output variable is never given a value: " + result.getResultName(), null ) );
                } else {
                    DataType type = vm.getType();
                    DataType wantType = result.dataType();
                    if ( type != null && wantType != null && !DataType.UNKNOWN.equals( type ) && !DataType.UNKNOWN.equals( wantType ) && !wantType.equals( type ) ) {
                        r.addWarning( new PolicyValidatorResult.Warning( lastAssertion, "Output variable '" + result.getResultName() + "' is expected to be of type " + wantType + " but was last set to type " + type, null ) );
                    }
                }
            }
        }

        if ( !pvc.getPolicyType().isServicePolicy() ) {
            // All subsequent rules pertain only to Service policies (i.e. not fragments)
            return;
        }

        if (!pv.seenResponse) { // no routing report that
            r.addWarning(new PolicyValidatorResult.
              Warning(lastAssertion, "No route assertion.", null));
        }
        if (!pv.seenParsing) {
            if (!pvc.isSoap()) {
                r.addWarning(new PolicyValidatorResult.
                  Warning(lastAssertion, "This path potentially allows non-xml content through.", null));
            }
        }
        if (!pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE, PathValidator.REQUEST_TARGET_NAME) && pv.seenResponse) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion,
                    "No credential assertion is present in the policy. The" +
              " service may be exposed to public access", null));
        }
        if (pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE, PathValidator.REQUEST_TARGET_NAME) && !pv.seenAccessControl(PathValidator.REQUEST_TARGET_NAME) && pv.seenResponse) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, "Credentials are collected but not authenticated." +
              " This service may be exposed to public access.", null));
        }
        if (pv.seenCredentials(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE, PathValidator.RESPONSE_TARGET_NAME) && !pv.seenAccessControl(PathValidator.RESPONSE_TARGET_NAME) && pv.seenResponse) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, "Response credentials are collected but not authenticated.", null));
        }
    }

    /**
     * The implementations are invoked after the regular (sequential)
     * validate of the assertion path. This is useful for unordered validations,
     * that is, where some assertion must be present but not necessarily
     * before the assertion currently examined.
     */
    static interface DeferredValidate {
        void validate(PathValidator pv, Assertion[] path);
    }
}
