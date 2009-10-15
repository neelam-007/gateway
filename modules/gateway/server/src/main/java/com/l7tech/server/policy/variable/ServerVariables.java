/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.identity.User;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerVariables {
    private static final Logger logger = Logger.getLogger(ServerVariables.class.getName());
    private static final String CLIENT_ID_UNKNOWN = "ClientId:Unknown";

    private static final RemoteIpGetter remoteIpGetter = new RemoteIpGetter();
    private static final OperationGetter soapOperationGetter = new OperationGetter();
    private static final SoapNamespaceGetter soapNamespaceGetter = new SoapNamespaceGetter();

    private static final Map<String, Variable> varsByName = new HashMap<String, Variable>();
    private static final Map<String, Variable> varsByPrefix = new HashMap<String, Variable>();

    static Variable getVariable(String name, PolicyEnforcementContext targetContext) {
        final String lname = name.toLowerCase();
        Variable var = varsByName.get(lname);
        if (var != null && var.isValidForContext(targetContext)) return var;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            var = varsByPrefix.get(tryname);
            if (var != null && var.isValidForContext(targetContext)) return var;
            pos = lname.lastIndexOf(".", pos-1);
        } while (pos > 0);

        return null;
    }

    private static String[] getParamValues(String prefix, String name, PolicyEnforcementContext context) throws IOException {
        HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) return new String[0];

        if (!name.startsWith(prefix)) throw new IllegalArgumentException("HTTP Param Getter can't handle variable named '" + name + "'!");
        String suffix = name.substring(prefix.length());
        if (!suffix.startsWith(".")) throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the parameter name.");
        String hname = name.substring(prefix.length()+1);
        return hrk.getParameterValues(hname);
    }

    public static void set(String name, Object value, PolicyEnforcementContext context) throws VariableNotSettableException, NoSuchVariableException {
        Variable var = getVariable(name, context);
        if (var instanceof SettableVariable) {
            SettableVariable sv = (SettableVariable)var;
            sv.set(name, value, context);
        } else if (var == null) {
            throw new NoSuchVariableException(name);
        } else {
            throw new VariableNotSettableException(name);
        }
    }

    public static Object get(String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        Variable var = getVariable(name, context);

        if (var == null) throw new NoSuchVariableException(name);

        return var.get(name, context);
    }

    public static boolean isValidForContext(String name, PolicyEnforcementContext context) {
        return getVariable(name, context) != null;
    }

    private static final Variable[] VARS = {
        new Variable("request", new RequestGetter("request")),
        new Variable("response", new ResponseGetter("response")),

        new Variable("request.tcp.remoteAddress", remoteIpGetter),
        new Variable("request.tcp.remoteip", remoteIpGetter),
        new Variable("request.tcp.remoteHost", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                TcpKnob tk = context.getRequest().getKnob(TcpKnob.class);
                return tk == null ? null : tk.getRemoteHost();
            }
        }),
        new Variable("request.clientid", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                User user = context.getAuthenticationContext(context.getRequest()).getLastAuthenticatedUser();
                String dat = user == null ? null : user.getProviderId() + ":" + user.getId();
                if (dat != null) return "AuthUser:" + dat;

                final Message request = context.getRequest();
                dat = getRequestRemoteIp(request);
                if (dat != null) return "ClientIp:" + dat;

                dat = getRequestProtocolId(request);
                if (dat != null) return "ProtocolId:" + dat;

                dat = getRequestProtocol(request);
                if (dat != null) return "Protocol:" + dat;

                return CLIENT_ID_UNKNOWN;
            }
        }),
        new Variable("request.tcp.localPort", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                TcpKnob tk = context.getRequest().getKnob(TcpKnob.class);
                return tk == null ? null : String.valueOf(tk.getLocalPort());
            }
        }),
        new Variable("request.http.method", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getMethod().name();
            }
        }),
        new Variable("request.http.uri", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getRequestUri();
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_URL, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
                if (hrk == null) {
                    FtpRequestKnob frk = context.getRequest().getKnob(FtpRequestKnob.class);
                    if (frk == null)
                        return null;
                    final String fullUrl = frk.getRequestUrl();
                    return getUrlValue(BuiltinVariables.PREFIX_REQUEST_URL, name, fullUrl);
                }
                final String fullUrl = hrk.getQueryString() == null ? hrk.getRequestUrl() : hrk.getRequestUrl() + "?" + hrk.getQueryString();
                return getUrlValue(BuiltinVariables.PREFIX_REQUEST_URL, name, fullUrl);
            }
        }),
        new Variable("request.http.secure", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : String.valueOf(hrk.isSecure());
            }
        }),
        new Variable("request.http.queryString", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getQueryString();
            }
        }),
        new Variable("request.ftp.path", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : frk.getPath();
            }
        }),
        new Variable("request.ftp.file", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : frk.getFile();
            }
        }),
        new Variable("request.ftp.unique", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : String.valueOf(frk.isUnique());
            }
        }),
        new Variable("request.ftp.secure", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : String.valueOf(frk.isSecure());
            }
        }),
        new Variable("request.elapsedTime", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return Long.toString(System.currentTimeMillis() - context.getStartTime());
            }
        }),
        new SettableVariable("auditLevel",
            new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    Level level = context.getAuditLevel();
                    return level == null ? null : level.getName();
                }
            },
            new Setter() {
                @Override
                public void set(String name, Object value, PolicyEnforcementContext context) {
                    Level level = Level.parse(value.toString());
                    if (level.equals(Level.SEVERE)) {
                        throw new IllegalArgumentException("SEVERE is reserved for audit system events");
                    }
                    context.setAuditLevel(level);
                }
            }
        ),
        new Variable("request.soap.operation", soapOperationGetter),
        new Variable("request.soap.operationname", soapOperationGetter),
        new Variable("request.soap.namespace", soapNamespaceGetter),
        new Variable("request.soap.urn", soapNamespaceGetter),
        new Variable("requestId", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                RequestId id = context.getRequestId();
                return id == null ? null : id.toString();
            }
        }),
        new Variable("routingStatus", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRoutingStatus().getName();
            }
        }),

        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                String[] vals;
                try {
                    vals = getParamValues(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, name, context);
                    if (vals == null) return null;
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Couldn't get HTTP parameter: " + name, e);
                    return null;
                }
                if (vals.length > 0) return vals[0];
                return null;
            }
        }),

        new Variable(BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                final JmsKnob jmsKnob = context.getRequest().getKnob(JmsKnob.class);
                if (jmsKnob == null) return null;
                final String prefix = BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP + ".";
                if (!name.startsWith(prefix)) {
                    logger.warning("Context variable for request JMS message property does not start with the correct prefix (" + prefix + "): " + name);
                    return null;
                }
                final String propName = name.substring(prefix.length());
                return jmsKnob.getJmsMsgPropMap().get(propName);
            }
        }),

        new Variable(BuiltinVariables.PREFIX_RESPONSE_JMS_MSG_PROP, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                final JmsKnob jmsKnob = context.getResponse().getKnob(JmsKnob.class);
                if (jmsKnob == null) return null;
                final String prefix = BuiltinVariables.PREFIX_RESPONSE_JMS_MSG_PROP + ".";
                if (!name.startsWith(prefix)) {
                    logger.warning("Context variable for response JMS message property does not start with the correct prefix (" + prefix + "): " + name);
                    return null;
                }
                final String propName = name.substring(prefix.length());
                return jmsKnob.getJmsMsgPropMap().get(propName);
            }
        }),

        new Variable(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_URL, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return getUrlValue(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_URL, name, context.getRoutedServiceUrl());
            }
        }),

        new Variable(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_NAME, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getService().getName();
            }
        }),

        new Variable(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_OID, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getService().getId();
            }
        }),

        new Variable(BuiltinVariables.PREFIX_GATEWAY_TIME, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return TimeVariableUtils.getTimeValue(BuiltinVariables.PREFIX_GATEWAY_TIME, name, new TimeVariableUtils.LazyLong() {
                    @Override
                    public long get() {
                        return System.currentTimeMillis();
                    }
                });
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_TIME, new Getter() {
            @Override
            public Object get(String name, final PolicyEnforcementContext context) {
                return TimeVariableUtils.getTimeValue(BuiltinVariables.PREFIX_REQUEST_TIME, name, new TimeVariableUtils.LazyLong() {
                    @Override
                    public long get() {
                        return context.getStartTime();
                    }
                });
            }
        }),
        new Variable("response.http.status", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                HttpResponseKnob hrk = context.getResponse().getKnob(HttpResponseKnob.class);
                return hrk == null ? null : Integer.toString(hrk.getStatus());
            }
        }),
        new Variable(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                if (context.getClusterPropertyCache() != null) {
                    if (name.length() < (BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 2)) {
                        logger.warning("variable name " + name + " cannot be resolved to a cluster property");
                        return null;
                    }
                    name = name.substring(BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 1);
                    // TODO make this cache timeout configurable
                    ClusterProperty cp = context.getClusterPropertyCache().getCachedEntityByName(name, 30000);
                    if (cp != null && cp.isHiddenProperty()) return null;
                    return cp != null && cp.getValue() != null ? cp.getValue() :
                                 context.getClusterPropertyCache().getPropertyValueWithDefaultFallback(name);
                } else {
                    logger.severe("cannot get ClusterPropertyCache through context");
                    return null;
                }
            }
        }),
        new Variable("request.ssl.clientCertificate", new SelectingGetter("request.ssl.clientCertificate"){
            @Override
            public Object getBaseObject(PolicyEnforcementContext context){
                return getOnlyOneClientCertificateForSource(
                        context.getAuthenticationContext(context.getRequest()).getCredentials(),
                        SslAssertion.class );
            }
        }),
        new Variable("request.wss.signingcertificate", new SelectingGetter("request.wss.signingcertificate"){
            @Override
            public Object getBaseObject(PolicyEnforcementContext context) {
                return getOnlyOneClientCertificateForSource(
                        context.getAuthenticationContext(context.getRequest()).getCredentials(),
                        RequireWssX509Cert.class );
            }
        }),
        new MultiVariable("request", MessageSelector.getPrefixes(), new SelectingGetter("request") {
            @Override
            protected Object getBaseObject( PolicyEnforcementContext context ) {
                return context.getRequest();
            }

        }),
        new MultiVariable("response", MessageSelector.getPrefixes(), new SelectingGetter("response") {
            @Override
            protected Object getBaseObject( PolicyEnforcementContext context ) {
                return context.getResponse();
            }
        }),
        new Variable("request.compression.gzip.found", new Getter() {
            @Override
            public Object get(String name, PolicyEnforcementContext context) {
                return context.isRequestWasCompressed();
            }
        }),

        new Variable("audit", new AuditContextGetter("audit")),
        new Variable("audit.request", new AuditOriginalMessageGetter("audit.request", false)),
        new Variable("audit.requestContentLength", new AuditOriginalMessageSizeGetter(false)),
        new Variable("audit.response", new AuditOriginalMessageGetter("audit.response", true)),
        new Variable("audit.responseContentLength", new AuditOriginalMessageSizeGetter(true)),
        new Variable("audit.var", new AuditOriginalContextVariableGetter("audit.var")),

    };

    private static X509Certificate getOnlyOneClientCertificateForSource( final List<LoginCredentials> credentials,
                                                                         final Class<? extends Assertion> assertionClass ) {
        X509Certificate certificate = null;

        for ( LoginCredentials credential : credentials ) {
            if ( credential != null && credential.getFormat().isClientCert() && credential.getCredentialSourceAssertion() == assertionClass ) {
                if ( certificate != null ) {
                    certificate = null; // multiple certificates not supported here
                    break;
                }
                certificate = credential.getClientCert();
            }
        }

        return certificate;
    }

    private static String getRequestRemoteIp(Message request) {
        TcpKnob tk = request.getKnob(TcpKnob.class);
        return tk == null ? null : tk.getRemoteAddress();
    }

    private static String getRequestProtocolId(Message request) {
        // We shouldn't get here if it's HTTP (it should have had a client IP), and we currently know of no
        // equivalent identifier for other supported protocols, so just return null for now
        return null;
    }

    private static String getRequestProtocol(Message request) {
        // We shouldn't get here if it's HTTP (it should have had a client IP) so check for JMS
        return request.getKnob(JmsKnob.class) != null ? "Protocol:JMS" : null;
    }

    private static Object getUrlValue(String prefix, String name, Object u) {
        if (u == null) return null;
        String suffix = name.substring(prefix.length());
        if (suffix.length() == 0) return u.toString(); // Unsuffixed gets the full URL
        String part = suffix.substring(1);
        URL url;
        if (u instanceof URL) {
            url = (URL) u;
        } else {
            try {
                url = new URL(u.toString());
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[]{u.toString()});
                return null;
            }
        }
        final String protocol = url.getProtocol();
        if (BuiltinVariables.SERVICE_SUFFIX_HOST.equalsIgnoreCase(part)) {
            return url.getHost();
        } else if (BuiltinVariables.SERVICE_SUFFIX_PROTOCOL.equalsIgnoreCase(part)) {
            return protocol;
        } else if (BuiltinVariables.SERVICE_SUFFIX_PORT.equalsIgnoreCase(part)) {
            int port = url.getPort();
            if (port == -1) {
                if ("http".equalsIgnoreCase(protocol)) {
                    port = 80;
                } else if ("https".equalsIgnoreCase(protocol)) {
                    port = 443;
                } else if ("ftp".equalsIgnoreCase(protocol)) {
                    port = 21;
                } else if ("smtp".equalsIgnoreCase(protocol)) {
                    port = 25;
                } else if ("pop3".equalsIgnoreCase(protocol)) {
                    port = 110;
                } else if ("imap".equalsIgnoreCase(protocol)) {
                    port = 143;
                }
            }
            return Integer.toString(port);
        } else if (BuiltinVariables.SERVICE_SUFFIX_FILE.equalsIgnoreCase(part)) {
            return url.getFile();
        } else if (BuiltinVariables.SERVICE_SUFFIX_PATH.equalsIgnoreCase(part)) {
            return url.getPath();
        } else if (BuiltinVariables.SERVICE_SUFFIX_QUERY.equalsIgnoreCase(part)) {
            return url.getQuery() == null ? null : "?" + url.getQuery();
        } else if (BuiltinVariables.SERVICE_SUFFIX_FRAGMENT.equalsIgnoreCase(part)) {
            return url.getRef();
        } else {
            logger.log(Level.WARNING, "Can't handle variable named " + name);
            return null;
        }
    }

    static {
        for (Variable var : VARS) {
            List<String> names = new ArrayList<String>();
            if ( var instanceof MultiVariable ) {
                for ( String subName : ((MultiVariable)var).getSubNames() ) {
                    names.add( var.getName().toLowerCase() + "." + subName.toLowerCase() );
                }
            } else {
                names.add( var.getName() );
            }

            for ( String name : names ) {
                VariableMetadata meta = BuiltinVariables.getMetadata(name);

                if (meta == null)
                    throw new IllegalStateException("ServerVariable '" + name + "' was not found in BuiltinVariables!");

                if (meta.isPrefixed()) {
                    varsByPrefix.put(name.toLowerCase(), var);
                } else {
                    varsByName.put(name.toLowerCase(), var);
                }
            }
        }
    }

    private static class MultiVariable extends Variable {
        private final String[] subNames;

        MultiVariable( final String name, final String[] subNames, final Getter getter ) {
            super( name, getter );
            this.subNames = subNames;
        }

        public String[] getSubNames() {
            return subNames;
        }
    }

    private static class RequestGetter extends SelectingGetter {
        private RequestGetter(final String baseName) {
            super(baseName);
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            return context.getRequest();
        }
    }

    private static class ResponseGetter extends SelectingGetter {
        private ResponseGetter(final String baseName) {
            super(baseName);
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            return context.getResponse();
        }
    }

    private static class RemoteIpGetter extends Getter {
        @Override
        public Object get(String name, PolicyEnforcementContext context) {
            return getRequestRemoteIp(context.getRequest());
        }
    }

    private static class OperationGetter extends Getter {
        @Override
        public Object get(String name, PolicyEnforcementContext context) {
            try {
                if (context.getService() == null) {
                    logger.info("Can't get operation name because there is no resolved service attached to this context");
                    return null;
                }
                if (context.getService().isSoap()) {
                    Operation operation = context.getOperation();
                    if (operation != null) return operation.getName();
                    return null;
                } else {
                    logger.info("Can't get operation name for a non-SOAP service");
                    return null;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get operation name", e);
                return null;
            }
        }
    }

    private static class SoapNamespaceGetter extends Getter {
        @Override
        public Object get(String name, PolicyEnforcementContext context) {
            SoapKnob soapKnob = context.getRequest().getKnob(SoapKnob.class);
            if (soapKnob == null) {
                logger.info("Can't get SOAP namespace for non-SOAP message");
                return null;
            }

            try {
                QName[] uris = soapKnob.getPayloadNames();
                return uris == null || uris.length < 1 ? null : uris[0].getNamespaceURI();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get SOAP namespace", e);
                return null;
            }
        }
    }

    private static class AuditContextGetter extends SelectingGetter {
        private AuditContextGetter(final String baseName) {
            super(baseName);
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof AuditSinkPolicyEnforcementContext) {
                AuditSinkPolicyEnforcementContext sinkctx = (AuditSinkPolicyEnforcementContext) context;
                return sinkctx.getAuditRecord();
            }
            logger.log(Level.WARNING, "The audit.* variables are only available while processing an audit sink policy.");
            return null;
        }

        @Override
        boolean isValidForContext(PolicyEnforcementContext context) {
            return context instanceof AuditSinkPolicyEnforcementContext;
        }
    }

    /**
     * Superclass for getters that only work inside an audit sink policy.
     */
    private static abstract class AbstractAuditGetter extends Getter {
        @Override
        boolean isValidForContext(PolicyEnforcementContext context) {
            return context instanceof AuditSinkPolicyEnforcementContext;
        }
    }

    /**
     * Superclass for selecting getters that only work inside an audit sink policy.
     */
    private static abstract class AbstractSelectingAuditGetter extends SelectingGetter {
        protected AbstractSelectingAuditGetter(final String baseName) {
            super(baseName);
        }

        @Override
        boolean isValidForContext(PolicyEnforcementContext context) {
            return context instanceof AuditSinkPolicyEnforcementContext;
        }
    }

    private static class AuditOriginalMessageGetter extends AbstractSelectingAuditGetter {
        private final boolean targetsResponse;

        private AuditOriginalMessageGetter(String baseName, boolean targetsResponse) {
            super(baseName);
            this.targetsResponse = targetsResponse;
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof AuditSinkPolicyEnforcementContext) {
                AuditSinkPolicyEnforcementContext auditpec = (AuditSinkPolicyEnforcementContext) context;
                return targetsResponse ? auditpec.getOriginalResponse() : auditpec.getOriginalRequest();
            }
            return null;
        }
    }

    /**
     * Used only for getting the "audit.requestContentLength" and "audit.responseContentLength" values
     * within an audit sink policy.
     */
    private static class AuditOriginalMessageSizeGetter extends AbstractAuditGetter {
        private final boolean targetsResponse;

        private AuditOriginalMessageSizeGetter(boolean targetsResponse) {
            this.targetsResponse = targetsResponse;
        }

        @Override
        Object get(String name, PolicyEnforcementContext context) {
            if (context instanceof AuditSinkPolicyEnforcementContext) {
                AuditSinkPolicyEnforcementContext auditpec = (AuditSinkPolicyEnforcementContext) context;
                Message target = targetsResponse ? auditpec.getOriginalResponse() : auditpec.getOriginalRequest();
                if (target == null)
                    return null;
                MimeKnob mimeKnob = target.getKnob(MimeKnob.class);
                if (mimeKnob == null)
                    return null;
                try {
                    return mimeKnob.getContentLength();
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }
    }

    private static class AuditOriginalContextVariableGetter extends AbstractSelectingAuditGetter {
        protected AuditOriginalContextVariableGetter(final String baseName) {
            super(baseName);
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof AuditSinkPolicyEnforcementContext) {
                AuditSinkPolicyEnforcementContext sinkctx = (AuditSinkPolicyEnforcementContext) context;
                return sinkctx.getOriginalContext();
            }
            return null;
        }
    }

    private static abstract class SelectingGetter extends Getter {
        private final String baseName;

        private SelectingGetter( final String baseName ) {
            this.baseName = baseName;
        }

        @Override
        public final Object get(final String name, final PolicyEnforcementContext context) {
            Object object = getBaseObject(context);
            if ( object != null && !baseName.equalsIgnoreCase(name) ) {
                object = ExpandVariables.processSingleVariableAsObject(
                        "${"+name+"}",
                        Collections.singletonMap(baseName.toLowerCase(), object),
                        new LogOnlyAuditor(logger) );
            }
            return object;
        }

        protected abstract Object getBaseObject( PolicyEnforcementContext context );
    }

}
