package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.policy.assertion.ext.CustomAuditor;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.ServiceInvocation;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public void auditInfo( CustomAssertion customAssertion, String message) {
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

    public void register( ServiceInvocation si) {
        // Just pretend this looks like this -> si.setCustomAuditor(this)
        //
        // This works around the issue of calling a package private method from
        // a different classloader.
        try {
            Method method = ServiceInvocation.class.getDeclaredMethod("setCustomAuditor", new Class[]{CustomAuditor.class});
            method.setAccessible(true);
            method.invoke(si, new Object[]{this});
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error setting auditor for custom assertion.", e);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CustomAuditorImpl.class.getName());

    private final Auditor auditor;

    private String notNull(String text) {
        return text == null ? "" : text;
    }
}
