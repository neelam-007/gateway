package com.l7tech.server.policy;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

public class PolicyAssertionRbacCheckerImpl implements PolicyAssertionRbacChecker {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    @Named("rbacServices")
    private RbacServices rbacServices;

    @Inject
    @Named("assertionAccessManager")
    private AssertionAccessManager assertionAccessManager;

    @Override
    public void checkPolicy(@Nullable Policy policy) throws FindException, PermissionDeniedException, IOException {
        if (policy != null) {
            if (policy.getXml() != null && policy.getXml().length() > 0) {
                checkPolicy(policy.getAssertion());
            }
        }
    }

    void checkPolicy(@Nullable Assertion rootAssertion) throws FindException, PermissionDeniedException {
        final User user = JaasUtils.getCurrentUser();
        if (user != null) {
            checkPolicy(user, rootAssertion);
        } else {
            // TODO currently fails open, but this is the convention adopted by other users of JaasUtils..
        }
    }

    /*
     * Check if all assertions are allowed for the user.  An assertion is allowed if the user has READ permission
     * on an AssertionAccess instance created for the assertion.
     */
    void checkPolicy(final @NotNull User user, @Nullable Assertion rootAssertion) throws FindException, PermissionDeniedException {
        final PermissionDeniedException[] permErr = { null };
        final FindException[] findErr = { null };

        PolicyUtil.visitDescendantsAndSelf(rootAssertion, new Functions.UnaryVoid<Assertion>() {
            @Override
            public void call(Assertion assertion) {
                final AssertionAccess access = assertionAccessManager.getAssertionAccessCached(assertion);
                try {
                    if (!rbacServices.isPermittedForEntity(user, access, OperationType.READ, null)) {
                        if (permErr[0] == null)
                            permErr[0] = new PermissionDeniedException(OperationType.READ, access, null);
                    }
                } catch (FindException e) {
                    if (findErr[0] == null)
                        findErr[0] = e;
                }
            }
        });

        if (findErr[0] != null)
            throw findErr[0];

        if (permErr[0] != null)
            throw permErr[0];
    }
}
