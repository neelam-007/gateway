package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.wsdl.Wsdl;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager.doAsSystem;

/**
 * Helper class for implementing policy updates & validation
 *
 * @author rraquepo
 */
public class PolicyHelper {

    PolicyHelper(@NotNull final ApplicationContext context) {
        this(context.getBean("policyManager", PolicyManager.class),
                context.getBean("policyVersionManager", PolicyVersionManager.class),
                context.getBean("transactionManager", PlatformTransactionManager.class),
                context.getBean("licenseManager", AssertionLicense.class),
                context.getBean("serverPolicyValidator", PolicyValidator.class));
    }

    PolicyHelper(@NotNull final PolicyManager policyManager,
                 @NotNull final PolicyVersionManager policyVersionManager,
                 @NotNull final PlatformTransactionManager transactionManager,
                 @NotNull final AssertionLicense licenseManager,
                 @NotNull final PolicyValidator policyValidator) {
        this.policyManager = policyManager;
        this.policyVersionManager = policyVersionManager;
        this.transactionManager = transactionManager;
        this.licenseManager = licenseManager;
        this.policyValidator = policyValidator;
    }

    private final PolicyManager policyManager;
    private final PolicyVersionManager policyVersionManager;
    private final PlatformTransactionManager transactionManager;
    private final AssertionLicense licenseManager;
    private final PolicyValidator policyValidator;

    public OperationResult updatePolicy(@NotNull final String guid, @NotNull final String policyXml, final String userLogin) {
        final OperationResult result = new OperationResult("Error");
        final TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                try {
                    OperationResult validateResult = validatePolicy(guid, policyXml);
                    result.setPolicyValidationResult(validateResult.getPolicyValidationResult());
                    if (validateResult.hasError()) {
                        result.setResult(validateResult.result);
                    } else {
                        final Policy policy = policyManager.findByGuid(guid);
                        if (policy == null) {
                            throw new FindException();
                        }
                        policy.setXml(policyXml);
                        doAsSystem(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                PolicyVersion policyVersion = policyVersionManager.checkpointPolicy(policy, true, true);
                                if (userLogin != null && userLogin.length() > 0) {
                                    policyVersion.setUserLogin(userLogin);
                                    policyVersionManager.save(policyVersion);
                                }
                                return null;
                            }
                        });
                        result.setResult(null);
                    }
                } catch (FindException e) {
                    final String message = "GUID " + guid + " does not exist";
                    result.setResult(message);
                } catch (ObjectModelException e) {
                    final String message = "Error Updating fragment - " + guid + " : " + e.getMessage();
                    result.setResult(message);
                    transactionStatus.setRollbackOnly();
                }
            }
        });
        return result;
    }

    public OperationResult validatePolicy(@NotNull final String guid, @NotNull final String policyXml) throws ResourceAccessException {
        final OperationResult operationResult = new OperationResult("Validator Error(s)");
        try {
            final Policy policyCheck = policyManager.findByGuid(guid);
            if (policyCheck == null) {
                throw new FindException();
            }
            policyCheck.setXml(policyXml);
            policyCheck.forceRecompile();
            final Assertion assertion = policyCheck.getAssertion();
            // Run the validator
            final PolicyValidatorResult result;
            try {
                Wsdl wsdl = null;//just because
                result = policyValidator.validate(assertion, new com.l7tech.policy.validator.PolicyValidationContext(policyCheck.getType(), policyCheck.getInternalTag(), policyCheck.getInternalSubTag(), wsdl, false, null), licenseManager);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResourceAccessException(e);
            }
            // Process the results, ensure duplicates are removed
            final Set<PolicyValidationResult.PolicyValidationMessage> messages = new LinkedHashSet<PolicyValidationResult.PolicyValidationMessage>();
            PolicyValidationResult.ValidationStatus status = PolicyValidationResult.ValidationStatus.OK;
            for (final PolicyValidatorResult.Message message : result.getMessages()) {
                final PolicyValidationResult.PolicyValidationMessage pvm = ManagedObjectFactory.createPolicyValidationMessage();
                pvm.setAssertionOrdinal(message.getAssertionOrdinal());
                if (message instanceof PolicyValidatorResult.Error) {
                    status = PolicyValidationResult.ValidationStatus.ERROR;
                    pvm.setLevel("Error");
                } else {
                    if (status == PolicyValidationResult.ValidationStatus.OK) {
                        status = PolicyValidationResult.ValidationStatus.WARNING;
                    }
                    pvm.setLevel("Warning");
                }
                pvm.setMessage(message.getMessage());

                final List<PolicyValidationResult.AssertionDetail> details = new ArrayList<PolicyValidationResult.AssertionDetail>();
                Assertion current = assertion;
                for (final Integer position : message.getAssertionIndexPath()) {
                    final PolicyValidationResult.AssertionDetail detail = ManagedObjectFactory.createAssertionDetail();
                    detail.setPosition(position);

                    Assertion child = null;
                    if (current instanceof CompositeAssertion) {
                        child = ((CompositeAssertion) current).getChildren().get(position);
                    } else if (current instanceof Include) {
                        final Include include = (Include) current;
                        Policy policy = getIncludePolicy(include);
                        child = getPolicyAssertionChild(policy, position);
                    }
                    current = child;

                    if (current != null) {
                        detail.setDescription(current.meta().<String>get(AssertionMetadata.WSP_EXTERNAL_NAME) + " (" + current.meta().<String>get(AssertionMetadata.PALETTE_NODE_NAME) + ")");
                    } else {
                        detail.setDescription("");
                    }

                    details.add(detail);
                }
                pvm.setAssertionDetails(details);

                messages.add(pvm);
            }

            final PolicyValidationResult pvr = ManagedObjectFactory.createPolicyValidationResult();
            pvr.setStatus(status);
            if (!messages.isEmpty()) {
                pvr.setPolicyValidationMessages(new ArrayList<PolicyValidationResult.PolicyValidationMessage>(messages));
            }
            if (PolicyValidationResult.ValidationStatus.ERROR != pvr.getStatus()) {
                operationResult.setResult("");
            }
            operationResult.setPolicyValidationResult(pvr);
        } catch (FindException e) {
            final String message = "GUID " + guid + " does not exist";
            operationResult.setResult(message);
        } catch (IOException e) {
            final String message = "IO errors Invalid Policy : " + e.getMessage();
            operationResult.setResult(message);
        }
        return operationResult;
    }

    private Policy getIncludePolicy(final Include include) {
        Policy policy = include.retrieveFragmentPolicy();

        if (policy == null) {
            try {
                policy = policyManager.findByGuid(include.getPolicyGuid());
            } catch (FindException e) {
                throw new ResourceAccessException(e);
            }
            include.replaceFragmentPolicy(policy);
        }

        return policy;
    }

    private Assertion getPolicyAssertionChild(final Policy policy,
                                              final int position) {
        Assertion child = null;

        if (policy != null) {
            try {
                final Assertion assertion = policy.getAssertion();
                final CompositeAssertion compositeAssertion = assertion instanceof CompositeAssertion ?
                        (CompositeAssertion) assertion :
                        null;
                if (compositeAssertion != null && compositeAssertion.getChildren().size() > position) {
                    child = compositeAssertion.getChildren().get(position);
                }
            } catch (IOException e) {
                // continue but don't include details
            }
        }

        return child;
    }

    private User getUser() {
        return JaasUtils.getCurrentUser();
    }

    /**
     * Class for OperationResult
     */
    static class OperationResult {
        public OperationResult(String result) {
            this.result = result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }

        public boolean hasError() {
            if ((result != null && result.length() > 0) ||
                    (policyValidationResult != null && policyValidationResult.getStatus() == PolicyValidationResult.ValidationStatus.ERROR)) {
                return true;
            }
            return false;
        }

        public PolicyValidationResult getPolicyValidationResult() {
            return policyValidationResult;
        }

        public void setPolicyValidationResult(PolicyValidationResult policyValidationResult) {
            this.policyValidationResult = policyValidationResult;
        }

        private String result;
        private PolicyValidationResult policyValidationResult;

    }

    /**
     * Runtime exception for unexpected resource access errors
     */
    static class ResourceAccessException extends RuntimeException {
        public ResourceAccessException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public ResourceAccessException(final String message) {
            super(message);
        }

        public ResourceAccessException(final Throwable cause) {
            super(cause);
        }
    }
}
