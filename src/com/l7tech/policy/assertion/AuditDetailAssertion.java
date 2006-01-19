package com.l7tech.policy.assertion;

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
public class AuditDetailAssertion extends Assertion {
    private Level level = Level.INFO;
    private String detail;

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
