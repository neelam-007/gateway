package com.l7tech.server.policy.variable;

import com.l7tech.policy.variable.Syntax;

/**
 * Copyright CA Technologies - 2013
 * @author : yuri
 */
public class RadiusAuthenticationContextSelector implements ExpandVariables.Selector<RadiusAuthenticationContext>{
    @Override
    public Selection select(String contextName, RadiusAuthenticationContext context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(null == context) return null;// empty data

        return new Selection(context.getAttributeValue(name));
    }

    @Override
    public Class<RadiusAuthenticationContext> getContextObjectClass() {
        return RadiusAuthenticationContext.class;
    }
}
