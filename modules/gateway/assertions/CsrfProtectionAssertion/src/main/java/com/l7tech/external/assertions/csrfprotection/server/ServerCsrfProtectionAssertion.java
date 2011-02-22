package com.l7tech.external.assertions.csrfprotection.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.csrfprotection.HttpParameterType;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.external.assertions.csrfprotection.CsrfProtectionAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
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
            auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The request was not an HTTP request, failing assertion.");
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
                            auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: Multiple cookie values were detected, failing assertion.");
                            return AssertionStatus.FAILED;
                        } else {
                            cookieValue = cookie.getCookieValue();
                        }
                    }
                }

                if(cookieValue == null) {
                    auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: No cookie value was detected, failing assertion.");
                    return AssertionStatus.FAILED;
                }
            } else {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: No cookie values were detected, failing assertion.");
                return AssertionStatus.FAILED;
            }

            // Try to get the parameter value
            String paramValue = null;
            if(assertion.getParameterType() == HttpParameterType.GET && requestKnob.getMethod() != HttpMethod.GET) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: Looking for a GET parameter, but the request was not a GET request, failing assertion.");
                return AssertionStatus.FAILED;
            }
            if(assertion.getParameterType() == HttpParameterType.POST && requestKnob.getMethod() != HttpMethod.POST) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: Looking for a POST parameter, but the request was not a POST request, failing assertion.");
                return AssertionStatus.FAILED;
            }
            String[] paramValues = requestKnob.getParameterValues(assertion.getParameterName());
            if(paramValues == null || paramValues.length == 0) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The parameter was not found, failing assertion.");
                return AssertionStatus.FAILED;
            } else if(paramValues.length > 1) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The parameter had more than one value, failing assertion.");
                return AssertionStatus.FAILED;
            }
            paramValue = paramValues[0];

            if(!cookieValue.equals(paramValue)) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The parameter did not match the cookie value, failing assertion.");
                return AssertionStatus.FAILED;
            }
        }

        if(assertion.isEnableHttpRefererChecking()) {
            String values[] = requestKnob.getHeaderValues("Referer");

            String referer = null;
            if(!assertion.isAllowEmptyReferer() && (values == null || values.length == 0)) {
                auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The HTTP-Referer header was not provided but it is required, failing assertion.");
                return AssertionStatus.FAILED;
            } else if(values != null && values.length > 0) {
                if(values.length > 1) {
                    auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The HTTP-Referer header was provided multiple times, failing assertion.");
                    return AssertionStatus.FAILED;
                }

                referer = values[0];
            }

            // Empty referer values are valid at this point. If not empty, then validate the value
            if(referer != null) {
                String domain = null;
                try {
                    URL url = new URL(requestKnob.getRequestURL(), referer);
                    domain = url.getHost();
                } catch(MalformedURLException e) {
                    auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The HTTP-Referer header was not a valid URL, failing assertion.");
                    return AssertionStatus.FAILED;
                }

                if(assertion.isOnlyAllowCurrentDomain()) {
                    String localDomain = requestKnob.getRequestURL().getHost();

                    if(!localDomain.equals(domain)) {
                        auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The HTTP-Referer header valid was not valid, failing assertion.");
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
                        auditor.logAndAudit(AssertionMessages.USERDETAIL_WARNING, "CSRF Protection: The HTTP-Referer header was not valid, failing assertion.");
                        return AssertionStatus.FAILED;
                    }
                }
            }
        }

        return AssertionStatus.NONE;
    }
}
