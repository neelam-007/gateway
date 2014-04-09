package com.l7tech.external.assertions.managecookie.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Either;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.*;

/**
 * Server side implementation of the ManageCookieAssertion.
 *
 * @see com.l7tech.external.assertions.managecookie.ManageCookieAssertion
 */
public class ServerManageCookieAssertion extends AbstractMessageTargetableServerAssertion<ManageCookieAssertion> {
    private final String[] variablesUsed;

    public ServerManageCookieAssertion(final com.l7tech.external.assertions.managecookie.ManageCookieAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        validateRequiredFields(assertion.getOperation());
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest(final PolicyEnforcementContext context, final Message message, final String messageDescription, final AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        switch (assertion.getOperation()) {
            case ADD:
                return doAdd(message, variableMap, false);
            case REMOVE:
                return doRemove(message, variableMap);
            case UPDATE:
                return doUpdate(message, variableMap);
            case ADD_OR_REPLACE:
                return doAdd(message, variableMap, true);
            default:
                throw new PolicyAssertionException(assertion, "Unsupported operation: " + assertion.getOperation());
        }
    }

    private AssertionStatus doUpdate(final Message message, final Map<String, Object> variableMap) {
        AssertionStatus status = AssertionStatus.NONE;
        final Either<Map<String, CookieCriteria>, String> result = expandCriteria(variableMap);
        if (result.isLeft()) {
            final List<HttpCookie> matchedCookies = new ArrayList<>();
            final List<HttpCookie> replacementCookies = new ArrayList<>();
            final HttpCookiesKnob cookiesKnob = message.getHttpCookiesKnob();
            for (final HttpCookie cookie : cookiesKnob.getCookies()) {
                if (matchAllCriteria(cookie, result.left())) {
                    matchedCookies.add(cookie);
                    final HttpCookie replacement = createCookie(cookie, variableMap);
                    if (replacement != null) {
                        replacementCookies.add(replacement);
                    } else {
                        // cookie couldn't be created
                        status = AssertionStatus.FALSIFIED;
                        break;
                    }
                }
            }
            if (status == AssertionStatus.NONE && matchedCookies.isEmpty()) {
                logAndAudit(AssertionMessages.COOKIES_NOT_MATCHED, result.left().toString());
            } else if (status == AssertionStatus.NONE) {
                removeCookiesFromKnob(matchedCookies, cookiesKnob);
                for (final HttpCookie replacementCookie : replacementCookies) {
                    cookiesKnob.addCookie(replacementCookie);
                    logAndAudit(AssertionMessages.COOKIE_ADDED, replacementCookie.getCookieName(), replacementCookie.getCookieValue());
                }
            }
        } else {
            logAndAudit(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE, result.right());
            status = AssertionStatus.FALSIFIED;
        }
        return status;
    }

    private AssertionStatus doRemove(final Message message, final Map<String, Object> variableMap) {
        AssertionStatus status = AssertionStatus.NONE;
        final Either<Map<String, CookieCriteria>, String> result = expandCriteria(variableMap);
        if (result.isLeft()) {
            if (!removeCookiesByCriteria(message, result.left())) {
                logAndAudit(AssertionMessages.COOKIES_NOT_MATCHED, result.left().toString());
            }
        } else {
            logAndAudit(AssertionMessages.EMPTY_COOKIE_ATTRIBUTE, result.right());
            status = AssertionStatus.FALSIFIED;
        }
        return status;
    }

    private AssertionStatus doAdd(final Message message, final Map<String, Object> variableMap, boolean replaceExisting) throws PolicyAssertionException {
        AssertionStatus status = AssertionStatus.NONE;
        final HttpCookie cookie = createCookie(variableMap);
        final HttpCookiesKnob cookiesKnob = message.getHttpCookiesKnob();
        if (cookie != null) {
            final List<HttpCookie> toRemove = new ArrayList<>();
            for (final HttpCookie existingCookie : cookiesKnob.getCookies()) {
                if (cookie.getCookieName().equals(existingCookie.getCookieName()) && ObjectUtils.equals(cookie.getDomain(), existingCookie.getDomain()) && ObjectUtils.equals(cookie.getPath(), existingCookie.getPath())) {
                    if (replaceExisting) {
                        toRemove.add(existingCookie);
                    } else {
                        logAndAudit(AssertionMessages.COOKIE_ALREADY_EXISTS, cookie.getCookieName(), cookie.getDomain(), cookie.getPath());
                        status = AssertionStatus.FALSIFIED;
                    }
                }
            }
            if (status == AssertionStatus.NONE) {
                removeCookiesFromKnob(toRemove, cookiesKnob);
                cookiesKnob.addCookie(cookie);
                logAndAudit(AssertionMessages.COOKIE_ADDED, cookie.getCookieName(), cookie.getCookieValue());
            }
        } else {
            status = AssertionStatus.FALSIFIED;
        }
        return status;
    }

    /**
     * Create a cookie from the assertion.
     *
     * @param variableMap the variable map to use for context variable resolution.
     * @return the created cookie or null if a cookie could not be created.
     */
    private HttpCookie createCookie(final Map<String, Object> variableMap) {
        return createCookie(null, variableMap);
    }

    private HttpCookie createCookie(final HttpCookie existingCookie, final Map<String, Object> variableMap) {
        HttpCookie cookie = null;
        final String name = getAttributeValue(NAME, existingCookie, variableMap);
        if (StringUtils.isNotBlank(name)) {
            final String value = getAttributeValue(VALUE, existingCookie, variableMap);
            final String path = getAttributeValue(PATH, existingCookie, variableMap);
            final String domain = getAttributeValue(DOMAIN, existingCookie, variableMap);
            final String maxAge = getAttributeValue(MAX_AGE, existingCookie, variableMap);
            final String comment = getAttributeValue(COMMENT, existingCookie, variableMap);
            final String secure = getAttributeValue(SECURE, existingCookie, variableMap);
            final String httpOnly = getAttributeValue(HTTP_ONLY, existingCookie, variableMap);
            final String versionStr = getAttributeValue(VERSION, existingCookie, variableMap);
            Integer version = null;
            try {
                version = Integer.valueOf(versionStr);
            } catch (final NumberFormatException e) {
                logAndAudit(AssertionMessages.INVALID_COOKIE_VERSION, versionStr);
            }
            if (version != null) {
                try {
                    final Integer maxAgeInt = StringUtils.isBlank(maxAge) ? -1 : Integer.valueOf(maxAge);
                    cookie = new HttpCookie(name, value, version, StringUtils.isBlank(path) ? null : path,
                            StringUtils.isBlank(domain) ? null : domain, maxAgeInt, Boolean.parseBoolean(secure),
                            StringUtils.isBlank(comment) ? null : comment, Boolean.parseBoolean(httpOnly));
                } catch (final NumberFormatException e) {
                    logAndAudit(AssertionMessages.INVALID_COOKIE_MAX_AGE, maxAge);
                }
            }
        } else {
            logAndAudit(AssertionMessages.EMPTY_COOKIE_NAME);
        }
        return cookie;
    }

    private boolean useExistingValue(final String attributeName) {
        return assertion.getCookieAttributes().containsKey(attributeName) && assertion.getCookieAttributes().get(attributeName).isUseOriginalValue();
    }

    private String getAttributeValue(final String attributeName, final HttpCookie existingCookie, final Map<String, Object> variableMap) {
        String attributeValue = null;
        if (existingCookie != null && useExistingValue(attributeName)) {
            if (attributeName.equals(NAME)) {
                attributeValue = existingCookie.getCookieName();
            } else if (attributeName.equals(VALUE)) {
                attributeValue = existingCookie.getCookieValue();
            } else if (attributeName.equals(DOMAIN)) {
                attributeValue = existingCookie.getDomain();
            } else if (attributeName.equals(PATH)) {
                attributeValue = existingCookie.getPath();
            } else if (attributeName.equals(VERSION)) {
                attributeValue = String.valueOf(existingCookie.getVersion());
            } else if (attributeName.equals(MAX_AGE)) {
                attributeValue = String.valueOf(existingCookie.getMaxAge());
            } else if (attributeName.equals(COMMENT)) {
                attributeValue = existingCookie.getComment();
            } else if (attributeName.equals(SECURE)) {
                attributeValue = String.valueOf(existingCookie.isSecure());
            } else if (attributeName.equals(HTTP_ONLY)) {
                attributeValue = String.valueOf(existingCookie.isHttpOnly());
            } else {
                logger.log(Level.WARNING, "Unknown cookie attribute: " + attributeName);
            }
        } else if (assertion.getCookieAttributes().containsKey(attributeName)) {
            attributeValue = ExpandVariables.process(assertion.getCookieAttributes().get(attributeName).getValue(), variableMap, getAudit());
        }
        return attributeValue;
    }

    private void validateRequiredFields(final ManageCookieAssertion.Operation operation) throws PolicyAssertionException {
        if (operation == ManageCookieAssertion.Operation.ADD || operation == Operation.ADD_OR_REPLACE) {
            final Map<String, ManageCookieAssertion.CookieAttribute> cookieAttributes = assertion.getCookieAttributes();
            if (!cookieAttributes.containsKey(ManageCookieAssertion.NAME)) {
                throw new PolicyAssertionException(assertion, "Missing cookie name");
            }
            if (!cookieAttributes.containsKey(ManageCookieAssertion.VALUE)) {
                throw new PolicyAssertionException(assertion, "Missing cookie value");
            }
            if (!cookieAttributes.containsKey(ManageCookieAssertion.VERSION)) {
                throw new PolicyAssertionException(assertion, "Missing cookie version");
            }
        }

        if (operation == Operation.UPDATE && assertion.getCookieAttributes().isEmpty()) {
            throw new PolicyAssertionException(assertion, "No cookie attributes specified for update cookie");
        }

        if ((operation == Operation.UPDATE || operation == Operation.REMOVE) && assertion.getCookieCriteria().isEmpty()) {
            throw new PolicyAssertionException(assertion, "No cookie criteria specified for " + operation.getName().toLowerCase() + " cookie");
        }
    }

    /**
     * Remove cookies which match the given cookie criteria from the context.
     *
     * @param message          the message which contains cookies to remove.
     * @param expandedCriteria the cookie attribute criteria which should already have any referenced context variables expanded.
     * @return true if at least one cookie was removed.
     */
    private boolean removeCookiesByCriteria(final Message message, final Map<String, CookieCriteria> expandedCriteria) {
        boolean atLeastOneRemoved = false;
        final List<HttpCookie> cookiesToRemove = new ArrayList<>();
        final HttpCookiesKnob cookiesKnob = message.getHttpCookiesKnob();
        for (final HttpCookie cookie : cookiesKnob.getCookies()) {
            if (matchAllCriteria(cookie, expandedCriteria)) {
                cookiesToRemove.add(cookie);
                atLeastOneRemoved = true;
            }
        }
        removeCookiesFromKnob(cookiesToRemove, cookiesKnob);
        return atLeastOneRemoved;
    }

    private void removeCookiesFromKnob(final List<HttpCookie> cookiesToRemove, final HttpCookiesKnob cookiesKnob) {
        for (final HttpCookie cookie : cookiesToRemove) {
            cookiesKnob.deleteCookie(cookie);
            logAndAudit(AssertionMessages.COOKIE_REMOVED, cookie.getCookieName(), cookie.getCookieValue());
        }
    }

    /**
     * Determine whether a given cookie matches all cookie attribute criteria.
     *
     * @param cookie           the HttpCookie in question.
     * @param expandedCriteria the map of cookie criteria which has any referenced context variables already expanded.
     * @return true if the cookie matches all attribute criteria.
     */
    private boolean matchAllCriteria(final HttpCookie cookie, final Map<String, CookieCriteria> expandedCriteria) {
        for (final Map.Entry<String, CookieCriteria> entry : expandedCriteria.entrySet()) {
            if (!matchSingleCriteria(cookie, entry.getKey(), entry.getValue().getValue(), entry.getValue().isRegex())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine whether a given cookie matches a single attribute criteria.
     *
     * @param cookie         the HttpCookie in question.
     * @param attributeName  the attribute name to match.
     * @param attributeValue the attribute value to match.
     * @param regex          true if the attribute value should be treated as a regular expression.
     * @return true if the given cookie matches the single attribute criteria.
     */
    private boolean matchSingleCriteria(final HttpCookie cookie, final String attributeName, final String attributeValue, final boolean regex) {
        boolean match = false;
        if (attributeName.equals(ManageCookieAssertion.NAME)) {
            match = regex ? Pattern.compile(attributeValue).matcher(cookie.getCookieName()).matches() : cookie.getCookieName().equals(attributeValue);
        } else if (attributeName.equals(ManageCookieAssertion.DOMAIN)) {
            final String cookieDomain = cookie.getDomain();
            if (cookieDomain != null) {
                match = regex ? Pattern.compile(attributeValue).matcher(cookieDomain).matches() : cookieDomain.equals(attributeValue);
            }
        } else if (attributeName.equals(ManageCookieAssertion.PATH)) {
            final String cookiePath = cookie.getPath();
            if (cookiePath != null) {
                match = regex ? Pattern.compile(attributeValue).matcher(cookiePath).matches() : cookiePath.equals(attributeValue);
            }
        }
        return match;
    }

    /**
     * Expand any context variables referenced in the cookie criteria.
     *
     * @param variableMap the context variable map.
     * @return an expanded map of cookie criteria or a string (error scenario) representing the name of a criteria attribute which is or resolved to empty.
     */
    private Either<Map<String, CookieCriteria>, String> expandCriteria(final Map<String, Object> variableMap) {
        final Map<String, CookieCriteria> expandedCriteria = new HashMap<>();
        for (final Map.Entry<String, ManageCookieAssertion.CookieCriteria> entry : assertion.getCookieCriteria().entrySet()) {
            final String expandedValue = ExpandVariables.process(entry.getValue().getValue(), variableMap, getAudit());
            if (StringUtils.isNotBlank(expandedValue)) {
                expandedCriteria.put(entry.getKey(), new CookieCriteria(entry.getKey(), expandedValue, entry.getValue().isRegex()));
            } else {
                // criteria value is or was resolved as empty
                return Either.right(entry.getKey());
            }
        }
        return Either.left(expandedCriteria);
    }
}
