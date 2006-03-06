/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.common.message.SoapKnob;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.wsdl.Operation;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerVariables {
    private static final Logger logger = Logger.getLogger(ServerVariables.class.getName());

    private static final RemoteIpGetter remoteIpGetter = new RemoteIpGetter();
    private static final OperationGetter soapOperationGetter = new OperationGetter();
    private static final SoapNamespaceGetter soapNamespaceGetter = new SoapNamespaceGetter();

    private static final Map varsByName = new HashMap();
    private static final Map varsByPrefix = new HashMap();

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


    private static String[] getHeaderValues(String prefix, String name, PolicyEnforcementContext context) {
        if (!name.startsWith(prefix)) throw new IllegalArgumentException("HTTP Header Getter can't handle variable named '" + name + "'!");
        String dotname = name.substring(prefix.length());
        if (!dotname.startsWith(".")) throw new IllegalArgumentException("Variable '" + name + "' does not have a period before the header name.");
        String hname = name.substring(prefix.length()+1);
        return context.getRequest().getHttpRequestKnob().getHeaderValues(hname);
    }


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

    private static final Variable[] VARS = {
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
        new Variable("routingStatus", new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return context.getRoutingStatus().getName();
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                String[] vals = getHeaderValues(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER, name, context);
                if (vals.length > 0) return vals[0];
                return null;
            }
        }),
        new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, new Getter() {
            public Object get(String name, PolicyEnforcementContext context) {
                return getHeaderValues(BuiltinVariables.PREFIX_REQUEST_HTTP_HEADER_VALUES, name, context);
            }
        }),
    };

    static {
        for (int i = 0; i < VARS.length; i++) {
            Variable var = VARS[i];
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
                String[] uris = soapKnob.getPayloadNamespaceUris();
                return uris == null || uris.length < 1 ? null : uris[0];
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't get SOAP namespace", e);
                return null;
            }
        }
    }

}
