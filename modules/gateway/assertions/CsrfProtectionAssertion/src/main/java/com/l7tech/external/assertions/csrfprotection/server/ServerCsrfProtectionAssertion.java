package com.l7tech.external.assertions.csrfprotection.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion;
import com.l7tech.external.assertions.csrfprotection.HttpParameterType;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Server side implementation of the CsrfProtectionAssertion.
 *
 * @see com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion
 */
public class ServerCsrfProtectionAssertion extends AbstractServerAssertion<CsrfProtectionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCsrfProtectionAssertion.class.getName());

    private final CsrfProtectionAssertion assertion;
    private final Auditor auditor;

    public ServerCsrfProtectionAssertion(CsrfProtectionAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        HttpRequestKnob requestKnob = null;
        try {
            requestKnob = context.getRequest().getHttpRequestKnob();
        } catch(IllegalStateException e) {
            auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_REQUEST_NOT_HTTP);
            return AssertionStatus.FALSIFIED;
        }

        if(assertion.isEnableDoubleSubmitCookieChecking()) {
            // Try to get the cookie value
            String cookieValue = null;
            HttpCookie[] cookies = requestKnob.getCookies();
            if(cookies != null) {
                for(HttpCookie cookie : cookies) {
                    if(assertion.getCookieName().equals(cookie.getCookieName())) {
                        if(cookieValue != null && !cookieValue.equals(cookie.getCookieValue())) {
                            auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_MULTIPLE_COOKIE_VALUES);
                            return AssertionStatus.FAILED;
                        } else {
                            cookieValue = cookie.getCookieValue();
                        }
                    }
                }

                if(cookieValue == null) {
                    auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_NO_COOKIE_VALUE);
                    return AssertionStatus.FAILED;
                }
            } else {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_NO_COOKIE_VALUE);
                return AssertionStatus.FAILED;
            }

            // Try to get the parameter value
            String paramValue = null;
            if(assertion.getParameterType() == HttpParameterType.GET && requestKnob.getMethod() != HttpMethod.GET) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_WRONG_REQUEST_TYPE, "GET");
                return AssertionStatus.FAILED;
            }
            if(assertion.getParameterType() == HttpParameterType.POST && requestKnob.getMethod() != HttpMethod.POST) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_WRONG_REQUEST_TYPE, "POST");
                return AssertionStatus.FAILED;
            }
            if(assertion.getParameterType() == HttpParameterType.GET_AND_POST
                    && (requestKnob.getMethod() != HttpMethod.GET && requestKnob.getMethod() != HttpMethod.POST)) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_WRONG_REQUEST_TYPE, "GET or POST");
                return AssertionStatus.FAILED;
            }
            String[] paramValues = requestKnob.getParameterValues(assertion.getParameterName());
            if(paramValues == null || paramValues.length == 0) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_NO_PARAMETER);
                return AssertionStatus.FAILED;
            } else if(paramValues.length > 1) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_MULTIPLE_PARAMETER_VALUES);
                return AssertionStatus.FAILED;
            }
            paramValue = paramValues[0];

            if(!cookieValue.equals(paramValue)) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_COOKIE_PARAMETER_MISMATCH);
                return AssertionStatus.FAILED;
            }

            // context variable set to value of cookie (or parameter, at this point both are the same)
            context.setVariable(CsrfProtectionAssertion.CTX_VAR_NAME_CSRF_VALID_TOKEN, cookieValue);
        }

        if(assertion.isEnableHttpRefererChecking()) {
            String values[] = requestKnob.getHeaderValues("Referer");

            String referer = null;
            if(!assertion.isAllowEmptyReferer() && (values == null || values.length == 0)) {
                auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_MISSING_REFERER);
                return AssertionStatus.FAILED;
            } else if(values != null && values.length > 0) {
                if(values.length > 1) {
                    auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_MULTIPLE_REFERERS);
                    return AssertionStatus.FAILED;
                }

                referer = values[0];
            }

            // Empty referer values are valid at this point. If not empty, then validate the value
            if(!StringUtils.isEmpty(referer)) {
                String domain = null;
                try {
                    URL url = new URL(requestKnob.getRequestURL(), referer);
                    domain = url.getHost();
                } catch(MalformedURLException e) {
                    auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_INVALID_REFERER);
                    return AssertionStatus.FAILED;
                }

                if(assertion.isOnlyAllowCurrentDomain()) {
                    String localDomain = requestKnob.getRequestURL().getHost();

                    if(!localDomain.equals(domain)) {
                        auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_INVALID_REFERER);
                        return AssertionStatus.FAILED;
                    }
                } else {
                    boolean valid = false;
                    for(String allowedDomain : assertion.getTrustedDomains()) {
                        if(domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain)) {
                            valid = true;
                            break;
                        }
                    }

                    if(!valid) {
                        auditor.logAndAudit(AssertionMessages.CSRF_PROTECTION_INVALID_REFERER);
                        return AssertionStatus.FAILED;
                    }
                }
            }
        }

        return AssertionStatus.NONE;
    }
}
