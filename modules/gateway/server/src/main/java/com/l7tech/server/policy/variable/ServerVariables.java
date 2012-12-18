package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.User;
import com.l7tech.message.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.variable.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.trace.TracePolicyEnforcementContext;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.server.policy.variable.BuildVersionContext.BUILD_VERSION_CONTEXT;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

public class ServerVariables {
    private static final Logger logger = Logger.getLogger(ServerVariables.class.getName());
    private static final String CLIENT_ID_UNKNOWN = "ClientId:Unknown";

    private static final RemoteIpGetter remoteIpGetter = new RemoteIpGetter();
    private static final OperationGetter soapOperationGetter = new OperationGetter();
    private static final SoapNamespaceGetter soapNamespaceGetter = new SoapNamespaceGetter();

    private static final Map<String, Variable> varsByName = new HashMap<String, Variable>();
    private static final Map<String, Variable> varsByPrefix = new HashMap<String, Variable>();
    private static ClusterPropertyCache clusterPropertyCache;
    private static SecurePasswordManager securePasswordManager;
    private static ClusterInfoManager clusterInfoManager;
    private static SsgConnectorManager ssgConnectorManager;
    private static JdbcConnectionManager jdbcConnectionManager;

    private static final long SELF_NODE_INF_CACHE_INTERVAL = SyspropUtil.getLong("com.l7tech.server.policy.variable.ssgnode.cacheMillis", 30000L);
    private static final CachedCallable<ClusterNodeInfo> SELF_NODE_INF_CACHE = new CachedCallable<ClusterNodeInfo>(SELF_NODE_INF_CACHE_INTERVAL, new Callable<ClusterNodeInfo>() {
        @Override
        public ClusterNodeInfo call() throws Exception {
            return clusterInfoManager == null ? null : clusterInfoManager.getSelfNodeInf();
        }
    });

    /**
     * Has default value to support simple usages in test cases.
     */
    @NotNull
    private static TimeSource timeSource = new TimeSource();

    static Variable getVariable(String name, PolicyEnforcementContext targetContext) {
        final String lname = name.toLowerCase();
        Variable var = varsByName.get(lname);
        if (var != null && var.isValidForContext(targetContext)) return var;

        int pos = lname.length();
        do {
            String tryname = lname.substring(0, pos);
            var = varsByPrefix.get(tryname);
            if (var != null && var.isValidForContext(targetContext)) return var;
            pos = lname.lastIndexOf(".", pos - 1);
        } while (pos > 0);

        return null;
    }

    private static String[] getParamValues(String prefix, String name, PolicyEnforcementContext context) throws IOException {
        HttpRequestKnob hrk = context.getRequest().getKnob(HttpRequestKnob.class);
        if (hrk == null) return new String[0];

        if (!name.startsWith(prefix))
            throw new IllegalArgumentException("HTTP Param Getter can't handle variable named '" + name + "'!");
        String suffix = name.substring(prefix.length());
        if (!suffix.startsWith(".")) {
            logger.warning("Variable '" + name + "' does not have a period before the parameter name.");
            return null;
        }
        String hname = name.substring(prefix.length() + 1);
        return hrk.getParameterValues(hname);
    }

    public static void set(String name, Object value, PolicyEnforcementContext context) throws VariableNotSettableException, NoSuchVariableException {
        Variable var = getVariable(name, context);
        if (var instanceof SettableVariable) {
            SettableVariable sv = (SettableVariable) var;
            sv.set(name, value, context);
        } else if (var == null) {
            throw new NoSuchVariableException(name, "The variable \"" + name + "\" could not be found.");
        } else {
            throw new VariableNotSettableException(name);
        }
    }

    public static Object get(String name, PolicyEnforcementContext context) throws NoSuchVariableException {
        Variable var = getVariable(name, context);

        if (var == null) throw new NoSuchVariableException(name, "The variable \"" + name + "\" could not be found.");

        return var.get(name, context);
    }

    public static boolean isValidForContext(String name, PolicyEnforcementContext context) {
        return getVariable(name, context) != null;
    }

    @SuppressWarnings({ "deprecation" })
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
            new Variable("request.ssh.path", new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    SshKnob sk = context.getRequest().getKnob(SshKnob.class);
                    return sk == null ? null : sk.getPath();
                }
            }),
            new Variable("request.ssh.file", new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    SshKnob sk = context.getRequest().getKnob(SshKnob.class);
                    return sk == null ? null : sk.getFile();
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

            new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAMS, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    String[] vals = null;
                    try {
                        vals = getParamValues(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAMS, name, context);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Couldn't get HTTP parameters: " + name, e);
                    }
                    return vals;
                }
            }),
            new Variable(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    try {
                        String[] vals = getParamValues(BuiltinVariables.PREFIX_REQUEST_HTTP_PARAM, name, context);
                        if (vals != null && vals.length > 0) return vals[0];
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Couldn't get HTTP parameter: " + name, e);
                    }
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

            new Variable(BuiltinVariables.REQUEST_JMS_MSG_PROP_NAMES, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final JmsKnob jmsKnob = context.getRequest().getKnob(JmsKnob.class);
                    if (jmsKnob == null) return null;
                    final String prefix = BuiltinVariables.REQUEST_JMS_MSG_PROP_NAMES;
                    if (!name.startsWith(prefix)) {
                        logger.warning("Context variable for request JMS message property does not start with the correct prefix (" + prefix + "): " + name);
                        return null;
                    }
                    return jmsKnob.getJmsMsgPropMap().keySet().toArray();
                }
            }),

            new Variable(BuiltinVariables.RESPONSE_JMS_MSG_PROP_NAMES, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final JmsKnob jmsKnob = context.getResponse().getKnob(JmsKnob.class);
                    if (jmsKnob == null) return null;
                    final String prefix = BuiltinVariables.RESPONSE_JMS_MSG_PROP_NAMES;
                    if (!name.startsWith(prefix)) {
                        logger.warning("Context variable for response JMS message property does not start with the correct prefix (" + prefix + "): " + name);
                        return null;
                    }
                    return jmsKnob.getJmsMsgPropMap().keySet().toArray();
                }
            }),

            new Variable(BuiltinVariables.REQUEST_JMS_MSG_ALL_PROP_VALS, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final JmsKnob jmsKnob = context.getRequest().getKnob(JmsKnob.class);
                    if (jmsKnob == null) return null;
                    ArrayList<String> values = new ArrayList<String>();
                    final char delimiter = ':';
                    for (String key : jmsKnob.getJmsMsgPropMap().keySet()) {
                        values.add(key + delimiter + jmsKnob.getJmsMsgPropMap().get(key).toString());
                    }
                    return values.toArray();
                }
            }),
            new Variable(BuiltinVariables.RESPONSE_JMS_MSG_ALL_PROP_VALS, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final JmsKnob jmsKnob = context.getResponse().getKnob(JmsKnob.class);
                    if (jmsKnob == null) return null;
                    ArrayList<String> values = new ArrayList<String>();
                    final char delimiter = ':';
                    for (String key : jmsKnob.getJmsMsgPropMap().keySet()) {
                        values.add(key + delimiter + jmsKnob.getJmsMsgPropMap().get(key).toString());
                    }
                    return values.toArray();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    return getUrlValue(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_URL, name, context.getRoutedServiceUrl());
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_NAME, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final PublishedService service = context.getService();
                    return service == null ? null : service.getName();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_OID, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    final PublishedService service = context.getService();
                    return service == null ? null : service.getId();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_ROUTINGURL, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    String routingUrl = null;

                    try {
                        final PublishedService service = context.getService();
                        if (service == null)
                            return null;
                        final URL url = service.serviceUrl();
                        routingUrl = url == null ? null : url.toString();
                    } catch (WSDLException e) {
                        logger.log(
                                Level.WARNING,
                                "Could not access default routing URL for service '" + context.getService().displayName() + "', due to '" + getMessage( e ) + "'.",
                                getDebugException( e ));
                    } catch (MalformedURLException e) {
                        logger.log(
                                Level.WARNING,
                                "Could not access default routing URL for service '" + context.getService().displayName() + "', due to '" + getMessage( e ) + "'.",
                                getDebugException( e ));
                    }

                    return routingUrl;
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_POLICY_GUID, new Getter() {
                @Override
                public Object get( final String name, final PolicyEnforcementContext context) {
                    final PublishedService service = context.getService();
                    return service == null || service.getPolicy()==null ?
                            null : service.getPolicy().getGuid();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_SERVICE + "." + BuiltinVariables.SERVICE_SUFFIX_POLICY_VERSION, new Getter() {
                @Override
                public Object get( final String name, final PolicyEnforcementContext context) {
                    final PolicyMetadata meta = context.getServicePolicyMetadata();
                    final PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
                    return head == null || head.getPolicyRevision()==0L ? null : head.getPolicyRevision();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_POLICY + "." + BuiltinVariables.POLICY_SUFFIX_NAME, new CurrentPolicyGetter() {
                @Override
                protected Object get( final PolicyHeader policyHeader ) {
                    return policyHeader.getName();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_POLICY + "." + BuiltinVariables.POLICY_SUFFIX_OID, new CurrentPolicyGetter() {
                @Override
                protected Object get( final PolicyHeader policyHeader ) {
                    return policyHeader.getOid();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_POLICY + "." + BuiltinVariables.POLICY_SUFFIX_GUID, new CurrentPolicyGetter() {
                @Override
                protected Object get( final PolicyHeader policyHeader ) {
                    return policyHeader.getGuid();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_POLICY + "." + BuiltinVariables.POLICY_SUFFIX_VERSION, new CurrentPolicyGetter() {
                @Override
                protected Object get( final PolicyHeader policyHeader ) {
                    return policyHeader.getPolicyRevision()==0L ? null : policyHeader.getPolicyRevision();
                }
            }),

            new Variable(BuiltinVariables.PREFIX_ASSERTION + "." + BuiltinVariables.ASSERTION_SUFFIX_NUMBER, new Getter() {
                @Override
                Object get( final String name, final PolicyEnforcementContext context ) {
                    final Collection<Integer> assertionNumber = context.getAssertionNumber();
                    return assertionNumber.toArray(new Integer[assertionNumber.size()]);
                }
            }),

            new Variable(BuiltinVariables.PREFIX_ASSERTION + "." + BuiltinVariables.ASSERTION_SUFFIX_NUMBERSTR, new Getter() {
                @Override
                Object get( final String name, final PolicyEnforcementContext context ) {
                    final Collection<Integer> assertionNumber = context.getAssertionNumber();
                    return TextUtils.join(".", assertionNumber).toString();
                }
            }),

            new Variable(BuiltinVariables.ASSERTION_LATENCY_NS, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    return context.getAssertionLatencyNanos();
                }
            }),

            new Variable(BuiltinVariables.ASSERTION_LATENCY_MS, new Getter() {
                @Override
                public Object get(String name, PolicyEnforcementContext context) {
                    return TimeUnit.NANOSECONDS.toMillis(context.getAssertionLatencyNanos());
                }
            }),
            new Variable(BuiltinVariables.PREFIX_LISTENPORTS, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    return getListenPorts(name,context);
                }
            }),
            new Variable(BuiltinVariables.PREFIX_JDBC_CONNECTION, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    return getJdbcConnection(name, context);
                }
            }),
            new Variable(BuiltinVariables.SSGNODE_ID, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    ClusterNodeInfo inf = getSelfNodeInfCached();
                    return inf == null ? null : inf.getNodeIdentifier();
                }
            }),
            new Variable(BuiltinVariables.SSGNODE_IP, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    ClusterNodeInfo inf = getSelfNodeInfCached();
                    return inf == null ? null : inf.getAddress();
                }
            }),

            new Variable(BuiltinVariables.SSGNODE_HOSTNAME, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    final ServerConfig serverConfig = ServerConfig.getInstance();
                    return serverConfig.getHostname();
                }
            }),

            new Variable(BuiltinVariables.SSGNODE_NAME, new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    ClusterNodeInfo inf = getSelfNodeInfCached();
                    return inf == null ? null : inf.getName();
                }
            }),

            new Variable(BuiltinVariables.SSGNODE_BUILD,
                    SelectingGetter.selectingGetter( BuiltinVariables.SSGNODE_BUILD, BUILD_VERSION_CONTEXT )),
            new Variable(BuiltinVariables.PREFIX_GATEWAY_TIME, new SelectingGetter(BuiltinVariables.PREFIX_GATEWAY_TIME) {
                @Override
                protected Object getBaseObject(PolicyEnforcementContext context) {
                    return new Date(timeSource.currentTimeMillis());
                }
            }),

            new Variable(BuiltinVariables.PREFIX_REQUEST_TIME, new SelectingGetter(BuiltinVariables.PREFIX_REQUEST_TIME) {
                @Override
                protected Object getBaseObject(PolicyEnforcementContext context) {
                    return new Date(context.getStartTime());
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
                    if (name.length() < (BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 2)) {
                        logger.warning("variable name " + name + " cannot be resolved to a cluster property");
                        return null;
                    }
                    name = name.substring(BuiltinVariables.PREFIX_CLUSTER_PROPERTY.length() + 1);
                    final ServerConfig serverConfig = ServerConfig.getInstance();
                    final String configName = serverConfig.getNameFromClusterName(name);
                    if (configName != null) {
                        return serverConfig.getProperty(configName);
                    } else {
                        if (getClusterPropertyCache() != null) {
                            ClusterProperty cp = getClusterPropertyCache().getCachedEntityByName(name);
                            if (cp != null && cp.isHiddenProperty()) return null;
                            return cp != null ? cp.getValue() : null;
                        } else {
                            logger.severe("cannot get ClusterPropertyCache context");
                            return null;
                        }
                    }
                }
            }),
            new Variable("request.ssl.clientCertificate", new SelectingGetter("request.ssl.clientCertificate") {
                @Override
                public Object getBaseObject(PolicyEnforcementContext context) {
                    return getOnlyOneClientCertificateForSource(
                            context.getAuthenticationContext(context.getRequest()).getCredentials(),
                            SslAssertion.class);
                }
            }),
            new Variable("request.wss.signingcertificate", new SelectingGetter("request.wss.signingcertificate") {
                @Override
                public Object getBaseObject(PolicyEnforcementContext context) {
                    return getOnlyOneClientCertificateForSource(
                            context.getAuthenticationContext(context.getRequest()).getCredentials(),
                            RequireWssX509Cert.class);
                }
            }),
            new MultiVariable("request", MessageSelector.getPrefixes(), new SelectingGetter("request") {
                @Override
                protected Object getBaseObject(PolicyEnforcementContext context) {
                    return context.getRequest();
                }

            }),
            new MultiVariable("response", MessageSelector.getPrefixes(), new SelectingGetter("response") {
                @Override
                protected Object getBaseObject(PolicyEnforcementContext context) {
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
            new Variable("audit.code",  new Getter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    name = name.substring("audit.code.".length());
                    int dot = name.indexOf('.');
                    int index;
                    try {
                        if (dot == -1) {
                            index = Integer.parseInt(name);
                        } else {
                            index = Integer.parseInt(name.substring(0, dot));
                        }
                        AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(index);
                        String output = message == null ? null : message.getMessage();
                        return output;
                    } catch (NumberFormatException nfe) {
                        logger.info("Invalid numeric index for audit detail message lookup");
                        return null;
                    }
                }
            }),
            new Variable("audit.recordQuery", new AuditQueryGetter("audit.recordQuery")),
            new Variable("audit.request", new AuditOriginalMessageGetter("audit.request", false, false)),
            new Variable("audit.requestContentLength", new AuditOriginalMessageSizeGetter(false)),
            new Variable("audit.response", new AuditOriginalMessageGetter("audit.response", true, false)),
            new Variable("audit.filteredrequest", new AuditOriginalMessageGetter("audit.filteredrequest", false, true)),
            new Variable("audit.filteredresponse", new AuditOriginalMessageGetter("audit.filteredresponse", true, true)),
            new Variable("audit.responseContentLength", new AuditOriginalMessageSizeGetter(true)),
            new Variable("audit.var", new AuditOriginalContextVariableGetter("audit.var")),
            new Variable("audit.policyExecutionAttempted", new AbstractAuditGetter() {
                @Override
                Object get(String name, PolicyEnforcementContext context) {
                    final PolicyEnforcementContext originalContext = ((AuditSinkPolicyEnforcementContext) context).getOriginalContext();
                    return originalContext == null ? null : originalContext.isPolicyExecutionAttempted();
                }
            }),

            new Variable("trace", new DebugTraceGetter("trace")),
            new SettableVariable("trace.out", new DebugTraceGetter("trace"), new DebugTraceGetter("trace")),

            new Variable(BuiltinVariables.PREFIX_SECURE_PASSWORD, new SelectingGetter(BuiltinVariables.PREFIX_SECURE_PASSWORD) {
                @Override
                protected Object getBaseObject(PolicyEnforcementContext context) {
                    // Return a placeholder so the correct selector can fire up.  (The PEC will not be required further and is not relevant.)
                    return new SecurePasswordLocatorContext();
                }
            }),
    };


    private static Object getJdbcConnection(String name, PolicyEnforcementContext context) {
        name = name.substring(BuiltinVariables.PREFIX_JDBC_CONNECTION.length() + 1);
        int dotIndex = name.indexOf('.');
        String connectionName = name.substring(0, dotIndex);
        try {
            JdbcConnection connection = jdbcConnectionManager.getJdbcConnection(connectionName);
            if (connection != null) {
                SelectingGetter getter = SelectingGetter.selectingGetter(connectionName, connection);
                return getter.get(name, context);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Jdbc connections not found", ExceptionUtils.getDebugException(e));
        }
        return null;
    }


    private static Object getListenPorts(String name, PolicyEnforcementContext context) {
        try {
            Collection<SsgConnector> connectors = ssgConnectorManager.findAll();
            List<SsgConnector> connectorList = CollectionUtils.toList(connectors);

            if (connectorList != null) {
                SelectingGetter getter = SelectingGetter.selectingGetter(BuiltinVariables.PREFIX_LISTENPORTS, connectorList);
                return getter.get(name, context);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Listen ports not found",ExceptionUtils.getDebugException(e));
        }
        return null;
    }


    private static X509Certificate getOnlyOneClientCertificateForSource(final List<LoginCredentials> credentials,
                                                                        final Class<? extends Assertion> assertionClass) {
        X509Certificate certificate = null;

        for (LoginCredentials credential : credentials) {
            if (credential != null && credential.getFormat().isClientCert() && credential.getCredentialSourceAssertion() == assertionClass) {
                if (certificate != null) {
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

    @SuppressWarnings({ "UnusedParameters" })
    private static String getRequestProtocolId(Message request) {
        // We shouldn't get here if it's HTTP (it should have had a client IP), and we currently know of no
        // equivalent identifier for other supported protocols, so just return null for now
        return null;
    }

    private static String getRequestProtocol(Message request) {
        // We shouldn't get here if it's HTTP (it should have had a client IP) so check for JMS
        return request.getKnob(JmsKnob.class) != null ? "Protocol:JMS" : null;
    }

    @SuppressWarnings({ "deprecation" })
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

    private static ClusterPropertyCache getClusterPropertyCache() {
        return clusterPropertyCache;
    }

    public static void setClusterPropertyCache(final ClusterPropertyCache cache) {
        if (clusterPropertyCache == null) {
            clusterPropertyCache = cache;
        }
    }

    public static void setClusterInfoManager(ClusterInfoManager clusterInfoManager) {
        if (ServerVariables.clusterInfoManager == null) {
            ServerVariables.clusterInfoManager = clusterInfoManager;
        }
    }


    public static void setSsgConnectorManager(final SsgConnectorManager scm) {
        if (ssgConnectorManager == null) {
            ssgConnectorManager = scm;
        }
    }

    public static void setJdbcConnectionManager(JdbcConnectionManager jcm) {
        if (ServerVariables.jdbcConnectionManager == null) {
            ServerVariables.jdbcConnectionManager = jcm;
        }
    }


    public static void setSecurePasswordManager(SecurePasswordManager spm) {
        securePasswordManager = spm;
    }

    /**
     * Configure the TimeSource. Called via spring and reflection in test cases.
     * @param timeSource TimeSource to set
     */
    public static void setTimeSource(@NotNull TimeSource timeSource) {
        ServerVariables.timeSource = timeSource;
    }

    static {
        for (Variable var : VARS) {
            List<String> names = new ArrayList<String>();
            if (var instanceof MultiVariable) {
                for (String subName : ((MultiVariable) var).getSubNames()) {
                    names.add(var.getName().toLowerCase() + "." + subName.toLowerCase());
                }
            } else {
                names.add(var.getName());
            }

            for (String name : names) {
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

        MultiVariable(final String name, final String[] subNames, final Getter getter) {
            super(name, getter);
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
                if ( context.getService() == null ) {
                    logger.fine( "Can't get operation name because there is no resolved service attached to this context" );
                    return null;
                }
                if ( context.getService().isSoap() ) {
                    final Pair<Binding, Operation> pair = context.getBindingAndOperation();
                    if (pair != null) {
                        Operation operation = pair.right;
                        return operation.getName();
                    }
                } else {
                    logger.fine( "Can't get operation name for a non-SOAP service" );
                }
            } catch ( WSDLException e ) {
                logger.log(Level.WARNING, "Couldn't get operation name, invalid WSDL for service "+context.getService().displayName()+": " + getMessage( e ), getDebugException( e ));
            } catch ( Exception e ) {
                logger.log(Level.WARNING, "Couldn't get operation name: " + getMessage( e ), getDebugException( e ));
            }
            return null;
        }
    }

    private static class SoapNamespaceGetter extends Getter {
        @Override
        public Object get(String name, PolicyEnforcementContext context) {
            SoapKnob soapKnob = context.getRequest().getKnob(SoapKnob.class);
            if (soapKnob == null) {
                logger.fine("Can't get SOAP namespace for non-SOAP message");
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

    private static class DebugTraceGetter extends SelectingGetter implements Setter {
        private DebugTraceGetter(final String baseName) {
            super(baseName);
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof TracePolicyEnforcementContext) {
                return new DebugTraceVariableContext((TracePolicyEnforcementContext) context);
            }
            logger.log(Level.WARNING, "The trace.* variables are only available while processing a debug trace policy.");
            return null;
        }

        @Override
        public void set(String name, Object value, PolicyEnforcementContext context) {
            if (!(context instanceof TracePolicyEnforcementContext)) {
                logger.log(Level.WARNING, "The trace.* variables are only available while processing a debug trace policy.");
                return;
            }

            if ("trace.out".equals(name)) {
                TracePolicyEnforcementContext traceContext = (TracePolicyEnforcementContext) context;
                traceContext.setTraceOut(value);
            } else {
                logger.log(Level.WARNING, "Unable to set trace variable: " + name);
            }
        }

        @Override
        boolean isValidForContext(PolicyEnforcementContext context) {
            return context instanceof TracePolicyEnforcementContext;
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
            } else {
                return new AuditSelector.AuditHolder(AuditContextFactory.getCurrent());
            }
        }
    }

    private static class AuditQueryGetter extends SelectingGetter {
        private AuditQueryGetter(String baseName) {
            super(baseName);
        }


        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof AuditLookupPolicyEnforcementContext) {
                AuditLookupPolicyEnforcementContext lookupctx = (AuditLookupPolicyEnforcementContext) context;
                return   lookupctx.getAuditLookupSearchCriteria();
            }
            return null;
        }

        @Override
        boolean isValidForContext(PolicyEnforcementContext context) {
            return context instanceof AuditLookupPolicyEnforcementContext;
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
        private final boolean targetsAuditedMessage;

        private AuditOriginalMessageGetter(String baseName, boolean targetsResponse, boolean targetsAuditedMessage) {
            super(baseName);
            this.targetsResponse = targetsResponse;
            this.targetsAuditedMessage = targetsAuditedMessage;
        }

        @Override
        protected Object getBaseObject(PolicyEnforcementContext context) {
            if (context instanceof AuditSinkPolicyEnforcementContext) {
                AuditSinkPolicyEnforcementContext auditpec = (AuditSinkPolicyEnforcementContext) context;
                if (targetsAuditedMessage) {
                    return targetsResponse ? auditpec.getAuditedResponse() : auditpec.getAuditedRequest();
                } else {
                    return targetsResponse ? auditpec.getOriginalResponse() : auditpec.getOriginalRequest();
                }
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

    private static abstract class CurrentPolicyGetter extends Getter {
        @Override
        public Object get( final String name, final PolicyEnforcementContext context) {
            final PolicyMetadata meta = context.getCurrentPolicyMetadata();
            final PolicyHeader head = meta == null ? null : meta.getPolicyHeader();
            return head == null ? null : get( head );
        }

        protected abstract Object get( final PolicyHeader policyHeader );
    }

    private static abstract class SelectingGetter extends Getter {
        private final String baseName;

        private SelectingGetter(final String baseName) {
            this.baseName = baseName;
        }

        @Override
        public final Object get(final String name, final PolicyEnforcementContext context) {
            Object object = getBaseObject(context);
            if (object != null && !baseName.equalsIgnoreCase(name)) {
                object = ExpandVariables.processSingleVariableAsObject(
                        "${" + name + "}",
                        Collections.singletonMap(baseName.toLowerCase(), object),
                        new LoggingAudit(logger));
            }
            return object;
        }

        protected abstract Object getBaseObject(PolicyEnforcementContext context);

        /**
         * Create a selecting getter with the given base object.
         */
        static SelectingGetter selectingGetter( final String name, final Object value ) {
            return new SelectingGetter( name ) {
                @Override
                protected Object getBaseObject( final PolicyEnforcementContext context ) {
                    return value;
                }
            };
        }
    }


    private static final Pattern SECPASS_PATTERN = Pattern.compile("^secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext$");

    /**
     * Utility method to expand a variable that is permitted to contain only ${secpass.*.plaintext} references.
     * <p/>
     * This method can work even if a PolicyEnforcementContext is not available.
     * <p/>
     * If a VariableNameSyntaxtException occurs, or if a variable reference other than a secure password reference
     * is detected, this method will audit a warning and return the original string unchanged.
     * <p/>
     * This method will only expand secure password references that are enabled for use via context variables.
     * <p/>
     * This method may be used from server policy assertions that need to prepare a password at assertion compile time
     * (before the first PolicyEnforcementContext is available), but only as a last resort if there is no reasonable
     * alternative.
     *
     * @param audit    auditor to use for logging.  Required.
     * @param template the template to examine.  If null, this method will always return null.  May contain variable
     *                 references but only those of the form ${secpass.*.plaintext}.
     * @return the expansion of the template, or the original string unmodified.  May be null only if template was null.
     * @throws FindException if there is an error looking up a secure password instance.
     */
    public static String expandPasswordOnlyVariable(Audit audit, String template) throws FindException {
        // TODO rewrite assertions that set up passwords at policy-compile-time so that they don't do so, then remove this hacky method and its ridiculous regex
        // TODO or at least move this to somewhere more appropriate
        if (template == null)
            return null;

        try {
            String[] vars = Syntax.getReferencedNames(template);
            if (vars.length < 1)
                return template;

            if (securePasswordManager == null) {
                // Probably running test code or something
                audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression cannot be expanded because a secure password manager is not available; assuming literal password");
                return template;
            }

            // Build variable map, permitting only secpass refs
            Map<String, Object> map = new HashMap<String, Object>();
            for (String var : vars) {
                Matcher matcher = SECPASS_PATTERN.matcher(var);
                if (!matcher.matches()) {
                    audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression referred to non-secpass variable; assuming literal password"); // avoid logging possible password material
                    return template;
                }
                String alias = matcher.group(1);
                assert alias != null; // enforced by regex
                assert alias.length() > 0; // enforced by regex
                SecurePassword secpass = findSecurePasswordByName(alias);
                if (secpass == null) {
                    audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression referred to a nonexistent secure password alias; assuming literal password"); // avoid logging possible password material
                    return template;
                }
                if (!secpass.isUsageFromVariable()) {
                    audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression referred to a secure password alias that does not enable use via context variable; assuming literal password"); // avoid logging possible password material
                    return template;
                }

                char[] plaintext = getPlaintextPassword(secpass);
                String plain;
                if (plaintext == null) {
                    audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Password-only context variable expression referred to a secure password with an empty password; using empty string as password");
                    plain = "";
                } else {
                    plain = new String(plaintext);
                }
                map.put(var, plain);
            }

            return ExpandVariables.process(template, map, audit);

        } catch (VariableNameSyntaxException e) {
            // The previous failures were less likely to occur by accident, but the string "${" on its own is more likely to appear in a legitimate hardcoded password
            audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Password-only context variable expression had invalid syntax; assuming literal password"); // Avoid logging complete exception in case it contains password material
            return template;
        } catch (ParseException e) {
            // avoid chaining parse exception in case it contains password material, as callers are quite likely to just dump it into the log if we do
            // to allow debugging we will log it if debug exceptions are enabled and the log level for this class is elevated
            final String msg = "Password-only context variable expression referred to secure password that could not be decrypted";
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINER, msg, getDebugException( e ));
            throw new FindException(msg);
        }
    }

    private static final Pattern SINGLE_SECPASS_PATTERN = Pattern.compile("^\\$\\{secpass\\.([a-zA-Z_][a-zA-Z0-9_\\-]*)\\.plaintext\\}$");

    /**
     * Utility method to recognize a password that is actually a single ${secpass.*.plaintext} reference.
     * <p/>
     * Note: This method works <b>even if</b> the referenced secure password is not enabled for use via context variable references.
     * <p/>
     * This method should never be used from a server policy assertion.
     *
     * @param audit             auditor to use for logging.  Required.
     * @param passwordOrSecPass a string to examine.  If null or empty or does not strictly match the format of a single ${secpass.*.plaintext} reference then this method will return the original argument unchanged.
     * @return the corresponding secure password plaintext, if the input was a secpass reference; otherwise, the input unchanged.  May be null only if the input is null.
     * @throws FindException if there is an error looking up a secure password instance.
     */
    public static String expandSinglePasswordOnlyVariable(Audit audit, String passwordOrSecPass) throws FindException {
        // TODO rewrite all entities that refer to passwords so that they use SecurePassword foreign key references, then remove this hacky method and its ridiculous regex
        // TODO or at least move this to somewhere more appropriate
        if (passwordOrSecPass == null)
            return null;
        Matcher matcher = SINGLE_SECPASS_PATTERN.matcher(passwordOrSecPass);
        if (!matcher.matches()) {
            // Assume it is a literal password
            return passwordOrSecPass;
        }

        if (securePasswordManager == null) {
            // Probably running test code or something
            audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression cannot be expanded because a secure password manager is not available; assuming literal password");
            return passwordOrSecPass;
        }

        String alias = matcher.group(1);
        assert alias != null; // enforced by regex
        assert alias.length() > 0; // enforced by regex
        SecurePassword secpass = findSecurePasswordByName(alias);
        if (secpass == null) {
            audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression referred to a nonexistent secure password alias; assuming literal password"); // avoid logging possible password material
            return passwordOrSecPass;
        }
        try {
            char[] plaintext = getPlaintextPassword(secpass);
            if (plaintext == null) {
                audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Password-only context variable expression referred to a secure password with an empty password; using empty string as password");
                return "";
            } else {
                return new String(plaintext);
            }
        } catch (ParseException e) {
            // avoid chaining parse exception in case it contains password material, as callers are quite likely to just dump it into the log if we do
            // to allow debugging we will log it if debug exceptions are enabled and the log level for this class is elevated
            final String msg = "Password-only context variable expression referred to secure password that could not be decrypted";
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINER, msg, getDebugException( e ));
            throw new FindException(msg);
        }
    }

    public static String getSecurePasswordByOid(Audit audit, long oid) throws FindException {


        if (securePasswordManager == null) {
            // Probably running test code or something
            audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Password-only context variable expression cannot be expanded because a secure password manager is not available; assuming literal password");
            return null;
        }

        SecurePassword secpass = securePasswordManager.findByPrimaryKey(oid);
        if (secpass == null) {
            audit.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Oid: " + oid + " referred to a nonexistent secure password; using empty string as password"); // avoid logging possible password material
            return "";
        }
        try {
            char[] plaintext = getPlaintextPassword(secpass);
            if (plaintext == null) {
                audit.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Oid: " + oid + " referred to a secure password with an empty password; using empty string as password");
                return "";
            } else {
                return new String(plaintext);
            }
        } catch (ParseException e) {
            // avoid chaining parse exception in case it contains password material, as callers are quite likely to just dump it into the log if we do
            // to allow debugging we will log it if debug exceptions are enabled and the log level for this class is elevated
            final String msg = "Oid: " + oid + " referred to secure password that could not be decrypted";
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINER, msg, getDebugException( e ));
            throw new FindException(msg);
        }
    }

    static SecurePassword findSecurePasswordByName(String secpassName) throws FindException {
        return securePasswordManager.findByUniqueName(secpassName);
    }

    static char[] getPlaintextPassword(SecurePassword securePassword) throws FindException, ParseException {
        String encoded = securePassword.getEncodedPassword();
        return encoded == null || encoded.length() < 1 ? null : securePasswordManager.decryptPassword(encoded);
    }

    static ClusterNodeInfo getSelfNodeInfCached() {
        try {
            return SELF_NODE_INF_CACHE.call();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get self node info: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }
}
