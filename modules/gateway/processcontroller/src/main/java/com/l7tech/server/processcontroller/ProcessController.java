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
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.annotation.Resource;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.*;
import java.net.ConnectException;
import java.net.SocketException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ProcessController {
    private static final Logger logger = Logger.getLogger(ProcessController.class.getName());

    @Resource
    private ConfigService configService;

    /** The maximum amount of time the PC should wait for a node to start */
    private static final int NODE_START_TIME_MAX = 30000;
    /** The amount of time the PC should wait for a node to start before beginning to test whether it's started yet */
    static final int NODE_START_TIME_MIN = 5000;
    /** The amount of time the PC should wait after a node has died before beginning to attempt to restart it */
    private static final int UNSTARTABLE_NODE_RETRY_INTERVAL = 60 * 1000; // one minute
    /** The amount of time the PC should wait after a supposedly running node has stopped responding to pings before killing and restarting it */
    private static final int NODE_CRASH_DETECTION_TIME = 15000;
    /** The amount of time the PC should wait after asking a node to shutdown before killing it */
    static final int DEFAULT_STOP_TIMEOUT = 10000;
    /** The amount of time the PC should wait, during shutdown, after asking a node to shutdown before killing it */ 
    private static final int NODE_SHUTDOWN_TIMEOUT = 5000;

    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<String, NodeState>();
    private final Map<String, ProcessBuilder> processBuilders = new HashMap<String, ProcessBuilder>();

    private static final String LOG_TIMEOUT = "{0} still hasn''t started after {1}ms.  Killing it dead.";

    private volatile boolean daemon;

    public synchronized void stopNode(final String nodeName, final int timeout) {
        logger.info("Stopping " + nodeName);
        final NodeState state = nodeStates.get(nodeName);
        final Process process = state instanceof HasProcess ? ((HasProcess)state).getProcess() : null;

        NodeApi api = state instanceof HasApi ? ((HasApi)state).getApi() : null;
        if (api == null) api = getNodeApi(state.node);

        try {
            api.shutdown();
            nodeStates.put(nodeName, new StoppingNodeState(state.node, process, api, timeout));
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, SocketException.class)) {
                logger.warning("Unable to contact node; assuming it crashed.");
                nodeStates.put(nodeName, new SimpleNodeState(state.node, CRASHED));
            } else {
                logger.log(Level.WARNING, "Unable to contact node, but not for any expected reason. Assuming it's crashed.", e);
                nodeStates.put(nodeName, new SimpleNodeState(state.node, CRASHED));
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
        return Collections.singletonList(SoftwareVersion.fromString("5.0")); //TODO
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
            if (!EnumSet.of(STOPPED, STOPPING, CRASHED, UNKNOWN).contains(state.type)) {
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
                case UNKNOWN:
                case WONT_START:
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
        NodeApi getApi();
    }

    private class RunningNodeState extends SimpleNodeState implements HasProcess, HasApi {
        /** May be null if the PC was started after the node; in such cases the PC cannot force-kill the node without
         * resorting to OS-level help */
        private final Process process;
        private final NodeApi api;

        public RunningNodeState(PCNodeConfig node, Process proc, NodeApi api) {
            super(node, RUNNING);
            this.process = proc;
            this.api = api;
        }

        public Process getProcess() {
            return process;
        }

        public NodeApi getApi() {
            return api;
        }
    }

    private class StoppingNodeState extends SimpleNodeState implements HasProcess, HasApi {
        /** May be null if the PC was started after the node; in such cases the PC cannot force-kill the node without
         * resorting to OS-level help */
        private final Process process;
        private final NodeApi api;
        private final int timeout;

        public StoppingNodeState(PCNodeConfig node, Process proc, NodeApi api, int timeout) {
            super(node, STOPPING);
            this.process = proc;
            this.api = api;
            this.timeout = timeout;
        }

        public Process getProcess() {
            return process;
        }

        public NodeApi getApi() {
            return api;
        }
    }

    private class StoppedNodeState extends NodeState {
        public StoppedNodeState(PCNodeConfig node) {
            super(node, STOPPED);
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
                        startItUp(node);
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
                    startItUp(node);
                    break;
                case STOPPING:
                    // TODO wait for shutdown of the process... Kill the process if it hasn't died after the timeout
                    handleStoppingState(node, (StoppingNodeState)state);
                    break;
                case STOPPED:
                    // TODO Nothing to see here, folks.
                    break;
            }


        }
    }

    private void startItUp(PCNodeConfig node) {
        try {
            nodeStates.put(node.getName(), new StartingNodeState(this, node));
        } catch (IOException e) {
            logger.log(Level.WARNING, node.getName() + " could not be started; will retry on next loop", e);
        }
    }

    private void handleStoppingState(PCNodeConfig node, StoppingNodeState state) {
        final long howLong = System.currentTimeMillis() - state.sinceWhen;
        final boolean timedOut = (howLong > state.timeout);

        if (state.process != null) {
            try {
                final int status = state.process.exitValue();
                logger.info(node.getName() + " exited with status " + status);
                nodeStates.put(node.getName(), new StoppedNodeState(node));
            } catch (IllegalThreadStateException e) {
                if (timedOut) {
                    logger.warning(node.getName() + " has taken " + howLong + "ms to shutdown; killing");
                    state.process.destroy();
                    nodeStates.put(node.getName(), new StoppedNodeState(node));
                } else {
                    logger.fine(node.getName() + " still hasn't stopped after " + howLong + "ms; will wait up to " + state.timeout);
                }
            }
        } else if (state.api != null) {
            try {
                state.api.ping();
            } catch (Exception e) {
                nodeStates.put(node.getName(), new StoppedNodeState(node));
                if (ExceptionUtils.causedBy(e, ConnectException.class))
                    logger.info(node.getName() + " stopped.");
                else
                    logger.log(Level.WARNING, node.getName() + " is probably stopped--TODO detect other kinds of clean shutdown", e);
            }
        } else if (timedOut) {
            osKill(node);
            nodeStates.put(node.getName(), new StoppedNodeState(node));
        }
    }

    private void osKill(PCNodeConfig node) {
        logger.warning("Killing " + node.getName());
        // TODO implement!
        // TODO implement!
        // TODO implement!
        // TODO implement!
        // TODO implement!
    }

    private void handleRunningState(PCNodeConfig node, RunningNodeState state) {
        final long now = System.currentTimeMillis();
        if (state.process != null) {
            try {
                int errorlevel = state.process.exitValue();
                logger.log(Level.WARNING, "{0} crashed with exit code {1}!  Will restart.", new Object[] { node.getName(), errorlevel });
                nodeStates.put(node.getName(), new SimpleNodeState(node, CRASHED));
            } catch (IllegalThreadStateException e) {
                logger.fine(node.getName() + " is still alive");
            }
        }

        try {
            state.api.ping();
            state.sinceWhen = now;
        } catch (Exception e) {
            final long howLong = now - state.sinceWhen;
            if (howLong > NODE_CRASH_DETECTION_TIME) {
                logger.log(Level.WARNING, MessageFormat.format("{0} is supposedly running but has not responded to a ping in {1}ms.  Killing and restarting.", node.getName(), howLong), e);
                if (state.process != null) {
                    state.process.destroy();
                } else {
                    osKill(node);
                }

                nodeStates.put(node.getName(), new SimpleNodeState(node, CRASHED));
            } else {
                logger.log(Level.WARNING, MessageFormat.format("{0} is supposedly running but has not responded to a ping in {1}ms.  Will keep retrying up to {2} ms.", node.getName(), howLong, NODE_CRASH_DETECTION_TIME), e);
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
            nodeStates.put(node.getName(), new RunningNodeState(node, state.getProcess(), state.getApi()));
        } else if (status == StartingNodeState.STARTING) {
            final long howLong = now - state.sinceWhen;
            if (howLong <= NODE_START_TIME_MAX) {
                // Ask me again later
                state.sinceWhen = now;
                return;
            }

            // Timed out!
            logger.log(Level.WARNING, LOG_TIMEOUT, new Object[] { node.getName(), NODE_START_TIME_MAX });

            if (state.getProcess() != null) state.getProcess().destroy();

            final Pair<byte[], byte[]> byteses = state.finishOutput();

            logger.warning(node.getName() + " wouldn't start after " + howLong + "ms; copying its output:");
            spew("STDERR", byteses.left);
            spew("STDOUT", byteses.right);

            nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
        } else if (status instanceof StartingNodeState.Died) {
            final StartingNodeState.Died died = (StartingNodeState.Died)status;
            logger.warning(node.getName() + " crashed on startup with exit code " + died.exitValue);

            logger.warning(node.getName() + " crashed on startup; copying its output:");
            spew("STDERR", died.stderr);
            spew("STDOUT", died.stdout);

            nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
        } else {
            logger.warning("Unexpected StartStatus: " + status);
            nodeStates.put(node.getName(), new SimpleNodeState(node, WONT_START));
        }
    }

    private void handleUnknownState(PCNodeConfig node) {
        final NodeApi api = getNodeApi(node);
        try {
            api.ping();
            if (daemon && !node.isEnabled()) {
                logger.info("Stopping disabled node " + node.getName());
                api.shutdown();
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
                    cmds = new LinkedList<String>(
                        Arrays.asList(
                            "/opt/SecureSpan/Appliance/libexec/gateway_control",
                            "run",
                            "-J-Dcom.l7tech.server.home=\"" + ssgPwd.getCanonicalPath() + "\"",
                            "-J-Dcom.l7tech.server.processControllerPresent=true",
                            "-J-Djava.util.logging.config.class=com.l7tech.server.log.JdkLogConfig",
                            "-J-Dcom.l7tech.server.log.console=true"
                        )
                    );
                    
                    if ( node.getClusterHostname() != null ) {
                        cmds.add("-J-Dcom.l7tech.server.defaultClusterHostname=\"" + node.getClusterHostname() + "\"");
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

    private void collectArgs(List<String> cmds, Feature feature) {
        if (feature instanceof HasCommandLineArguments) {
            cmds.addAll(Arrays.asList(((HasCommandLineArguments)feature).getArguments()));
        }
    }

    /**
     * Invoke the node API.
     * 
     * @param nodeName the name of the node to invoke the API on
     * @param callable the function to call the NodeApi with
     * @return the API method's return value
     * @throws IOException if the node API cannot be obtained (e.g. because the node is down)
     */
    public <T> T callNodeApi(String nodeName, Functions.UnaryThrows<T, NodeApi, Exception> callable) throws Exception {
        NodeState state = nodeStates.get(nodeName);
        if (state == null) throw new IllegalStateException("Unknown node " + nodeName);
        if (state instanceof HasApi) {
            NodeApi napi = ((HasApi) state).getApi();
            return callable.call(napi);
        } else {
            throw new IOException(nodeName + " is currently uncommunicative (" + state.type + ")");
        }
    }

    NodeApi getNodeApi(PCNodeConfig node) {
        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
        pfb.setServiceClass(NodeApi.class);
        final String url = node.getProcessControllerApiUrl();
        pfb.setAddress(url == null ? "https://localhost:2124/ssg/services/processControllerNodeApi" : url);
        Client c = pfb.getClientFactoryBean().create();
        HTTPConduit httpConduit = (HTTPConduit)c.getConduit();
        httpConduit.setTlsClientParameters(new TLSClientParameters() {
            public boolean isDisableCNCheck() {
                return true;
            }

            public TrustManager[] getTrustManagers() {
                return new TrustManager[] { new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {}
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                    public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                }};
            }
        });
        return (NodeApi)pfb.create();
    }

    private void spew(String what, byte[] outBytes) {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outBytes) /* TODO Note platform default encoding is actually wanted here */));
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
            logger.info(name + " stopping"); // TODO find some cleaner way of ensuring this log text gets flushed promptly
            final NodeState state = entry.getValue();
            if (EnumSet.of(RUNNING, UNKNOWN, STARTING).contains(state.type)) {
                stopNode(name, NODE_SHUTDOWN_TIMEOUT);
            }
        }
    }

}
