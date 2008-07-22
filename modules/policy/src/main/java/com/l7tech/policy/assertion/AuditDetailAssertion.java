package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.Syntax;

import java.util.logging.Level;

/**
 * Allows administrator to add audit details to the audit context of a message. Detail record
 * is added using the record id AssertionMessages.USERDETAIL.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertion extends Assertion implements UsesVariables {
    private String level = Level.INFO.toString();
    private String detail;

    public AuditDetailAssertion() {
    }

    public AuditDetailAssertion(final String detail) {
        setDetail(detail);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(detail);
    }
}
