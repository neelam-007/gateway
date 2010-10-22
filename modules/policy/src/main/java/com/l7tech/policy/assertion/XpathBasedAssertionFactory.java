package com.l7tech.policy.assertion;

import com.l7tech.util.Functions;

/**
 * A utility ASSERTION_FACTORY for XpathBasedAssertions that nulls any default XpathExpression value
 * (likely present for policy deserialization compatibility purposes) so that it can be populated with something
 * more appropriate by the AddXPathAssertionAdvice.
 */
public class XpathBasedAssertionFactory<AT extends XpathBasedAssertion> implements Functions.Unary<AT, AT> {
    private final Class<AT> assClass;

    /**
     * @param assClass the concrete class for this assertion factory (ie, RequestXpathAssertion.class).
     */
    public XpathBasedAssertionFactory(Class<AT> assClass) {
        this.assClass = assClass;
    }

    @Override
    public AT call(AT assertion) {
        try {
            AT ret = assClass.newInstance();
            ret.setXpathExpression(null);
            return ret;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
