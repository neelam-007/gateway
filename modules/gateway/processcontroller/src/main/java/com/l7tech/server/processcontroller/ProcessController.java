/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.NodeStateType;
import static com.l7tech.server.management.NodeStateType.*;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.Feature;
import com.l7tech.server.management.config.HasCommandLineArguments;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.host.HostFeature;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.NodeFeature;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.ProcUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.*;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ProcessController implements InitializingBean {
    private static final Logger logger = Logger.getLogger(ProcessController.class.getName());

    @Resource
    private ConfigService configService;

    /** The maximum amount of time the PC should wait for a node to start */
    private int NODE_START_TIME_MAX = 60000;
    /** The amount of time the PC should wait for a node to start before beginning to test whether it's started yet */
    private int NODE_START_TIME_MIN = 5000;
    /** The amount of time the PC should wait after a node has died before beginning to attempt to restart it */
    private int UNSTARTABLE_NODE_RETRY_INTERVAL = 60 * 1000; // one minute
    /** The amount of time the PC should wait after a supposedly running node has stopped responding to pings before killing and restarting it */
    private int NODE_CRASH_DETECTION_TIME = 15000;
    /** The amount of time the PC should wait after asking a node to shutdown before killing it */
    private int DEFAULT_STOP_TIMEOUT = 10000;
    /** The amount of time the PC should wait between checks to see if someone else started the node */
    private int DEFAULT_STOPPED_TIMEOUT = 30000;
    /** The amount of time the PC should wait, during shutdown, after asking a node to shutdown before killing it */
    private int NODE_SHUTDOWN_TIMEOUT = 5000;
    /** The amount of time the PC should wait for nodes to shutdown before exiting */
    private int PC_SHUTDOWN_TIMEOUT = 15000;
    /** Should the PC ever kill an unresponsive running node? **/
    private boolean KILL_RUNNING_NODE = true;

    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<String, NodeState>();
    private final Map<String, ProcessBuilder> processBuilders = new HashMap<String, ProcessBuilder>();

    private static final String LOG_TIMEOUT = "{0} still hasn''t started after {1}ms.  Killing it dead.";

    private volatile boolean daemon;
    private static final int NODEAPI_FAST_CONNECT_TIMEOUT = 2000;
    private static final int NODEAPI_FAST_RECEIVE_TIMEOUT = 2000;
    private static final int NODEAPI_CONNECT_TIMEOUT = 30000;
    private static final int NODEAPI_RECEIVE_TIMEOUT = 60000;

    public int getStopTimeout() {
        return DEFAULT_STOP_TIMEOUT;
    }

    public int getNodeStartTimeMin() {
        return NODE_START_TIME_MIN;
    }

    @Override
    public void afterPropertiesSet() {
        NODE_START_TIME_MAX = configService.getIntProperty( "host.controller.nodeStartTimeMax", NODE_START_TIME_MAX );
        NODE_START_TIME_MIN = configService.getIntProperty( "host.controller.nodeStartTimeMin", NODE_START_TIME_MIN );
        NODE_CRASH_DETECTION_TIME = configService.getIntProperty( "host.controller.crashDetectionTime", NODE_CRASH_DETECTION_TIME );
        DEFAULT_STOP_TIMEOUT = configService.getIntProperty( "host.controller.nodeStopTimeout", DEFAULT_STOP_TIMEOUT );
        NODE_SHUTDOWN_TIMEOUT = configService.getIntProperty( "host.controller.nodeShutdownTimeout", NODE_SHUTDOWN_TIMEOUT );
        PC_SHUTDOWN_TIMEOUT = configService.getIntProperty( "host.controller.pcShutdownTimeout", PC_SHUTDOWN_TIMEOUT );
        KILL_RUNNING_NODE = configService.getBooleanProperty( "host.controller.restartRunningNode", KILL_RUNNING_NODE);
    }

    public synchronized void stopNode(final String nodeName, final int timeout) {
        logger.info("Stopping " + nodeName);
        final NodeState state = nodeStates.get(nodeName);
        final Process process = state instanceof HasProcess ? ((HasProcess)state).getProcess() : null;

        HasApi api = state instanceof HasApi ? ((HasApi)state).getApiHaver() : null;
        if (api == null) api = getNodeApi(state.node);

        try {
            api.getApi(false).shutdown();
            nodeStates.put(nodeName, new StoppingNodeState(state.node, process, api, timeout));
        } catch (Exception e) {
            nodeStates.put(nodeName, new StoppedNodeState(state.node, api, DEFAULT_STOPPED_TIMEOUT));
            boolean dead = false;
            if ( process != null ) {
                try {
                    process.exitValue();
                    // if we get here it is dead
                    dead = true;
                    if ( isNetworkException(e) ) {
                        logger.warning("Unable to contact node; assuming it crashed.");
                    } else {
                        logger.log(Level.WARNING, "Unable to contact node, but not for any expected reason. Assuming it's crashed.", e);
                    }
                } catch (IllegalThreadStateException itse) {
                    // is alive, so will be killed below
                }
            }

            if ( !dead ) {
                // TODO do an OS shutdown and put into shutting down state ...
                if ( isNetworkException(e) ) {
                    logger.warning("Unable to contact node; killing process.");
                } else {
                    logger.log(Level.WARNING, "Unable to contact node, but not for any expected reason.  killing process.", e);
                }
                osKill(state.node);
            }
        }
    }

    Process spawnProcess(PCNodeConfig node) throws IOException {
        // TODO Should the OS helper be doing this (once we have one)?
        ProcessBuilder builder = getProcessBuilder(node);
        if (builder == null) throw new IllegalStateException("Don't know how to start " + node.getName());
        return builder.start();
    }

    /**
     * @return a Pair containing the node's state, and the time at which the state was last observed. Never null.
     */
    public synchronized NodeStatus getNodeStatus(String nodeName) {
        final NodeState state = nodeStates.get(nodeName);
        if (state != null) 
            return new NodeStatus(state.type, new Date(state.startTime), new Date(state.sinceWhen));

        final Date now = new Date();
        return new NodeStatus(NodeStateType.UNKNOWN, now, now);
    }

    /**
     * @param node the node that should be started.
     * @param sync true if this API should wait until the node has been started before returning.
     * @throws IOException
     */
    public synchronized void startNode(PCNodeConfig node, boolean sync) throws IOException {
        nodeStates.put(node.getName(), new StartingNodeState(this, node));
        if (!sync) return;
        NodeState state;
        do {
            visitNodes();
            state = nodeStates.get(node.getName());
        } while (state.type == NodeStateType.STARTING);
    }

    public synchronized void startNode(String nodeName, boolean sync) throws IOException {
        final PCNodeConfig node = (PCNodeConfig)configService.getHost().getNodes().get(nodeName);
        if (node == null) throw new IllegalArgumentException(nodeName + " does not exist");
        startNode(node, sync);
    }

    public List<SoftwareVersion> getAvailableNodeVersions() {
        return Collections.singletonList(SoftwareVersion.fromString("5.0"));
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * Note that this method must not be synchronized because it relies on the PC's main loop coming along and changing
     * node states asynchronously.
     *
     * @param nodeName
     * @param shutdownTimeout
     */
    public void deleteNode(String nodeName, int shutdownTimeout) {
        NodeState state;
        synchronized (this) {
            state = nodeStates.get(nodeName);
            if (!EnumSet.of(STOPPED, STOPPING, CRASHED).contains(state.type)) {
                stopNode(nodeName, shutdownTimeout);
            }
            notifyAll();
        }

        waiting: while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            state = nodeStates.get(nodeName);
            if (state == null) throw new IllegalStateException("No known state for " + nodeName);

            switch (state.type) {
                case STOPPING:
                case RUNNING:
                    logger.info("Waiting for shutdown...");
                    continue waiting;
                case STOPPED:
                    logger.info("Node is shutdown");
                    break waiting;
                case CRASHED:
                case WONT_START:
                    logger.info("Node is not running.");
                    break waiting;
                case UNKNOWN:
                case STARTING:
                    throw new IllegalStateException("Unexpected state for " + nodeName + " after shutdown: " + state.type);
            }
        }
        nodeStates.remove(nodeName);
    }

    public Map<String, NodeStatus> listNodes() {
        final Map<String, NodeStatus> result = new HashMap<String, NodeStatus>();
        for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
            final NodeState state = entry.getValue();
            result.put(entry.getKey(), new NodeStatus(state.type, new Date(state.startTime), new Date(state.sinceWhen)));
        }
        return result;
    }

    public boolean isDisabledApiException( final Exception exception ) {
        return ExceptionUtils.getMessage(exception).endsWith("This request cannot be accepted on this port.");
    }

    private static abstract class NodeState {
        protected final PCNodeConfig node;
        protected final NodeStateType type;
        protected final long startTime = System.currentTimeMillis();
        protected volatile long sinceWhen;

        public NodeState(PCNodeConfig node, NodeStateType type) {
            this.type = type;
            this.node = node;
            this.sinceWhen = System.currentTimeMillis();
        }
    }

    static class SimpleNodeState extends NodeState {
        public SimpleNodeState(PCNodeConfig node, NodeStateType type) {
            super(node, type);
        }
    }

    static interface HasProcess {
        Process getProcess();
    }

    static interface HasApi {
        /**
         * Get the NodeApi
         *
         * @param fastTimeout True to get an API with fast timeouts.
         * @return The NodeApi
         */
        NodeApi getApi( boolean fastTimeout );

        /**
         * Get the underlying HasApi.
         *
         * @return The wrapped HasApi, or this
         */
        HasApi getApiHaver();
    }

    private class RunningNodeState extends SimpleNodeState implements HasProcess, HasApi {
        /** May be null if the PC was started after the node; in such cases the PC cannot force-kill the node without
         * resorting to OS-level help */
        private final Process process;
        private final HasApi hasApi;

        public RunningNodeState( PCNodeConfig node, Process proc, HasApi hasApi ) {
            super(node, RUNNING);
            this.process = proc;
            this.hasApi = hasApi;
        }

        @Override
        public Process getProcess() {
            return process;
        }

        @Override
        public NodeApi getApi( final boolean fastTimeout ) {
            return hasApi.getApi( fastTimeout );
        }

        @Override
        public HasApi getApiHaver() {
            return hasApi;
        }
    }

    private class StoppingNodeState extends SimpleNodeState implements HasProcess, HasApi {
        /** May be null if the PC was started after the node; in such cases the PC cannot force-kill the node without
         * resorting to OS-level help */
        private final Process process;
        private final HasApi hasApi;
        private final int timeout;

        public StoppingNodeState(PCNodeConfig node, Process proc, HasApi hasApi, int timeout) {
            super(node, STOPPING);
            this.process = proc;
            this.hasApi = hasApi;
            this.timeout = timeout;
        }

        @Override
        public Process getProcess() {
            return process;
        }

        @Override
        public NodeApi getApi( boolean fastTimeout ) {
            return hasApi.getApi( fastTimeout );
        }

        @Override
        public HasApi getApiHaver() {
            return hasApi;
        }
    }

    private class StoppedNodeState extends NodeState {
        private final HasApi hasApi;
        private final int timeout;

        public StoppedNodeState(PCNodeConfig node, HasApi hasApi, int timeout) {
            super(node, STOPPED);
            this.hasApi = hasApi;
            this.timeout = timeout;
        }

        public HasApi getHasApi() {
            return hasApi;
        }
    }

    synchronized void visitNodes() {
        final Collection<NodeConfig> nodeConfigs = configService.getHost().getNodes().values();
        if (nodeConfigs.isEmpty()) return;

        // TODO when do we notice if a node has been deleted or disabled?

        for (NodeConfig _node : nodeConfigs) {
            final PCNodeConfig node = (PCNodeConfig)_node;
            final NodeState state = nodeStates.get(node.getName());
            final NodeStateType stateType = state == null ? UNKNOWN : state.type;
            switch (stateType) {
                case UNKNOWN:
                    handleUnknownState(node);
                    break;
                case STARTING:
                    handleStartingState(node, (StartingNodeState)state);
                    break;
                case WONT_START:
                    if (System.currentTimeMillis() - state.sinceWhen > UNSTARTABLE_NODE_RETRY_INTERVAL) {
                        logger.info(node.getName() + " wouldn't start; restarting...");
                        handleUnknownState(node);
                        break;
                    } else {
                        logger.fine(node.getName() + " wouldn't start; waiting " + UNSTARTABLE_NODE_RETRY_INTERVAL + "ms before attempting to restart");
                        break;
                    }
                case RUNNING:
                    handleRunningState(node, (RunningNodeState)state);
                    break;
                case CRASHED:
                    logger.info(node.getName() + " crashed; restarting...");
                    handleUnknownState(node);
                    break;
                case STOPPING:
                    // TODO wait for shutdown of the process... Kill the process if it hasn't died after the timeout
                    handleStoppingState(node, (StoppingNodeState)state);
                    break;
                case STOPPED:
                    handleStoppedState(node, (StoppedNodeState)state);
                    break;
            }


        }
    }

    private void handleStoppingState(PCNodeConfig node, StoppingNodeState state) {
        final long howLong = System.currentTimeMillis() - state.sinceWhen;
        final boolean timedOut = (howLong > state.timeout);

        if (state.process != null) {
            try {
                final int status = state.process.exitValue();
                logger.info(node.getName() + " exited with status " + status);
                nodeStates.put(node.getName(), new StoppedNodeState(node, state.hasApi, DEFAULT_STOPPED_TIMEOUT));
            } catch (IllegalThreadStateException e) {
                if (timedOut) {
                    logger.warning(node.getName() + " has taken " + howLong + "ms to shutdown; killing");
                    osKill(node);
                    nodeStates.put(node.getName(), new StoppedNodeState(node, state.hasApi, DEFAULT_STOPPED_TIMEOUT));
                } else {
                    logger.fine(node.getName() + " still hasn't stopped after " + howLong + "ms; will wait up to " + state.timeout);
                }
            }
        } else if (state.hasApi != null) {
            try {
                state.hasApi.getApi(false).ping();
            } catch (Exception e) {
                if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                    nodeStates.put(node.getName(), new StoppedNodeState(node, state.hasApi, DEFAULT_STOPPED_TIMEOUT));
                    logger.info(node.getName() + " stopped.");
                } else {
                    logger.log(Level.FINE, "Error when checking node "+node.getName()+" status during shutdown '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                }
            }
        } else if (timedOut) {
            osKill(node);
            nodeStates.put(node.getName(), new StoppedNodeState(node, state.hasApi, DEFAULT_STOPPED_TIMEOUT));
        }
    }

    private void handleStoppedState(PCNodeConfig node, StoppedNodeState state) {
        final long howLong = System.currentTimeMillis() - state.sinceWhen;
        final boolean timedOut = (howLong > state.timeout);

        if ( timedOut ) {
            if (state.hasApi != null) {
                try {
                    state.hasApi.getApi(false).ping();
                    logger.info(node.getName() + " start detected.");
                    nodeStates.put(node.getName(), new RunningNodeState(node, null, state.hasApi));
                } catch (Exception e) {
                    if ( ExceptionUtils.causedBy(e, ConnectException.class) ) {
                        logger.fine("Node " + node.getName() + " is not running.");
                    } else {
                        logger.log(Level.FINE, "Error when checking node "+node.getName()+" status when stopped '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
                    }
                }
            }
        }
    }

    private void osKill( final PCNodeConfig node ) {
        logger.warning("Killing " + node.getName());
        File ssgPwd = getNodeDirectory(node.getName());

        File gatewayShutdown = new File("/opt/SecureSpan/Appliance/libexec/gateway_control");
        if ( gatewayShutdown.exists() ) {
            try {
                ProcUtils.exec(ssgPwd, gatewayShutdown, new String[]{"stop", "-force"}, null, false );
            } catch ( IOException ioe ) {
                logger.log(Level.WARNING, "Failed to kill node '"+node.getName()+"':\n" + ioe.getMessage() );
            }
        } else {
            logger.warning("Gateway OS shutdown script not present, node '"+node.getName()+"' not stopped.");
        }
    }

    File getNodeDirectory(String name) {
        final File nodesDir = configService.getNodeBaseDirectory();
        if (!(nodesDir.exists() && nodesDir.isDirectory())) throw new IllegalStateException("Couldn't find node directory " + nodesDir.getAbsolutePath());

        File ssgPwd = new File(nodesDir, name);
        if (!(ssgPwd.exists() && ssgPwd.isDirectory())) throw new IllegalStateException("Node directory " + ssgPwd.getAbsolutePath() + " does not exist or is not a directory");
        return ssgPwd;
    }

    private void handleRunningState(PCNodeConfig node, RunningNodeState state) {
        final long now = System.currentTimeMillis();
        boolean running = false;
        if (state.process != null) {
            try {
                int errorlevel = state.process.exitValue();
                logger.log(Level.WARNING, "{0} crashed with exit code {1}!  Will restart.", new Object[] { node.getName(), errorlevel });
                nodeStates.put(node.getName(), new SimpleNodeState(node, CRASHED));
            } catch (IllegalThreadStateException e) {
                running = true;
                logger.fine(node.getName() + " is still alive");
            }
        }

        try {
            state.hasApi.getApi(false).ping();
            state.sinceWhen = now;
        } catch (Exception e) {
            final long howLong = now - state.sinceWhen;
            if ( KILL_RUNNING_NODE || !running ) {
                if (howLong > NODE_CRASH_DETECTION_TIME) {
                    logger.log(Level.WARNING, MessageFormat.format("{0} is supposedly running but has not responded to a ping in {1}ms.  Killing and restarting.", node.getName(), howLong), filterException(e));
                    osKill(node);

                    nodeStates.put(node.getName(), new SimpleNodeState(node, CRASHED));
                }  else {
                    logger.log(Level.WARNING, MessageFormat.format("{0} is supposedly running but has not responded to a ping in {1}ms.  Will keep retrying up to {2} ms.", node.getName(), howLong, NODE_CRASH_DETECTION_TIME), filterException(e));
                }
            } else {
                logger.log(Level.WARNING, MessageFormat.format("{0} is supposedly running but has not responded to a ping in {1}ms.", node.getName(), howLong), filterException(e));
            }
        }
    }

    /**
     * Retry pings, counting retries and waiting for either successful startup or a timeout, whichever comes first.
     */
    private void handleStartingState(PCNodeConfig node, StartingNodeState state) {
        final StartingNodeState.StartStatus status = state.getStatus();
        final long now = System.currentTimeMillis();

        if (status == StartingNodeState.STARTED) {
            logger.info(node.getName() + " started");
            nodeStates.put(node.getName(), new RunningNodeState(node, state.getProcess(), state.getApiHaver()));
        } else if (status == StartingNodeState.STARTING) {
            final long howLong = now - state.sinceWhen;
            if (howLong <= NODE_START_TIME_MAX) {
                // Ask me again later
                state.sinceWhen = now;
                return;
            }

            // Timed out!
            logger.log(Level.WARNING, LOG_TIMEOUT, new Object[] { node.getName(), NODE_START_TIME_MAX });
            osKill(node);

            final Pair<byte[], byte[]> byteses = state.finishOutput();

            logger.warning(node.getName() + " wouldn't start after " + howLong + "ms; copying its output:");
            spew("STDERR", byteses.left);
            spew("STDOUT", byteses.right);

            nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
        } else if (status instanceof StartingNodeState.Died) {
            final StartingNodeState.Died died = (StartingNodeState.Died)status;
            if ( died.exitValue == 33 ) {
                logger.warning(node.getName() + " is already running");    
                nodeStates.put(node.getName(), new RunningNodeState(node, null, getNodeApi(node)));
            } else {
                logger.warning(node.getName() + " crashed on startup with exit code " + died.exitValue);

                logger.warning(node.getName() + " crashed on startup; copying its output:");
                spew("STDERR", died.stderr);
                spew("STDOUT", died.stdout);

                nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
            }
        } else {
            logger.warning("Unexpected StartStatus: " + status);
            nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
        }
    }

    private void handleUnknownState(PCNodeConfig node) {
        final HasApi api = getNodeApi(node);
        try {
            api.getApi(false).ping();
            if (daemon && !node.isEnabled()) {
                logger.info("Stopping disabled node " + node.getName());
                api.getApi(false).shutdown();
                nodeStates.put(node.getName(), new StoppingNodeState(node, null, api, DEFAULT_STOP_TIMEOUT));
            } else {
                logger.info(node.getName() + " is already running");
                nodeStates.put(node.getName(), new RunningNodeState(node, null, api));
            }
        } catch (Exception e) {
            if (daemon && e instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException)e;
                if (NodeApi.NODE_NOT_CONFIGURED_FOR_PC.equals(sfe.getFault().getFaultString())) {
                    logger.warning(node.getName() + " is already running but has not been configured for use with the PC; will try again later");
                    nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
                    return;
                }
            }

            if (!daemon) return;

            // TODO what about other kinds of node-is-still-running? We want to avoid spinning helplessly on stuff like "address already in use".
            logger.log(Level.FINE, node.getName() + " isn't running", e);
            try {
                StartingNodeState startingState = new StartingNodeState(this, node);
                nodeStates.put(node.getName(), startingState);
            } catch (IOException e1) {
                logger.log(Level.WARNING, "Unable to start " + node + "; will retry", e1);
            }
        }
    }

    private ProcessBuilder getProcessBuilder(PCNodeConfig node) {
        // TODO what if the node's config changes in any meaningful way?
        ProcessBuilder processBuilder = processBuilders.get(node.getName());
        if (processBuilder != null) return processBuilder;

        final HostConfig.OSType osType = configService.getHost().getOsType();
        final List<String> cmds;
        final File ssgPwd;
        // TODO use info from the host profile
        switch(osType) {
            case RHEL:
                final File nodesDir = configService.getNodeBaseDirectory();
                if (!(nodesDir.exists() && nodesDir.isDirectory())) throw new IllegalStateException("Couldn't find node directory " + nodesDir.getAbsolutePath());

                ssgPwd = new File(nodesDir, node.getName());
                if (!(ssgPwd.exists() && ssgPwd.isDirectory())) throw new IllegalStateException("Node directory " + ssgPwd.getAbsolutePath() + " does not exist or is not a directory");
                try {
                    // TODO make this less hard-coded (e.g. use the host profile)
                    cmds = getGatewayLauncher( ssgPwd );
                    
                    if ( node.getClusterHostname() != null ) {
                        cmds.add("-J-Dcom.l7tech.server.defaultClusterHostname=\"" + node.getClusterHostname() + "\"");
                    }

                    if (configService.isUseSca()) { // TODO this should just use the ScaFeature
                        cmds.add("-J-Dcom.l7tech.server.sca.enable=true");
                    }

                    for (HostFeature hf : node.getHost().getFeatures()) {  // TODO needs more scala.Seq#filter
                        collectArgs(cmds, hf);
                    }

                    for (NodeFeature nf : node.getFeatures()) {
                        collectArgs(cmds, nf);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e); 
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        processBuilder = new ProcessBuilder(cmds.toArray(new String[cmds.size()]));
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(ssgPwd);
        processBuilders.put(node.getName(), processBuilder);
        return processBuilder;
    }

    private List<String> getGatewayLauncher( final File ssgPwd ) throws IOException {
        List<String> commands = new LinkedList<String>();

        String ssgHome = ssgPwd.getCanonicalPath();
        String applianceLauncher = "/opt/SecureSpan/Appliance/libexec/gateway_control";
        if ( new File(applianceLauncher).exists() ) {
            commands.add( applianceLauncher );
            commands.add( "run" );
            addGatewaySystemProperties( commands, "-J-D", ssgHome );
        } else {
            commands.add( System.getProperty("java.home") + "/bin/java" );
            addGatewaySystemProperties( commands, "-D", ssgHome );
            commands.add( "-jar" );
            commands.add( "../../runtime/Gateway.jar" );
        }

        return commands;
    }

    private void addGatewaySystemProperties( final List<String> commands, final String propPrefix, final String ssgHome ) {
        commands.add( propPrefix + "com.l7tech.server.home=\"" + ssgHome + "\"" );
        commands.add( propPrefix + "com.l7tech.server.processControllerPresent=true" );
        commands.add( propPrefix + "java.util.logging.config.class=com.l7tech.server.log.JdkLogConfig" );
        commands.add( propPrefix + "com.l7tech.server.log.console=true" );
    }

    private void collectArgs(List<String> cmds, Feature feature) {
        if (feature instanceof HasCommandLineArguments) {
            cmds.addAll(Arrays.asList(((HasCommandLineArguments)feature).getArguments()));
        }
    }

    /**
     * Invoke the node API.
     * 
     * @param nodeName the name of the node to invoke the API on, or null to pick the first node in the system
     * @param callable the function to call the NodeApi with
     * @return the API method's return value
     * @throws TemporarilyUnavailableException if the node API cannot be obtained (e.g. because the node is down)
     */
    public <T, E extends Exception> T callNodeApi(String nodeName, Functions.UnaryThrows<T, NodeApi, E> callable) throws E, TemporarilyUnavailableException {
        if (nodeStates.isEmpty()) throw new TemporarilyUnavailableException(nodeName, NodeStateType.UNKNOWN);
        final NodeState state;
        if (nodeName == null) {
            state = nodeStates.values().iterator().next();
        } else {
            state = nodeStates.get(nodeName);
        }
        if (state == null) throw new IllegalStateException("Unknown node " + nodeName);
        if (state instanceof HasApi) {
            NodeApi napi = ((HasApi) state).getApi(true);
            if (napi == null) throw new TemporarilyUnavailableException(state.node.getName(), state.type);
            return callable.call(napi);
        } else {
            throw new TemporarilyUnavailableException(state.node.getName(), state.type);
        }
    }

    public static class TemporarilyUnavailableException extends Exception {
        private final String name;
        private final NodeStateType type;

        public TemporarilyUnavailableException(String name, NodeStateType type) {
            super("Node " + name + " is temporarily unavailable (last known state was " + type + ")");
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public NodeStateType getType() {
            return type;
        }
    }

    public HasApi getNodeApi( final PCNodeConfig node ) {
        final int connectTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_CONNECT, NODEAPI_CONNECT_TIMEOUT);
        final int receiveTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_SLOW_READ, NODEAPI_RECEIVE_TIMEOUT);
        final int fastConnectTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_FAST_CONNECT, NODEAPI_FAST_CONNECT_TIMEOUT);
        final int fastReceiveTimeout = configService.getIntProperty(ConfigService.HOSTPROPERTIES_SAMPLER_TIMEOUT_FAST_READ, NODEAPI_FAST_RECEIVE_TIMEOUT);

        return new HasApi(){
            private NodeApi fastApi;
            private NodeApi api;

            @Override
            public NodeApi getApi( final boolean fastTimeout ) {
                NodeApi api;
                if ( fastTimeout ) {
                    api = this.fastApi;
                    if ( api == null ) {
                        api = this.fastApi = getNodeApi( node, fastReceiveTimeout, fastConnectTimeout );
                    }
                } else {
                    api = this.api;
                    if ( api == null ) {
                        api = this.api = getNodeApi( node, receiveTimeout, connectTimeout );
                    }
                }
                return api;
            }

            @Override
            public HasApi getApiHaver() {
                return this;
            }
        };
    }

    public NodeApi getNodeApi(PCNodeConfig node, int receiveTimeout, int connectTimeout) {
        String name;
        if (node == null) {
            name = nodeStates.keySet().iterator().next();
            node = (PCNodeConfig) configService.getHost().getNodes().get(name);
        } else {
            name = node.getName();
        }

        String autoUrl = null;
        final File portfile = new File(new File(getNodeDirectory(name), "var"), "processControllerPort");
        if (portfile.exists()) {
            logger.info("Getting API port from " + portfile.getAbsolutePath());
            try {
                int port = Integer.valueOf(new String(IOUtils.slurpFile(portfile), "UTF-8"));
                autoUrl = String.format("https://localhost:%d/ssg/services/processControllerNodeApi", port);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Couldn't read API port file; will try default port", e);
            }
        } else {
            logger.log(Level.INFO, "{0} does not exist yet, will try default port", portfile.getAbsolutePath());
        }

        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
        pfb.setServiceClass(NodeApi.class);

        String url = node.getProcessControllerApiUrl();
        if (url == null) url = autoUrl;
        if (url == null) url = "https://localhost:2124/ssg/services/processControllerNodeApi";
        pfb.setAddress(url);
        final Client c = pfb.getClientFactoryBean().create();
        final HTTPConduit httpConduit = (HTTPConduit)c.getConduit();

        final HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
        clientPolicy.setConnectionTimeout(connectTimeout);
        clientPolicy.setReceiveTimeout(receiveTimeout);
        httpConduit.setClient(clientPolicy);

        httpConduit.setTlsClientParameters(new TLSClientParameters() {
            @Override
            public boolean isDisableCNCheck() {
                return true;
            }

            @Override
            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }
        });
        return (NodeApi)pfb.create();
    }

    private void spew(String what, byte[] outBytes) {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outBytes) /* Note platform default encoding is actually wanted here */));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                logger.warning("        " + what + ": " + line);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Couldn't spew stdout!", e); // Extremely Unlikely
        }
    }

    synchronized void stopNodes() {
        // Reset any running node states to STOPPING
        for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
            final String name = entry.getKey();
            logger.info(name + " stopping");
            final NodeState state = entry.getValue();
            if (EnumSet.of(RUNNING, UNKNOWN, STARTING).contains(state.type)) {
                stopNode(name, NODE_SHUTDOWN_TIMEOUT);
            }
        }

        long time = System.currentTimeMillis();
        while( (System.currentTimeMillis()-PC_SHUTDOWN_TIMEOUT) < time ) {
            boolean allStopped = true;
            for (Map.Entry<String, NodeState> entry : nodeStates.entrySet()) {
                final NodeState state = entry.getValue();
                if ( !EnumSet.of(WONT_START, CRASHED, STOPPED).contains(state.type) ) {
                    allStopped = false;
                }
            }
            
            if ( allStopped ) break;

            try {
                Thread.sleep( 1000L );
            } catch ( InterruptedException ie ) {
                break;
            }
        }
    }

    private Exception filterException( final Exception thrown ) {
        Exception showThrown = null;

        if ( isNetworkException(thrown) ) {
            showThrown = ExceptionUtils.getDebugException( thrown );    
        } else if ( !isDisabledApiException(thrown) ) {
            showThrown = thrown;
        }

        return showThrown;
    }

    public static boolean isNetworkException( final Exception exception ) {
        return ExceptionUtils.causedBy( exception, ConnectException.class ) ||
               ExceptionUtils.causedBy( exception, NoRouteToHostException.class ) ||
               ExceptionUtils.causedBy( exception, UnknownHostException.class ) ||
               ExceptionUtils.causedBy( exception, SocketTimeoutException.class );
    }


}
