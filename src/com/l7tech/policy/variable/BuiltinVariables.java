/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.variable;

import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.wsdl.Operation;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages built-in system variables and the strategies for finding their values.
 */
public class BuiltinVariables {
    private static final Logger logger = Logger.getLogger(BuiltinVariables.class.getName());

    public static final String PREFIX_REQUEST_HTTP_HEADER = "request.http.header";
    public static final String PREFIX_REQUEST_HTTP_HEADER_VALUES = "request.http.headerValues";

    public static final String PREFIX_RESPONSE_HTTP_HEADER = "response.http.header";
    public static final String PREFIX_RESPONSE_HTTP_HEADER_VALUES = "response.http.headerValues";

    private static final Map varsByName = new HashMap();
    private static final Map varsByPrefix = new HashMap();

    public static void set(String name, Object value, PolicyEnforcementContext context) throws VariableNotSettableException, NoSuchVariableException {
        Variable var = getVar(name);
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
        Variable var = getVar(name);

        if (var == null) throw new NoSuchVariableException(name);

        return var.get(name, context);
    }

    private static Variable getVar(String name) {
        // Try simple name first
        final String lname = name.toLowerCase();
        Variable var = (Variable)varsByName.get(lname);
        if (var == null) {
            // Try prefixed name
            int pos = lname.lastIndexOf(".");
            if (pos > 0)
                var = (Variable)varsByPrefix.get(lname.substring(0,pos));
        }
        return var;
    }

    public static boolean isSupported(String name) {
        return getVar(name) != null;
    }

    public static boolean isSettable(String name) {
        return getVar(name) instanceof SettableVariable;
    }

    public static boolean isMultivalued(String name) {
        Variable var = getVar(name);
        if (var == null) return false;
        return var.isMultivalued();
    }

    private static final RemoteIpGetter remoteIpGetter = new RemoteIpGetter();
    private static final OperationGetter soapOperationGetter = new OperationGetter();
    private static final SoapNamespaceGetter soapNamespaceGetter = new SoapNamespaceGetter();

    public static final Variable[] VARS = {
        new Variable("request.tcp.remoteAddress", remoteIpGetter),
        new Variable("request.tcp.remoteip", remoteIpGetter),
        new Variable("request.tcp.remoteHost", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRequest().getTcpKnob().getRemoteHost();
            }
        }),
        new Variable("request.tcp.localPort", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return String.valueOf(context.getRequest().getTcpKnob().getLocalPort());
            }
        }),
        new Variable("request.http.method", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRequest().getHttpRequestKnob().getMethod();
            }
        }),
        new Variable("request.http.uri", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRequest().getHttpRequestKnob().getRequestUri();
            }
        }),
        new Variable("request.http.url", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRequest().getHttpRequestKnob().getRequestUrl();
            }
        }),
        new Variable("request.http.secure", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return String.valueOf(context.getRequest().getHttpRequestKnob().isSecure());
            }
        }),
        new Variable("request.http.queryString", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRequest().getHttpRequestKnob().getQueryString();
            }
        }),
        new SettableVariable("auditLevel",
            new Getter() {
                public Object get(String name, PolicyEnforcementContext context) {
                    return context.getAuditLevel().getName();
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
                return context.getRequestId();
            }
        }),
        new Variable("routingStartTime", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return ISO8601Date.format(new Date(context.getRoutingStartTime()));
            }
        }),
        new Variable("routingEndTime", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return ISO8601Date.format(new Date(context.getRoutingEndTime()));
            }
        }),
        new Variable("routingStatus", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRoutingStatus().getName();
            }
        }),
        new Variable(PREFIX_REQUEST_HTTP_HEADER, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                String[] vals = getHeaderValues(PREFIX_REQUEST_HTTP_HEADER, name, context);
                if (vals.length > 0) return vals[0];
                return null;
            }
        }, true, false),
        new Variable(PREFIX_REQUEST_HTTP_HEADER_VALUES, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return getHeaderValues(PREFIX_REQUEST_HTTP_HEADER_VALUES, name, context);
            }
        }, true, true),
    };

    static {
        for (int i = 0; i < VARS.length; i++) {
            Variable var = VARS[i];
            if (var.isPrefixed()) {
                varsByPrefix.put(var.getName().toLowerCase(), var);
            } else {
                varsByName.put(var.getName().toLowerCase(), var);
            }
        }
    }

    private static String[] getHeaderValues(String prefix, String name, PolicyEnforcementContext context) {
        if (!name.startsWith(prefix)) throw new IllegalArgumentException("HTTP Header Getter can't handle variable named '" + name + "'!");
        String dotname = name.substring(prefix.length());
        if (!dotname.startsWith(".")) throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the header name.");
        String hname = name.substring(prefix.length()+1);
        return context.getRequest().getHttpRequestKnob().getHeaderValues(hname);
    }

    private static final String[] SINGLEOBJECT = {
        "request.http.clientCert",
        "ssg.cert",
        "protectedService.cert",
        "authenticatedUser",
        "service",
    };

    private static final String[] MULTIOBJECT = {
        "request.http.clientCert.chain",
        "ssg.cert.chain",
    };

    private static class RemoteIpGetter implements Getter {
        public Object get(String name, PolicyEnforcementContext context) {
            return context.getRequest().getTcpKnob().getRemoteAddress();
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
                return soapKnob.getPayloadNamespaceUri();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get SOAP namespace", e);
                return null;
            }
        }
    }
}
