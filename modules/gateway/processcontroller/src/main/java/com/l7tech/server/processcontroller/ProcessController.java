/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.SoftwareVersion;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.*;
import java.net.ConnectException;
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

    @Resource
    private ApplicationContext spring;

    ProcessBuilder processBuilder;

    /** The maximum amount of time the PC should wait for a node to start */
    private static final int NODE_START_TIME_MAX = 30000;
    /** The amount of time the PC should wait for a node to start before beginning to test whether it's started yet */
    static final int NODE_START_TIME_MIN = 5000;
    /** The amount of time the PC should wait after a node has died before beginning to attempt to restart it */
    private static final int DEAD_NODE_RETRY_INTERVAL = 60 * 1000; // one minute
    /** The amount of time the PC should wait after a supposedly running node has stopped responding to pings before killing and restarting it */
    private static final int NODE_CRASH_DETECTION_TIME = 60000;
    /** The amount of time the PC should wait after asking a node to shutdown before killing it */
    private static final int DEFAULT_STOP_TIMEOUT = 10000;

    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<String, NodeState>();

    private static final String LOG_TIMEOUT = "{0} still hasn''t started after {1}ms.  Killing it dead.";

    public void stopNode(final String nodeName, final int timeout) {
        logger.info("Stopping " + nodeName);
        final NodeState state = nodeStates.get(nodeName);
        final NodeApi api = state instanceof HasApi ? ((HasApi)state).getApi() : null;
        final Process process = state instanceof HasProcess ? ((HasProcess)state).getProcess() : null;

        try {
            if (api != null) {
                api.shutdown();
            } else {
                logger.warning(nodeName + " is supposed to be shut down, we don't have an API handle to it");
                // TODO kill it now or wait until the timeout?
            }
        } finally {
            nodeStates.put(nodeName, new StoppingNodeState(state.node, process, api, timeout));
        }
    }

    Process spawnProcess(PCNodeConfig node) throws IOException {
        // TODO start different nodes differently?

        // TODO get the OS helper to do this
        return processBuilder.start();
    }

    public NodeStateType getNodeState(String nodeName) {
        final NodeState state = nodeStates.get(nodeName);
        return state == null ? NodeStateType.UNKNOWN : state.type;
    }

    public void startNode(PCNodeConfig node) throws IOException {
        nodeStates.put(node.getName(), new StartingNodeState(this, node));
    }

    public List<SoftwareVersion> getAvailableNodeVersions() {
        return Collections.emptyList(); // TODO
    }

    private static abstract class NodeState {
        protected final PCNodeConfig node;
        protected final NodeStateType type;
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
            super(node, NodeStateType.RUNNING);
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
            super(node, NodeStateType.STOPPING);
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
            super(node, NodeStateType.STOPPED);
        }
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");

        howDoIStartedNode();

        loop();
    }

    void loop() {
        final Collection<NodeConfig> nodeConfigs = configService.getHost().getNodes().values();
        if (nodeConfigs.isEmpty()) return;

        // TODO when do we notice if a node has been deleted or disabled?

        for (NodeConfig _node : nodeConfigs) {
            final PCNodeConfig node = (PCNodeConfig)_node;
            final NodeState state = nodeStates.get(node.getName());
            final NodeStateType stateType = state == null ? NodeStateType.UNKNOWN : state.type;
            switch (stateType) {
                case UNKNOWN:
                    handleUnknownState(node);
                    break;
                case STARTING:
                    handleStartingState(node, (StartingNodeState)state);
                    break;
                case WONT_START:
                    // TODO
                    break;
                case RUNNING:
                    handleRunningState(node, (RunningNodeState)state);
                    break;
                case CRASHED:
                    // TODO attempt to restart
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
                nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.CRASHED));
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

                nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.CRASHED));
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

            nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.WONT_START));
        } else if (status instanceof StartingNodeState.Died) {
            final StartingNodeState.Died died = (StartingNodeState.Died)status;
            logger.warning(node.getName() + " crashed on startup with exit code " + died.exitValue);

            logger.warning(node.getName() + " crashed on startup; copying its output:");
            spew("STDERR", died.stderr);
            spew("STDOUT", died.stdout);

            nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.WONT_START));
        } else {
            logger.warning("Unexpected StartStatus: " + status);
            nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.WONT_START));
        }
    }

    private void handleUnknownState(PCNodeConfig node) {
        final NodeApi api = getNodeApi(node);
        try {
            api.ping();
            if (!node.isEnabled()) {
                logger.info("Stopping disabled node " + node.getName());
                api.shutdown();
                nodeStates.put(node.getName(), new StoppingNodeState(node, null, api, DEFAULT_STOP_TIMEOUT));
            } else {
                logger.info(node.getName() + " is already running");
                nodeStates.put(node.getName(), new RunningNodeState(node, null, api));
            }
        } catch (Exception e) {
            if (e instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException)e;
                if (NodeApi.NODE_NOT_CONFIGURED_FOR_PC.equals(sfe.getFault().getFaultString())) {
                    logger.warning(node.getName() + " is already running but has not been configured for use with the PC; will try again later");
                    nodeStates.put(node.getName(), new SimpleNodeState(node, NodeStateType.WONT_START));
                    return;
                }
            }

            // TODO what about other kinds of node-is-still-running? We want to avoid "address already in use".

            logger.log(Level.FINE, node.getName() + " isn't running", e);
            try {
                StartingNodeState startingState = new StartingNodeState(this, node);
                nodeStates.put(node.getName(), startingState);
            } catch (IOException e1) {
                logger.log(Level.WARNING, "Unable to start " + node + "; will retry", e);
            }
        }
    }

    private void howDoIStartedNode() {
        final HostConfig.OSType osType = configService.getHost().getOsType();
        final String[] cmds;
        final File ssgPwd;
        switch(osType) {
            case RHEL:
                ssgPwd = new File("build"); // TODO get the node installation directory (preferably not hardcoded)
                try {
                    cmds = new String[] {
                            "/ssg/jdk/bin/java",
                            "-Dcom.l7tech.server.home=\"" + ssgPwd.getCanonicalPath() + "\"",
                            "-Dcom.l7tech.server.processControllerPresent=true",
                            "-jar", "Gateway.jar", 
                    };
                } catch (IOException e) {
                    throw new RuntimeException(e); // If the
                }
//                cmds = new String[] { "/ssg/bin/partitionControl.sh", "run", node.getName() };
//                env = null;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        processBuilder = new ProcessBuilder(cmds);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(ssgPwd);
//        processBuilder.environment().clear();
//        processBuilder.environment().put("SSG_HOME", "/ssg");
    }

    NodeApi getNodeApi(PCNodeConfig node) {
        final JaxWsProxyFactoryBean pfb = new JaxWsProxyFactoryBean();
        pfb.setServiceClass(NodeApi.class);
        final String url = node.getProcessControllerApiUrl();
        pfb.setAddress(url == null ? "http://localhost:8080/ssg/services/processControllerNodeApi" : url);
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

    /**
     * Note that this method actually does get invoked, but for some reason the log message doesn't get flushed in time
     * to survive shutdown.
     */
    @PreDestroy
    public void stop() {
        logger.info("Stopping");
    }

}
