/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.message.*;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;

import javax.wsdl.Operation;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.CertificateEncodingException;

public class ServerVariables {
    private static final Logger logger = Logger.getLogger(ServerVariables.class.getName());
    private static final String CLIENT_ID_UNKNOWN = "ClientId:Unknown";

    private static final RemoteIpGetter remoteIpGetter = new RemoteIpGetter();
    private static final OperationGetter soapOperationGetter = new OperationGetter();
    private static final SoapNamespaceGetter soapNamespaceGetter = new SoapNamespaceGetter();

    private static final Map<String, Variable> varsByName = new HashMap<String, Variable>();
    private static final Map<String, Variable> varsByPrefix = new HashMap<String, Variable>();

    static Variable getVariable(String name) {
        final String lname = name.toLowerCase();
        Variable var = varsByName.get(lname);
        if (var != null) return var;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            var = varsByPrefix.get(tryname);
            if (var != null) return var;
            pos = lname.lastIndexOf(".", pos-1);
        } while (pos > 0);

        return null;
    }

    private static String[] getParamValues(String prefix, String name, PolicyEnforcementContext context) throws IOException {
        HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) return new String[0];

        if (!name.startsWith(prefix)) throw new IllegalArgumentException("HTTP Param Getter can't handle variable named '" + name + "'!");
        String suffix = name.substring(prefix.length());
        if (!suffix.startsWith(".")) throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the parameter name.");
        String hname = name.substring(prefix.length()+1);
        return hrk.getParameterValues(hname);
    }

    private static String[] getHeaderValues(String prefix, String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        // TODO what about response headers?
        HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) return new String[0];

        if (!name.startsWith(prefix)) {
            logger.warning("HTTP Header Getter can't handle variable named '" + name + "'!");
            throw new NoSuchVariableException("invalid http header name " + name);
        }
        String suffix = name.substring(prefix.length());
        if (!suffix.startsWith(".")) {
            logger.warning("Variable '" + name + "' does not have a period before the header name.");
            throw new NoSuchVariableException("invalid http header name " + name);
        }
        String hname = name.substring(prefix.length()+1);
        return hrk.getHeaderValues(hname);
    }

    public static void set(String name, Object value, PolicyEnforcementContext context) throws VariableNotSettableException, NoSuchVariableException {
        Variable var = getVariable(name);
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
        Variable var = getVariable(name);

        if (var == null) throw new NoSuchVariableException(name);

        return var.get(name, context);
    }

    private static final Variable[] VARS = {
        new Variable("request.tcp.remoteAddress", remoteIpGetter),
        new Variable("request.tcp.remoteip", remoteIpGetter),
        new Variable("request.tcp.remoteHost", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                TcpKnob tk = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
                return tk == null ? null : tk.getRemoteHost();
            }
        }),
        new Variable(BuiltinVariables.PREFIX_AUTHENTICATED_USER, new AuthenticatedUserGetter(BuiltinVariables.PREFIX_AUTHENTICATED_USER)),
        new Variable(BuiltinVariables.PREFIX_AUTHENTICATED_USERS, new AuthenticatedUserGetter(BuiltinVariables.PREFIX_AUTHENTICATED_USERS)),
        new Variable("request.clientid", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                User user = context.getLastAuthenticatedUser();
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
            public Object get(String name, PolicyEnforcementContext context) {
                TcpKnob tk = (TcpKnob)context.getRequest().getKnob(TcpKnob.class);
                return tk == null ? null : String.valueOf(tk.getLocalPort());
            }
        }),
        new Variable("request.http.method", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getMethod().name();
            }
        }),
        new Variable("request.http.uri", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getRequestUri();
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_URL, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
                if (hrk == null) {
                    FtpRequestKnob frk = (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
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
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : String.valueOf(hrk.isSecure());
            }
        }),
        new Variable("request.http.queryString", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                HttpRequestKnob hrk = (HttpRequestKnob)context.getRequest().getKnob(HttpRequestKnob.class);
                return hrk == null ? null : hrk.getQueryString();
            }
        }),
        new Variable("request.ftp.path", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : frk.getPath();
            }
        }),
        new Variable("request.ftp.file", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : frk.getFile();
            }
        }),
        new Variable("request.ftp.unique", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : String.valueOf(frk.isUnique());
            }
        }),
        new Variable("request.ftp.secure", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                FtpRequestKnob frk = (FtpRequestKnob)context.getRequest().getKnob(FtpRequestKnob.class);
                return frk == null ? null : String.valueOf(frk.isSecure());
            }
        }),
        new Variable("request.elapsedTime", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return Long.toString(System.currentTimeMillis() - context.getStartTime());
            }
        }),
        new SettableVariable("auditLevel",
            new Getter() {
                public Object get(String name, PolicyEnforcementContext context) {
                    Level level = context.getAuditLevel();
                    return level == null ? null : level.getName();
                }
            },
            new Setter() {
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
            public Object get(String name, PolicyEnforcementContext context) {
                RequestId id = context.getRequestId();
                return id == null ? null : id.toString();
            }
        }),
        new Variable("routingStatus", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRoutingStatus().getName();
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                String[] vals;
                try {
                    vals = getHeaderValues(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, name, context);
                } catch (NoSuchVariableException e) {
                    logger.log(Level.WARNING, "invalid http header request", e);
                    return null;
                }
                if (vals.length > 0) return vals[0];
                return null;
            }
        }),

        new Variable(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                // get HttpResponseKnob from pec
                HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
                if (hrk == null) return new String[0];

                String suffix = name.substring(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER.length());
                if (!suffix.startsWith(".")) {
                    logger.warning("Variable '" + name + "' does not have a period before the header name.");
                    return new String[0];
                }
                String hname = name.substring(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER.length()+1);

                String[] vals = hrk.getHeaderValues(hname);
                if (vals.length > 0) return vals[0];
                return null;
            }
        }),

        new Variable(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER_VALUES, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                // get HttpResponseKnob from pec
                HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
                if (hrk == null) return new String[0];

                String suffix = name.substring(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER_VALUES.length());
                if (!suffix.startsWith(".")) {
                    logger.warning("Variable '" + name + "' does not have a period before the header name.");
                    return new String[0];
                }
                String hname = name.substring(BuiltinVariables.PREFIX_RESPONSE_HTTP_HEADER_VALUES.length()+1);

                return hrk.getHeaderValues(hname);
            }
        }),

        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, new Getter() {
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
        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                try {
                    return getHeaderValues(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, name, context);
                } catch (NoSuchVariableException e) {
                    logger.log(Level.WARNING, "invalid http header values request", e);
                    return null;
                }
            }
        }),

        new Variable(BuiltinVariables.PREFIX_REQUEST_JMS_MSG_PROP, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                final JmsKnob jmsKnob = (JmsKnob)context.getRequest().getKnob(JmsKnob.class);
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
            public Object get(String name, PolicyEnforcementContext context) {
                final JmsKnob jmsKnob = (JmsKnob)context.getResponse().getKnob(JmsKnob.class);
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
            public Object get(String name, PolicyEnforcementContext context) {
                return getUrlValue(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_URL, name, context.getRoutedServiceUrl());
            }
        }),

        new Variable(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_NAME, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getService().getName();
            }
        }),

        new Variable(BuiltinVariables.PREFIX_SERVICE+"."+BuiltinVariables.SERVICE_SUFFIX_OID, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getService().getId();
            }
        }),

        new Variable(BuiltinVariables.PREFIX_GATEWAY_TIME, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return TimeVariableUtils.getTimeValue(BuiltinVariables.PREFIX_GATEWAY_TIME, name, new TimeVariableUtils.LazyLong() {
                    public long get() {
                        return System.currentTimeMillis();
                    }
                });
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_TIME, new Getter() {
            public Object get(String name, final PolicyEnforcementContext context) {
                return TimeVariableUtils.getTimeValue(BuiltinVariables.PREFIX_REQUEST_TIME, name, new TimeVariableUtils.LazyLong() {
                    public long get() {
                        return context.getStartTime();
                    }
                });
            }
        }),
        new Variable("response.http.status", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                HttpResponseKnob hrk = (HttpResponseKnob)context.getResponse().getKnob(HttpResponseKnob.class);
                return hrk == null ? null : Integer.toString(hrk.getStatus());
            }
        }),
        new Variable(BuiltinVariables.PREFIX_CLUSTER_PROPERTY, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                if (context.getClusterPropertyCache() != null) {
                    if (name.length() < (BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 2)) {
                        logger.warning("variable name " + name + " cannot be resolved to a cluster property");
                        return null;
                    }
                    name = name.substring(BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 1);
                    // TODO make this cache timeout configurable
                    ClusterProperty cp = context.getClusterPropertyCache().getCachedEntityByName(name, 30000);
                    if (cp == null || cp.isHiddenProperty()) return null;
                    return cp.getValue();
                } else {
                    logger.severe("cannot get ClusterPropertyCache through context");
                    return null;
                }
            }
        }),
        new Variable("request.username", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                LoginCredentials creds = context.getLastCredentials();
                if (creds == null) return null;
                return creds.getName();
            }
        }),
        new Variable("request.password", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                LoginCredentials creds = context.getLastCredentials();
                if (creds == null) return null;
                final char[] pass = creds.getCredentials();
                if (pass == null || pass.length == 0) return null;
                return new String(pass);
            }
        }),

        new Variable("request.ssl.clientCertificate", new Getter(){
            public Object get(String name, PolicyEnforcementContext context){
                List<LoginCredentials> allCreds = context.getCredentials();
                for(LoginCredentials creds : allCreds){
                    if(creds != null && creds.getFormat().isClientCert()){
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        return creds.getClientCert();
                    }
                }
                return null;
            }
        }),
        new Variable("request.ssl.clientCertificate.base64", new Getter(){
            public Object get(String name, PolicyEnforcementContext context){
                List<LoginCredentials> allCreds = context.getCredentials();
                for(LoginCredentials creds : allCreds){
                    if(creds != null && creds.getFormat().isClientCert()){
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        BufferPoolByteArrayOutputStream bos = new BufferPoolByteArrayOutputStream();
                        try{
                            String encoding = "UTF-8";
                            bos.write(HexUtils.encodeBase64(creds.getClientCert().getEncoded()).getBytes(encoding));
                            return bos.toString(encoding);
                        }catch(Exception e){
                            return null;
                        }finally{
                            bos.close();
                        }
                    }
                }
                return null;
            }
        }),
        new Variable("request.ssl.clientCertificate.pem", new Getter(){
            public Object get(String name, PolicyEnforcementContext context) {
                List<LoginCredentials> allCreds = context.getCredentials();
                for (LoginCredentials creds : allCreds) {
                    if (creds != null && creds.getFormat().isClientCert()) {
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        try{
                            return CertUtils.encodeAsPEM(creds.getClientCert());
                        }catch(Exception e){
                            logger.log(Level.SEVERE, "Error getting certificate's PEM value.", e);
                            return null;
                        }
                    }
                }
                return null;
            }
        }),
        new Variable("request.ssl.clientCertificate.der", new Getter(){
            public Object get(String name, PolicyEnforcementContext context){
                List<LoginCredentials> allCreds = context.getCredentials();
                for(LoginCredentials creds : allCreds){
                    if(creds != null && creds.getFormat().isClientCert()){
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        try {
                            return creds.getClientCert().getEncoded();
                        } catch (CertificateEncodingException cee) {
                            logger.log(Level.SEVERE, "Error getting certificate encoding.", cee);
                            return null;
                        }
                    }
                }
                return null;
            }
        }),
        new Variable("request.wss.signingcertificate", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                List<LoginCredentials> allCreds = context.getCredentials();
                for (LoginCredentials creds : allCreds) {
                    if (creds != null  && creds.getFormat().isClientCert() && creds.getCredentialSourceAssertion() == RequestWssX509Cert.class) {
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        return creds.getClientCert();
                    }
                }
                return null;
            }
        }),
        new Variable("request.wss.signingcertificate.base64", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                List<LoginCredentials> allCreds = context.getCredentials();
                for (LoginCredentials creds : allCreds) {
                    if (creds != null  && creds.getFormat().isClientCert() && creds.getCredentialSourceAssertion() == RequestWssX509Cert.class) {
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        try {
                            return HexUtils.encodeBase64(creds.getClientCert().getEncoded(), true);//strip whitespaces
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Error getting base64 value.", e);
                            return null;
                        }
                    }
                }
                return null;
            }
        }),
        new Variable("request.wss.signingcertificate.pem", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                List<LoginCredentials> allCreds = context.getCredentials();
                for (LoginCredentials creds : allCreds) {
                    if (creds != null  && creds.getFormat().isClientCert() && creds.getCredentialSourceAssertion() == RequestWssX509Cert.class) {
                        //can only have one credential of a particular type per ProcessingContext, so this must be it
                        try {
                            return CertUtils.encodeAsPEM(creds.getClientCert());
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Error getting certificate's PEM value.", e);
                            return null;
                        }
                    }
                }
                return null;
            }
        }),
    };

    private static String getRequestRemoteIp(Message request) {
        TcpKnob tk = (TcpKnob) request.getKnob(TcpKnob.class);
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
            String name = var.getName();
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

    private static class RemoteIpGetter implements Getter {
        public Object get(String name, PolicyEnforcementContext context) {
            return getRequestRemoteIp(context.getRequest());
        }
    }

    private static class AuthenticatedUserGetter implements Getter {
        private String variableType;
        public AuthenticatedUserGetter(String variableType) {
            this.variableType = variableType;
        }
        public Object get(String name, PolicyEnforcementContext context) {
            // The variable is request.authenticatedusers
            if (BuiltinVariables.PREFIX_AUTHENTICATED_USERS.equals(variableType)) {
                List<AuthenticationResult> authResultList = context.getAllAuthenticationResults();
                List<String> authUsersList = new ArrayList<String>();
                for (AuthenticationResult authResult : authResultList) {
                    if (authResult != null) {
                        authUsersList.add(authResult.getUser().getName());
                    }
                }
                return authUsersList.toArray(new String[authUsersList.size()]);
            }
            // The variable is request.authenticateduser
            else if (BuiltinVariables.PREFIX_AUTHENTICATED_USER.equals(variableType)) {
                String suffix = name.substring(BuiltinVariables.PREFIX_AUTHENTICATED_USER.length());
                if (suffix.length() == 0) { // Without suffix
                    String user = null;
                    User authenticatedUser = context.getLastAuthenticatedUser();
                    if (authenticatedUser != null) {
                        user = authenticatedUser.getName();
                        if (user == null) user = authenticatedUser.getId();
                    }
                    return user;
                } else { // With suffix
                    if (!suffix.startsWith("."))
                        throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the parameter name.");
                    String indexS = name.substring(BuiltinVariables.PREFIX_AUTHENTICATED_USER.length() + 1);
                    try {
                        int index = Integer.parseInt(indexS);
                        AuthenticationResult ar = context.getAllAuthenticationResults().get(index);
                        if (ar == null) {
                            logger.info("Context variable " + name + " yielded null");
                            return null;
                        }
                        return ar.getUser().getName();
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Was expecting a number suffix with " + name + ". " + e.getMessage());
                    } catch (IndexOutOfBoundsException e) {
                        logger.info("not that many users authenticated: " + e.getMessage());
                        // shouldn't throw here, we'll be nice
                        return "";
                    }
                }
            }
            // Otherwise, the variable is invalid.
            logger.info("The context variable, '" + name + "' is invalid");
            return null;
        }
    }

    private static class OperationGetter implements Getter {
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

    private static class SoapNamespaceGetter implements Getter {
        public Object get(String name, PolicyEnforcementContext context) {
            SoapKnob soapKnob = (SoapKnob) context.getRequest().getKnob(SoapKnob.class);
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

}
