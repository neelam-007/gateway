package com.l7tech.external.assertions.generateoauthsignaturebasestring.server;

import com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeSource;
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
 * 1. parsing the authorization header<br />
 * 2. request parameters<br />
 * 3. 'manually' through the assertion fields
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
public class ServerGenerateOAuthSignatureBaseStringAssertion extends AbstractServerAssertion<GenerateOAuthSignatureBaseStringAssertion> {
    public ServerGenerateOAuthSignatureBaseStringAssertion(@NotNull final GenerateOAuthSignatureBaseStringAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        if (StringUtils.isBlank(assertion.getRequestUrl())) {
            throw new PolicyAssertionException(assertion, "Request Url cannot be null or empty.");
        }
        if (StringUtils.isBlank(assertion.getHttpMethod())) {
            throw new PolicyAssertionException(assertion, "Http method cannot be null or empty.");
        }
        if (StringUtils.isBlank(assertion.getVariablePrefix())) {
            throw new PolicyAssertionException(assertion, "Variable prefix cannot be null or empty");
        }
        this.timeSource = new TimeSource();
    }

    @Override
    public AssertionStatus checkRequest(@NotNull final PolicyEnforcementContext context) throws PolicyAssertionException {
        AssertionStatus assertionStatus = AssertionStatus.NONE;
        final Audit audit = getAudit();
        final Map<String, Object> variableMap = context.getVariableMap(assertion.getVariablesUsed(), audit);
        final String httpMethod = ExpandVariables.process(assertion.getHttpMethod(), variableMap, audit);
        if (StringUtils.isBlank(httpMethod)) {
            logAndAudit(AssertionMessages.OAUTH_MISSING_HTTP_METHOD);
            assertionStatus = AssertionStatus.FALSIFIED;
        } else {
            final String requestUrl = ExpandVariables.process(assertion.getRequestUrl(), variableMap, audit);
            try {
                final String encodedUrl = percentEncode(normalizeUrl(requestUrl));
                final TreeMap<String, String> sortedParameters = getSortedParameters(variableMap, audit, context);
                final String requestType = getRequestType(sortedParameters);
                validateParameters(sortedParameters);

                final StringBuilder stringBuilder = new StringBuilder(httpMethod);
                stringBuilder.append(AMPERSAND).append(encodedUrl).append(AMPERSAND);
                stringBuilder.append(encodeMap(sortedParameters));

                // set context variables
                context.setVariable(assertion.getVariablePrefix() + "." + SIG_BASE_STRING, stringBuilder.toString());
                context.setVariable(assertion.getVariablePrefix() + "." + REQUEST_TYPE, requestType);
                for (final Map.Entry<String, String> entry : sortedParameters.entrySet()) {
                    if (entry.getKey().startsWith("oauth_")) {
                        context.setVariable(assertion.getVariablePrefix() + "." + entry.getKey(), entry.getValue());
                    }
                }
            } catch (final DuplicateParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_DUPLICATE_PARAMETER, new String[]{e.getParameter()}, ExceptionUtils.getDebugException(e));
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final MissingRequiredParameterException e) {
                logAndAudit(AssertionMessages.OAUTH_MISSING_PARAMETER, new String[]{e.getParameter()}, ExceptionUtils.getDebugException(e));
                assertionStatus = AssertionStatus.FALSIFIED;
            } catch (final URISyntaxException e) {
                logAndAudit(AssertionMessages.OAUTH_INVALID_REQUEST_URL, new String[]{requestUrl}, ExceptionUtils.getDebugException(e));
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
    private String getRequestType(final TreeMap<String, String> sortedParameters) {
        String requestType;
        if (sortedParameters.get(OAUTH_TOKEN) != null) {
            if (sortedParameters.get(OAUTH_VERIFIER) != null) {
                requestType = AUTHORIZED_REQUEST_TOKEN;
            } else {
                requestType = ACCESS_TOKEN;
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
    private TreeMap<String, String> getSortedParameters(final Map<String, Object> variableMap, final Audit audit, final PolicyEnforcementContext context)
            throws IOException, NoSuchVariableException, DuplicateParameterException {
        final List<Pair<String, String>> unsortedParameters = new ArrayList<Pair<String, String>>();
        addHeaderParams(variableMap, audit, context, unsortedParameters);
        addRequestParams(unsortedParameters, context);
        addParams(unsortedParameters, getQueryString(variableMap));
        addManualParams(variableMap, audit, unsortedParameters);

        logAndAudit(AssertionMessages.OAUTH_PARAMETERS, unsortedParameters.toString());

        final TreeMap<String, String> sortedParameters = new TreeMap<String, String>();
        for (final Pair<String, String> parameter : unsortedParameters) {
            if (sortedParameters.containsKey(parameter.getKey()) && !sortedParameters.get(parameter.getKey()).equals(parameter.getValue())) {
                throw new DuplicateParameterException(parameter.getKey(), "Duplicate oauth parameter detected");
            }
            sortedParameters.put(parameter.getKey(), parameter.getValue());
        }
        // exclude realm and parameters according to the OAUTH 1.0 spec
        sortedParameters.remove(REALM);
        sortedParameters.remove(OAUTH_SIGNATURE);

        return sortedParameters;
    }

    /**
     * Add any parameters that are specified manually in the assertion.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addManualParams(final Map<String, Object> variableMap, final Audit audit, final List<Pair<String, String>> parameters) {
        if (assertion.isUseManualParameters()) {
            addManualParam(parameters, OAUTH_CALLBACK, assertion.getOauthCallback(), variableMap, audit);
            addManualParam(parameters, OAUTH_CONSUMER_KEY, assertion.getOauthConsumerKey(), variableMap, audit);
            addManualParam(parameters, OAUTH_NONCE, assertion.getOauthNonce(), variableMap, audit);
            addManualParam(parameters, OAUTH_SIGNATURE_METHOD, assertion.getOauthSignatureMethod(), variableMap, audit);
            addManualParam(parameters, OAUTH_TIMESTAMP, assertion.getOauthTimestamp(), variableMap, audit);
            addManualParam(parameters, OAUTH_TOKEN, assertion.getOauthToken(), variableMap, audit);
            addManualParam(parameters, OAUTH_VERIFIER, assertion.getOauthVerifier(), variableMap, audit);
            if (assertion.isUseOAuthVersion()) {
                addManualParam(parameters, OAUTH_VERSION, assertion.getOauthVersion(), variableMap, audit);
            }
        }
    }

    /**
     * Add any parameters detected in the Authorization header and set authHeader context variable.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addHeaderParams(final Map<String, Object> variableMap, final Audit audit, final PolicyEnforcementContext context, final List<Pair<String, String>> parameters) {
        if (assertion.isUseAuthorizationHeader() && assertion.getAuthorizationHeader() != null) {
            String authorizationHeader = ExpandVariables.process(assertion.getAuthorizationHeader(), variableMap, audit);
            if (!authorizationHeader.isEmpty()) {
                context.setVariable(assertion.getVariablePrefix() + "." + AUTH_HEADER, authorizationHeader);
                if (authorizationHeader.startsWith(OAUTH)) {
                    authorizationHeader = authorizationHeader.replaceFirst(OAUTH, StringUtils.EMPTY);
                }
                addParams(parameters, authorizationHeader);
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
            final HttpRequestKnob httpRequestKnob = targetMessage.getHttpRequestKnob();
            final Map parameterMap = httpRequestKnob.getParameterMap();
            for (final Object key : parameterMap.keySet()) {
                final String keyString = key.toString();
                parameters.add(new Pair<String, String>(keyString, httpRequestKnob.getParameter(keyString)));
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
            if (unexpandedValue.equals(AUTO)) {
                boolean alreadyExists = false;
                for (final Pair<String, String> parameter : parameters) {
                    if (name.equals(parameter.getKey())) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists && name.equals(OAUTH_TIMESTAMP)) {
                    final long currentTimeSeconds = timeSource.currentTimeMillis() / MILLIS_PER_SEC;
                    parameters.add(new Pair<String, String>(name, String.valueOf(currentTimeSeconds)));
                } else if (!alreadyExists && name.equals(OAUTH_NONCE)) {
                    parameters.add(new Pair<String, String>(name, generateNonce()));
                }
            } else {
                parameters.add(new Pair<String, String>(name, ExpandVariables.process(unexpandedValue, variableMap, audit)));
            }
        }
    }


    /**
     * Add any parameters detected in the parameter string.
     */
    @SuppressWarnings({"JavaDoc"})
    private void addParams(final List<Pair<String, String>> parameters, final String parameterString) {
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
                    if (splitString.length == 1) {
                        parameters.add(new Pair<String, String>(splitString[0].trim(), ""));
                    } else if (splitString.length == 2) {
                        parameters.add(new Pair<String, String>(splitString[0].trim(), deQuote(splitString[1].trim())));
                    }
                }
            }
        }
    }

    /**
     * Convert a parameter map to an encoded string.
     */
    @SuppressWarnings({"JavaDoc"})
    private String encodeMap(final Map<String, String> parameterMap) throws UnsupportedEncodingException {
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        for (final Map.Entry<String, String> entry : parameterMap.entrySet()) {
            sb.append(percentEncode(entry.getKey()));
            // equals symbol
            sb.append(EQUALS_ENCODED);
            sb.append(percentEncode(entry.getValue()));
            if (count != parameterMap.size() - 1) {
                // & symbol
                sb.append(AMPERSAND_ENCODED);
            }
            count++;
        }
        return sb.toString();
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

    private void validateParameters(final TreeMap<String, String> sortedParameters) throws MissingRequiredParameterException {
        for (final String requiredParameter : REQUIRED_PARAMETERS) {
            if (!sortedParameters.containsKey(requiredParameter) || StringUtils.isBlank(sortedParameters.get(requiredParameter))) {
                throw new MissingRequiredParameterException(requiredParameter, "Missing required oauth parameter");
            }
        }
    }

    static final String REQUEST_TOKEN = "request token";
    static final String AUTHORIZED_REQUEST_TOKEN = "authorized request token";
    static final String ACCESS_TOKEN = "access token";
    private static final String OAUTH_CALLBACK = "oauth_callback";
    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_NONCE = "oauth_nonce";
    private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
    private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    private static final String OAUTH_TOKEN = "oauth_token";
    private static final String OAUTH_VERIFIER = "oauth_verifier";
    private static final String OAUTH_VERSION = "oauth_version";
    private static final String REALM = "realm";
    private static final String OAUTH_SIGNATURE = "oauth_signature";
    private static final Integer MILLIS_PER_SEC = 1000;
    private static final String OAUTH = "OAuth";
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
    private TimeSource timeSource;

    static {
        REQUIRED_PARAMETERS = new ArrayList<String>(4);
        REQUIRED_PARAMETERS.add(OAUTH_CONSUMER_KEY);
        REQUIRED_PARAMETERS.add(OAUTH_SIGNATURE_METHOD);
        REQUIRED_PARAMETERS.add(OAUTH_TIMESTAMP);
        REQUIRED_PARAMETERS.add(OAUTH_NONCE);
    }
}
