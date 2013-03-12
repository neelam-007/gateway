package com.l7tech.external.assertions.jdbcquery;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.advice.DefaultAssertionAdvice;
import com.l7tech.policy.assertion.Assertion;

/**
 * This was created: 3/12/13 as 2:21 PM
 *
 * @author Victor Kazakov
 */
public class JdbcQueryAssertionAdvice extends DefaultAssertionAdvice<JdbcQueryAssertion> {

    @Override
    public void proceed(final PolicyChange pc) {
        Assertion[] assertions = pc.getEvent().getChildren();

        if (assertions == null || assertions.length != 1 || !(assertions[0] instanceof JdbcQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        JdbcQueryAssertion subject = (JdbcQueryAssertion) assertions[0];

        //default this to be unselected for new assertions.
        subject.setConvertVariablesToStrings(false);

        super.proceed(pc);
    }
}
