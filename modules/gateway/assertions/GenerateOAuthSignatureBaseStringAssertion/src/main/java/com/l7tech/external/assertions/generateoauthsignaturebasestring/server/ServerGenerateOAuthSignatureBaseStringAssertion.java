package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;

import static com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion.*;

/**
 * Server side logic which generates an OAuth signature base string that conforms to the OAuth 1.0 spec.
 * <p/>
 * See http://tools.ietf.org/html/rfc5849.
 * <p/>
 * There are three ways that this assertion can retrieve oauth parameters in order to build the signature base string:<br />
 * 1. parsing the authorization header (UsageMode.SERVER only)<br />
 * 2. request parameters (UsageMode.SERVER only)<br />
 * 3. 'manually' through the assertion fields (UsageMode.CLIENT only)
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class ServerGenerateOAuthSignatureBaseStringAssertion extends AbstractServerAssertion<GenerateOAuthSignatureBaseStringAssertion> {
    public ServerGenerateOAuthSignatureBaseStringAssertion(@NotNull final GenerateOAuthSignatureBaseStringAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        throwIfNullOrBlank(assertion.getRequestUrl(), "Request Url");
        throwIfNullOrBlank(assertion.getHttpMethod(), "Http method");
        throwIfNullOrBlank(assertion.getVariablePrefix(), "Variable prefix");
        if (assertion.getUsageMode() == null) {
            throw new PolicyAssertionException(assertion, "Usage mode cannot be null");
        }
        this.timeSource = new TimeSource();
    }

    @Override
    public AssertionStatus checkRequest(@NotNull final PolicyEnforcementContext context) throws PolicyAssertionException {
        AssertionStatus assertionStatus = AssertionStatus.NONE;
        final Audit audit = getAudit();
        final Map<String, Object> variableMap = context.getVariableMap(assertion.getVariablesUsed(), audit);
        final String httpMethod = ExpandVariables.process(assertion.getHttpMethod(), variableMap, audit).toUpperCase();
        if (StringUtils.isBlank(httpMethod)) {
            logAndAudit(AssertionMessages.OAUTH_MISSING_HTTP_METHOD);
            context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Missing http method");
            assertionStatus = AssertionStatus.FALSIFIED;
        } else {
            final String requestUrl = ExpandVariables.process(assertion.getRequestUrl(), variableMap, audit);
            try {
                final String encodedUrl = percentEncode(normalizeUrl(requestUrl));
                final TreeMap<String, List<String>> sortedParameters = getSortedParameters(variableMap, audit, context);
                validateParameters(sortedParameters);
                final String requestType = getRequestType(sortedParameters);

                final StringBuilder stringBuilder = new StringBuilder(httpMethod);
                stringBuilder.append(AMPERSAND).append(encodedUrl).append(AMPERSAND);
                stringBuilder.append(buildEncodedParamString(sortedParameters));

                // set context variables
                context.setVariable(assertion.getVariablePrefix() + "." + SIG_BASE_STRING, stringBuilder.toString());
                context.setVariable(assertion.getVariablePrefix() + "." + REQUEST_TYPE, requestType);
                for (final Map.Entry<String, List<String>> entry : sortedParameters.entrySet()) {
                    if (entry.getKey().startsWith("oauth_")) {
                        context.setVariable(assertion.getVariablePrefix() + "." + entry.getKey(), entry.getValue().iterator().next());
                    }
                }
            } catch (final DuplicateParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_DUPLICATE_PARAMETER, new String[]{e.getParameter(), StringUtils.join(e.getDuplicateValues(), ",")}, ExceptionUtils.getDebugException(e));
                context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Duplicate oauth parameter: " + e.getParameter());
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final MissingRequiredParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_MISSING_PARAMETER, new String[]{e.getParameter()}, ExceptionUtils.getDebugException(e));
                context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Missing " + e.getParameter());
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final InvalidParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_INVALID_PARAMETER, new String[]{e.getParameter(), e.getInvalidValue()}, ExceptionUtils.getDebugException(e));
                context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Invalid " + e.getParameter() + ": " + e.getInvalidValue());
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final ParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_INVALID_QUERY_PARAMETER, new String[]{e.getParameter()}, ExceptionUtils.getDebugException(e));
                context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Query parameter " + e.getParameter() + " is not allowed");
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final URISyntaxException e) {
                logAndAudit(AssertionMessages.OAUTH_INVALID_REQUEST_URL, new String[]{requestUrl}, ExceptionUtils.getDebugException(e));
                context.setVariable(assertion.getVariablePrefix() + "." + ERROR, "Invalid request url: " + requestUrl);
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final UnsupportedEncodingException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error encoding signature base string: " + e.getMessage()},
                        ExceptionUtils.getDebugException(e));
                assertionStatus = AssertionStatus.FAILED;
            } catch (final NoSuchVariableException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to retrieve variable " +
                        e.getVariable() + ": " + e.getMessage()}, ExceptionUtils.getDebugException(e));
                assertionStatus = AssertionStatus.FAILED;
            } catch (final IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                assertionStatus = AssertionStatus.FAILED;
            }
        }
        return assertionStatus;
    }

    /**
     * Performs the OAuth 1.0 specific encoding of parameters
     *
     * @param input the String to percent encode.
     * @return the percent encoded input.
     * @throws java.io.UnsupportedEncodingException
     *          if cannot encode.
     */
    String percentEncode(final String input) throws UnsupportedEncodingException {
        return URLEncoder.encode(input, UTF_8)
                // OAuth encodes some characters differently:
                .replace(PLUS, PLUS_ENCODED).replace(ASTERISK, ASTERISK_ENCODED)
                .replace(TILDE_ENCODED, TILDE);
    }

    /**
     * Isolate static call for unit tests.
     *
     * @return an generated nonce.
     */
    String generateNonce() {
        return UUID.randomUUID().toString();
    }

    void setTimeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    /**
     * Determine the request type based on which parameters are present.
     *
     * @param sortedParameters the oauth parameters.
     * @return the request type based on the oauth parameters present.
     */
    private String getRequestType(final TreeMap<String, List<String>> sortedParameters) {
        String requestType;
        final List<String> oauthToken = sortedParameters.get(OAUTH_TOKEN);
        if (oauthToken != null) {
            if (sortedParameters.get(OAUTH_VERIFIER) != null) {
                requestType = AUTHORIZED_REQUEST_TOKEN;
            } else if (!oauthToken.iterator().next().isEmpty()) {
                requestType = ACCESS_TOKEN;
            } else {
                requestType = REQUEST_TOKEN;
            }
        } else {
            requestType = REQUEST_TOKEN;
        }
        return requestType;
    }

    /**
     * Retrieve all parameters sorted alphabetically.
     *
     * @param variableMap
     * @param audit
     * @param context
     * @return a map of oauth parameters sorted alphabetically.
     * @throws IOException             if unable to retrieve request parameters.
     * @throws NoSuchVariableException if the request target message cannot be retrieved.
     */
    @SuppressWarnings({"JavaDoc"})
    private TreeMap<String, List<String>> getSortedParameters(final Map<String, Object> variableMap, final Audit audit, final PolicyEnforcementContext context)
            throws IOException, NoSuchVariableException, DuplicateParameterException, ParameterException {
        final List<Pair<String, String>> unsortedParameters = new ArrayList<Pair<String, String>>();
        addParams(unsortedParameters, getQueryString(variableMap), assertion.isAllowCustomOAuthQueryParams());
        switch (assertion.getUsageMode()) {
            case CLIENT:
                addManualParams(variableMap, audit, unsortedParameters);
                break;
            case SERVER:
                addHeaderParams(variableMap, audit, context, unsortedParameters);
                addRequestParams(unsortedParameters, context);
        }

        logAndAudit(AssertionMessages.OAUTH_PARAMETERS, unsortedParameters.toString());

        final TreeMap<String, List<String>> sortedParameters = new TreeMap<String, List<String>>();
        for (final Pair<String, String> parameter : unsortedParameters) {
            if (!sortedParameters.containsKey(parameter.getKey())) {
                sortedParameters.put(parameter.getKey(), new ArrayList<String>());
            }
            sortedParameters.get(parameter.getKey()).add(parameter.getValue());
        }
        // exclude realm and parameters according to the OAUTH 1.0 spec
        sortedParameters.remove(REALM);
        sortedParameters.remove(OAUTH_SIGNATURE);

        for (final List<String> paramValues : sortedParameters.values()) {
            Collections.sort(paramValues);
        }

        return sortedParameters;
    }

    /**
     * Add any parameters that are specified manually in the assertion.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addManualParams(final Map<String, Object> variableMap, final Audit audit, final List<Pair<String, String>> parameters) {
        if (assertion.getUsageMode().equals(UsageMode.CLIENT)) {
            addManualParam(parameters, OAUTH_CALLBACK, assertion.getOauthCallback(), variableMap, audit);
            addManualParam(parameters, OAUTH_CONSUMER_KEY, assertion.getOauthConsumerKey(), variableMap, audit);
            addManualParam(parameters, OAUTH_NONCE, generateNonce());
            addManualParam(parameters, OAUTH_SIGNATURE_METHOD, assertion.getOauthSignatureMethod(), variableMap, audit);
            addManualParam(parameters, OAUTH_TIMESTAMP, String.valueOf(timeSource.currentTimeMillis() / MILLIS_PER_SEC));
            addManualParam(parameters, OAUTH_TOKEN, assertion.getOauthToken(), variableMap, audit);
            addManualParam(parameters, OAUTH_VERIFIER, assertion.getOauthVerifier(), variableMap, audit);
            if (assertion.isUseOAuthVersion()) {
                addManualParam(parameters, OAUTH_VERSION, OAUTH_1_0, variableMap, audit);
            }
        }
    }

    /**
     * Add any parameters detected in the Authorization header and set authHeader context variable.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addHeaderParams(final Map<String, Object> variableMap, final Audit audit,
                                 final PolicyEnforcementContext context, final List<Pair<String, String>> parameters) throws ParameterException {
        if (assertion.isUseAuthorizationHeader() && assertion.getAuthorizationHeader() != null) {
            String authorizationHeader = ExpandVariables.process(assertion.getAuthorizationHeader(), variableMap, audit);
            if (!authorizationHeader.isEmpty() && authorizationHeader.toLowerCase().startsWith(OAUTH)) {
                context.setVariable(assertion.getVariablePrefix() + "." + AUTH_HEADER, authorizationHeader);
                authorizationHeader = authorizationHeader.replaceFirst("(oauth|OAuth)", StringUtils.EMPTY);
                addParams(parameters, authorizationHeader, true);
            }
        }
    }

    /**
     * Add any parameters detected in the request.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addRequestParams(final List<Pair<String, String>> parameters, final PolicyEnforcementContext context) throws NoSuchVariableException, IOException {
        if (assertion.isUseMessageTarget()) {
            final Message targetMessage = context.getTargetMessage(assertion.getMessageTargetableSupport());
            final HttpServletRequestKnob httpRequestKnob = targetMessage.getKnob(HttpServletRequestKnob.class);
            final String contentType = httpRequestKnob.getHeaderFirstValue("Content-Type");
            if (contentType != null) {
                final Map<String, String[]> parameterMap = httpRequestKnob.getRequestBodyParameterMap();
                // parameters retrieved from request knob will be decoded
                for (final Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    // duplicates are checked later
                    for (int i = 0; i < entry.getValue().length; i++) {
                        // re-encode the parameter
                        parameters.add(new Pair<String, String>(entry.getKey(), percentEncode(entry.getValue()[i])));
                    }
                }
            }
        }
    }

    /**
     * Get the query string, stripping out anything before the ? symbol.
     */
    @SuppressWarnings({"JavaDoc"})
    private String getQueryString(final Map<String, Object> variableMap) {
        String queryString = null;
        if (assertion.getQueryString() != null) {
            queryString = ExpandVariables.process(assertion.getQueryString(), variableMap, getAudit());
            final int index = queryString.indexOf(QUESTION);
            if (index != -1) {
                queryString = queryString.substring(index);
            }
        }
        return queryString;
    }

    private void addManualParam(final List<Pair<String, String>> parameters, final String name, final String unexpandedValue, final Map<String, Object> variableMap, final Audit audit) {
        if (unexpandedValue != null) {
            addManualParam(parameters, name, ExpandVariables.process(unexpandedValue, variableMap, audit));
        }
    }

    private void addManualParam(final List<Pair<String, String>> parameters, final String name, final String value) {
        if (value != null) {
            if (!value.isEmpty() || REQUIRED_PARAMETERS.contains(name)) {
                parameters.add(new Pair<String, String>(name, value));
            }
        }
    }


    /**
     * Add any parameters detected in the parameter string.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addParams(final List<Pair<String, String>> parameters, final String parameterString,
                           final boolean allowUnrecognizedOAuthParams) throws ParameterException {
        if (parameterString != null && parameterString.contains(EQUALS)) {
            String params = parameterString;
            // remove leading '/' from parameterString, if any
            if (params.startsWith("/")) {
                params = params.substring(1);
            }
            for (String s : Arrays.asList(params.split("[&,]"))) {
                if (s.startsWith(QUESTION)) {
                    s = s.substring(1);
                }
                String[] splitString = s.split(EQUALS, 2);
                if (splitString[0].length() > 0) {
                    final String parameterName = splitString[0].trim();
                    if (!allowUnrecognizedOAuthParams && parameterName.startsWith(OAUTH_PREFIX) &&
                            !OAUTH_PARAMETERS.contains(parameterName)) {
                        throw new ParameterException(parameterName, "Unrecognized oauth parameter: " + parameterName);
                    }
                    if (splitString.length == 1) {
                        parameters.add(new Pair<String, String>(parameterName, ""));
                    } else if (splitString.length == 2) {
                        parameters.add(new Pair<String, String>(parameterName, deQuote(splitString[1].trim())));
                    }
                }
            }
        }
    }

    /**
     * Convert a parameter map to an encoded string.
     */
    @SuppressWarnings({"JavaDoc"})
    private String buildEncodedParamString(final TreeMap<String, List<String>> parameterMap) throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, List<String>> entry : parameterMap.entrySet()) {
            final Iterator<String> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                sb.append(percentEncode(entry.getKey()));
                // equals symbol
                sb.append(EQUALS_ENCODED);
                sb.append(percentEncode(iterator.next()));
                sb.append(AMPERSAND_ENCODED);
            }
        }
        return sb.substring(0, sb.length() - AMPERSAND_ENCODED.length());
    }

    /**
     * Generates a normalized URI according to the OAUTH 1.0 spec
     */
    @SuppressWarnings({"JavaDoc"})
    private String normalizeUrl(final String baseUrl) throws URISyntaxException {
        final URI uri = new URI(baseUrl);
        if (uri.getScheme() == null) {
            throw new URISyntaxException(baseUrl, "Missing scheme");
        }
        if (uri.getAuthority() == null) {
            throw new URISyntaxException(baseUrl, "Missing authority");
        }
        final String scheme = uri.getScheme().toLowerCase();
        String authority = uri.getAuthority().toLowerCase();
        final boolean dropPort = (scheme.equals(HTTP) && uri.getPort() == 80)
                || (scheme.equals(HTTPS) && uri.getPort() == 443);
        if (dropPort) {
            // find the last : in the authority
            final int index = authority.lastIndexOf(":");
            if (index >= 0) {
                authority = authority.substring(0, index);
            }
        }
        String path = uri.getRawPath();
        if (path == null || path.length() <= 0) {
            path = "/"; // conforms to RFC 2616 section 3.2.2
        }
        // we know that there is no query and no fragment here.
        return scheme + "://" + authority + path;
    }

    private String deQuote(final String value) {
        String result = value;
        if (result != null) {
            result = result.trim();
            if (result.startsWith("\"") && result.endsWith("\"")) {
                return result.substring(1, result.length() - 1);
            }
        }
        return result;
    }

    private void validateParameters(final TreeMap<String, List<String>> sortedParameters) throws DuplicateParameterException, MissingRequiredParameterException, InvalidParameterException {
        for (final String requiredParameter : REQUIRED_PARAMETERS) {
            if (!sortedParameters.containsKey(requiredParameter) || sortedParameters.get(requiredParameter).isEmpty() ||
                    StringUtils.isBlank(sortedParameters.get(requiredParameter).iterator().next())) {
                throw new MissingRequiredParameterException(requiredParameter, "Missing required oauth parameter");
            }
        }
        for (final Map.Entry<String, List<String>> entry : sortedParameters.entrySet()) {
            // oauth parameters cannot be specified more than once
            if (OAUTH_PARAMETERS.contains(entry.getKey()) && entry.getValue().size() > 1) {
                throw new DuplicateParameterException(entry.getKey(), new ArrayList<String>(entry.getValue()), "Duplicate oauth parameter detected");
            }
        }
        // token is required if there is a verifier
        if (sortedParameters.containsKey(OAUTH_VERIFIER) && !sortedParameters.containsKey(OAUTH_TOKEN)) {
            throw new MissingRequiredParameterException(OAUTH_TOKEN, "Missing required oauth parameter");
        }
        // callback is required if there is no token or token is empty
        if ((!sortedParameters.containsKey(OAUTH_TOKEN) || sortedParameters.get(OAUTH_TOKEN).get(0).isEmpty()) && !sortedParameters.containsKey(OAUTH_CALLBACK)) {
            throw new MissingRequiredParameterException(OAUTH_CALLBACK, "Missing required oauth parameter");
        }
        // version is not required
        final String foundVersion = sortedParameters.get(OAUTH_VERSION) != null ? sortedParameters.get(OAUTH_VERSION).iterator().next() : null;
        if (foundVersion != null && !OAUTH_1_0.equals(foundVersion)) {
            throw new InvalidParameterException(OAUTH_VERSION, foundVersion, OAUTH_VERSION + " must be " + OAUTH_1_0 + " but found: " + foundVersion);
        }
        // signature method is required
        final String foundSignatureMethod = sortedParameters.get(OAUTH_SIGNATURE_METHOD).iterator().next();
        if (!SIGNATURE_METHODS.contains(foundSignatureMethod.toUpperCase())) {
            throw new InvalidParameterException(OAUTH_SIGNATURE_METHOD, foundSignatureMethod, OAUTH_SIGNATURE_METHOD + " is invalid: " + foundSignatureMethod);
        }
        // callback must be oob or start with http/https and be a max of 200 characters
        if (sortedParameters.get(OAUTH_CALLBACK) != null) {
            final String foundCallback = sortedParameters.get(OAUTH_CALLBACK).iterator().next();
            if (!foundCallback.matches("oob|http[s]?[^\"]{1,200}")) {
                throw new InvalidParameterException(OAUTH_CALLBACK, foundCallback, OAUTH_CALLBACK + " is invalid: " + foundCallback);
            }
        }
        // timestamp must be positive integer
        final String foundTimestamp = sortedParameters.get(OAUTH_TIMESTAMP).iterator().next();
        final Option<Integer> option = ConversionUtils.getTextToIntegerConverter().call(foundTimestamp);
        if (!option.isSome() || option.some() < 0) {
            throw new InvalidParameterException(OAUTH_TIMESTAMP, foundTimestamp, OAUTH_TIMESTAMP + " is invalid: " + foundTimestamp);
        }
    }

    private void throwIfNullOrBlank(final String toTest, final String fieldName) throws PolicyAssertionException {
        if (StringUtils.isBlank(toTest)) {
            throw new PolicyAssertionException(assertion, fieldName + " cannot be null or empty");
        }
    }

    private static final String OAUTH_PREFIX = "oauth_";
    private static final String OAUTH_CALLBACK = "oauth_callback";
    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_NONCE = "oauth_nonce";
    private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    private static final String OAUTH_TOKEN = "oauth_token";
    private static final String OAUTH_VERIFIER = "oauth_verifier";
    private static final String OAUTH_VERSION = "oauth_version";
    private static final String OAUTH_SIGNATURE = "oauth_signature";
    private static final String REALM = "realm";
    private static final Integer MILLIS_PER_SEC = 1000;
    private static final String OAUTH = "oauth";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String UTF_8 = "UTF-8";
    private static final String EQUALS = "=";
    private static final String EQUALS_ENCODED = "%3D";
    private static final String AMPERSAND = "&";
    private static final String AMPERSAND_ENCODED = "%26";
    private static final String PLUS = "+";
    private static final String PLUS_ENCODED = "%20";
    private static final String ASTERISK = "*";
    private static final String ASTERISK_ENCODED = "%2A";
    private static final String TILDE = "~";
    private static final String TILDE_ENCODED = "%7E";
    private static final String QUESTION = "?";
    private static final List<String> REQUIRED_PARAMETERS;
    private static final List<String> OAUTH_PARAMETERS;
    private static final List<String> SIGNATURE_METHODS;
    private TimeSource timeSource;

    /**
     * These oauth parameters are required regardless of CLIENT/SERVER or request type.
     */
    static {
        REQUIRED_PARAMETERS = new ArrayList<String>(4);
        REQUIRED_PARAMETERS.add(OAUTH_CONSUMER_KEY);
        REQUIRED_PARAMETERS.add(OAUTH_SIGNATURE_METHOD);
        REQUIRED_PARAMETERS.add(OAUTH_TIMESTAMP);
        REQUIRED_PARAMETERS.add(OAUTH_NONCE);

        OAUTH_PARAMETERS = new ArrayList<String>(REQUIRED_PARAMETERS);
        OAUTH_PARAMETERS.add(OAUTH_CALLBACK);
        OAUTH_PARAMETERS.add(OAUTH_TOKEN);
        OAUTH_PARAMETERS.add(OAUTH_VERIFIER);
        OAUTH_PARAMETERS.add(OAUTH_VERSION);
        OAUTH_PARAMETERS.add(OAUTH_SIGNATURE);

        SIGNATURE_METHODS = new ArrayList<String>(3);
        SIGNATURE_METHODS.add(HMAC_SHA1);
        SIGNATURE_METHODS.add(RSA_SHA1);
        SIGNATURE_METHODS.add(PLAINTEXT);
    }
}
