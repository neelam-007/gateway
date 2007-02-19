/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.common.RequestId;
import com.l7tech.common.message.*;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.wsdl.Operation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author alex
 */
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
        new Variable("request.authenticateduser", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                String user = null;
                User authenticatedUser = context.getAuthenticatedUser();
                if (authenticatedUser != null) {
                    user = authenticatedUser.getName();
                    if (user == null) user = authenticatedUser.getId();
                }
                return user;
            }
        }),
        new Variable("request.clientid", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                User user = context.getAuthenticatedUser();
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
                return hrk == null ? null : hrk.getMethod();
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
                if (hrk == null)
                    return null;
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
        new Variable(BuiltinVariables.PREFIX_SERVICE_URL, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return getUrlValue(BuiltinVariables.PREFIX_SERVICE_URL, name, context.getRoutedServiceUrl());
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
                if (context.getClusterPropertyManager() != null) {
                    try {
                        if (name.length() < (BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 2)) {
                            logger.warning("variable name " + name + " cannot be resolved to a cluster property");
                            return null;
                        }
                        name = name.substring(BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 1);
                        // TODO make this cache timeout configurable
                        ClusterProperty cp = context.getClusterPropertyManager().getCachedEntityByName(name, 30000);
                        if (cp == null) return null;
                        return cp.getValue();
                    } catch (FindException e) {
                        logger.log(Level.WARNING, "exception querying for cluster property", e);
                        return null;
                    }
                } else {
                    logger.severe("cannot get ClusterPropertyManager through context");
                    return null;
                }
            }
        }),
        new Variable("request.username", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                LoginCredentials creds = context.getCredentials();
                if (creds == null) return null;
                return creds.getName();
            }
        }),
        new Variable("request.password", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                LoginCredentials creds = context.getCredentials();
                if (creds == null) return null;
                return new String(creds.getCredentials());
            }
        })
    };

    private static String getRequestRemoteIp(Message request) {
        TcpKnob tk = (TcpKnob)request.getKnob(TcpKnob.class);
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
            url = (URL)u;
        } else {
            try {
                url = new URL(u.toString());
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "URL cannot be parsed: {0}", new String[] {u.toString()});
                return null;
            }
        }
        final String protocol = url.getProtocol();
        if (BuiltinVariables.URLSUFFIX_HOST.equalsIgnoreCase(part)) {
            return url.getHost();
        } else if (BuiltinVariables.URLSUFFIX_PROTOCOL.equalsIgnoreCase(part)) {
            return protocol;
        } else if (BuiltinVariables.URLSUFFIX_PORT.equalsIgnoreCase(part)) {
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
        } else if (BuiltinVariables.URLSUFFIX_FILE.equalsIgnoreCase(part)) {
            return url.getFile();
        } else if (BuiltinVariables.URLSUFFIX_PATH.equalsIgnoreCase(part)) {
            return url.getPath();
        } else if (BuiltinVariables.URLSUFFIX_QUERY.equalsIgnoreCase(part)) {
            return url.getQuery() == null ? null : "?" + url.getQuery();
        } else if (BuiltinVariables.URLSUFFIX_FRAGMENT.equalsIgnoreCase(part)) {
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

    private static class OperationGetter implements Getter {
        public Object get(String name, PolicyEnforcementContext context) {
            try {
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
            SoapKnob soapKnob = (SoapKnob)context.getRequest().getKnob(SoapKnob.class);
            if (soapKnob == null) {
                logger.info("Can't get SOAP namespace for non-SOAP message");
                return null;
            }

            try {
                String[] uris = soapKnob.getPayloadNamespaceUris();
                return uris == null || uris.length < 1 ? null : uris[0];
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get SOAP namespace", e);
                return null;
            }
        }
    }

}
