package com.l7tech.policy.assertion;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public abstract class InjectionThreatProtectionAssertion extends MessageTargetableAssertion {
    final static AssertionNodeNameFactory<InjectionThreatProtectionAssertion> policyNameFactory =
            new AssertionNodeNameFactory<InjectionThreatProtectionAssertion>() {
        @Override
        public String getAssertionName(final InjectionThreatProtectionAssertion assertion,
                                       final boolean decorate) {
            if (!decorate) return assertion.getBaseName();

            StringBuilder sb = new StringBuilder(assertion.getBaseName());

            sb.append(" [");

            if (assertion.includeUrlPath) {
                sb.append("URL Path");

                if (assertion.includeUrlQueryString) {
                    sb.append(" + URL Query String");
                }

                if (assertion.includeBody) {
                    sb.append(" + Body");
                }
            } else if (assertion.includeUrlQueryString) {
                sb.append("URL Query String");

                if (assertion.includeBody) {
                    sb.append(" + Body");
                }
            } else if (assertion.includeBody) {
                sb.append("Body");
            }

            sb.append("]");

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    /** Whether to apply protections to request URL path. */
    protected boolean includeUrlPath;
    /** Whether to apply protections to request URL query string. */
    protected boolean includeUrlQueryString;
    /** Whether to apply protections to request body. */
    protected boolean includeBody;

    public InjectionThreatProtectionAssertion(TargetMessageType targetMessageType,
                                              boolean targetModifiedByGateway) {
        super(targetMessageType, targetModifiedByGateway);
    }

    public boolean isIncludeUrlPath() {
        return includeUrlPath;
    }

    public void setIncludeUrlPath(boolean includeUrlPath) {
        this.includeUrlPath = includeUrlPath;
    }

    public boolean isIncludeUrlQueryString() {
        return includeUrlQueryString;
    }

    public void setIncludeUrlQueryString(boolean includeUrlQueryString) {
        this.includeUrlQueryString = includeUrlQueryString;
    }

    public boolean isIncludeBody() {
        return includeBody;
    }

    public void setIncludeBody(boolean includeBody) {
        this.includeBody = includeBody;
    }

    protected abstract String getBaseName();
}
