package com.l7tech.proxy.util;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.protocol.DomainIdStatusCode;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.util.SyspropUtil;
import com.l7tech.security.socket.LocalTcpPeerIdentifier;

import javax.mail.internet.MimeUtility;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.io.UnsupportedEncodingException;

/**
 * Encapsulates configuration of domain ID injection feature.
 */
public class DomainIdInjector {
    private static final Logger logger = Logger.getLogger(DomainIdInjector.class.getName());

    public static final String PROP_INJECTION_ENABLED = "com.l7tech.proxy.domainidlookup.enable";
    public static final String PROP_INJECT_ALWAYS = "com.l7tech.proxy.domainidlookup.always";
    public static final String PROP_HEADER_USERNAME = "com.l7tech.proxy.domainidlookup.user.name";
    public static final String PROP_HEADER_DOMAIN = "com.l7tech.proxy.domainidlookup.domain.name";
    public static final String PROP_HEADER_PROGRAM = "com.l7tech.proxy.processlookup.name";

    static boolean INJECT_ALWAYS = SyspropUtil.getBoolean(PROP_INJECT_ALWAYS, false);
    static boolean INJECTION_ENABLED = INJECT_ALWAYS || SyspropUtil.getBoolean(PROP_INJECTION_ENABLED, true);

    static String HEADER_USERNAME = SyspropUtil.getString(PROP_HEADER_USERNAME, "X-Injected-User-Name");
    static String HEADER_DOMAIN = SyspropUtil.getString(PROP_HEADER_DOMAIN, "X-Injected-Domain-Name");
    static String HEADER_PROGRAM = SyspropUtil.getString(PROP_HEADER_PROGRAM, "X-Injected-Program-Name");

    /**
     * Handle any domain ID injection that is configured to occur.
     * <p/>
     * If domain ID injection is not enabled, this method returns immediately without taking action.
     * <p/>
     * Otherwise, it injects domain ID headers into the specified HTTP request, using domain identity
     * read from the specified context.
     * <p/>
     * If domain ID injection is enabled, but no client socket credentials can be found in the context,
     * this message logs an error message and sends "unknown" values.
     *
     * @param context a PolicyApplicationContex from which to read client request socket credentials.  Required.
     * @param params  parameters for a pending outbound HTTP request.  Domain ID headers will be added to this request.  Required.
     */
    public static void injectHeaders(PolicyApplicationContext context, GenericHttpRequestParams params) {
        final PolicyApplicationContext.DomainIdInjectionFlags flags = context.getDomainIdInjectionFlags();
        if (!INJECT_ALWAYS && !flags.enable)
            return;

        if (!INJECTION_ENABLED) {
            logger.warning("Domain ID injection is requested by policy, but is locally disabled; sending DECLINED");
            addStatusHeader(params, DomainIdStatusCode.DECLINED, null);
            return;
        }

        Map<String, String> id = context.getClientSocketCredentials();
        if (id == null) {
            logger.warning("Domain ID injection is enabled, but no socket credentials could be found for this request; sending FAILED");
            addStatusHeader(params, DomainIdStatusCode.FAILED, null);
            return;
        }

        Map<String, String> statusParams = new LinkedHashMap<String, String>();
        addIdentifier(statusParams, params, id, flags.userHeaderName, HEADER_USERNAME, LocalTcpPeerIdentifier.IDENTIFIER_USERNAME);
        addIdentifier(statusParams, params, id, flags.domainHeaderName, HEADER_DOMAIN, LocalTcpPeerIdentifier.IDENTIFIER_NAMESPACE);
        addIdentifier(statusParams, params, id, flags.processHeaderName, HEADER_PROGRAM, LocalTcpPeerIdentifier.IDENTIFIER_PROGRAM);
        addStatusHeader(params, DomainIdStatusCode.INCLUDED, statusParams);
    }

    static void addStatusHeader(GenericHttpRequestParams params, DomainIdStatusCode included, Map<String, String> statusParams) {
        StringBuilder value = new StringBuilder(included.name());
        if (statusParams != null) {
            for (Map.Entry<String, String> entry : statusParams.entrySet()) {
                value.append("; ");
                value.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        params.addExtraHeader(new GenericHttpHeader(SecureSpanConstants.HttpHeaders.HEADER_DOMAINIDSTATUS, value.toString()));
    }

    static void addIdentifier(Map<String, String> statusParams, GenericHttpRequestParams params,
                                      Map<String, String> identifier, String headerName, String defaultHeaderName,
                                      String identifierName)
    {
        if (headerName == null)
            headerName = defaultHeaderName;
        if (headerName != null) {
            String value = identifier.get(identifierName);
            if (value != null) {
                try {
                    params.addExtraHeader(new GenericHttpHeader(headerName, MimeUtility.encodeText(value, "utf-8", "q")));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e); // can't happen
                }
                statusParams.put(identifierName, headerName);
            }
        }
    }
}
