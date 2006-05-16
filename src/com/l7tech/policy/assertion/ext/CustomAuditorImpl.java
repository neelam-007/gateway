package com.l7tech.policy.assertion.ext;

import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;

/**
 * An auditor for Custom Assertions.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class CustomAuditorImpl implements CustomAuditor {

    //- PUBLIC

    public CustomAuditorImpl(Auditor auditor) {
        if (auditor == null) throw new IllegalArgumentException("auditor must not be null");
        this.auditor = auditor;
    }

    public void auditInfo(CustomAssertion customAssertion, String message) {
        if (customAssertion != null && message != null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_INFO,
                    new String[]{notNull(customAssertion.getName()), message});
        }
    }

    public void auditWarning(CustomAssertion customAssertion, String message) {
        if (customAssertion != null && message != null) {
            auditor.logAndAudit(AssertionMessages.CUSTOM_ASSERTION_WARN,
                    new String[]{notNull(customAssertion.getName()), message});
        }
    }

    public void register(ServiceInvocation si) {
        si.setCustomAuditor(this);
    }

    //- PRIVATE

    private final Auditor auditor;

    private String notNull(String text) {
        return text == null ? "" : text;
    }
}
