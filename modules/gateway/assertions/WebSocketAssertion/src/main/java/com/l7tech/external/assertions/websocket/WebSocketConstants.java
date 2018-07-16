package com.l7tech.external.assertions.websocket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketConstants {

    public enum ConnectionType {
        Inbound,
        Outbound
    }

    protected static final Logger logger = Logger.getLogger(WebSocketConstants.class.getName());
    private static ConcurrentHashMap<String, Object> clusterprops = new ConcurrentHashMap<>();

    //General
    public static final String ACTION_MENU_NAME = "Manage WebSocket Connections";
    public static final String ACTION_MENU_ICON_PATH = "com/l7tech/console/resources/Properties16.gif";
    public static final String MANAGE_CONNECTIONS_TITLE = "Manage WebSocket Connections";
    public static final String MANAGE_CONNECTION_TITLE = "WebSocket Connection Properties";

    //Connections
    //keys
    public static final String BUFFER_SIZE_KEY = "websocket.client.buffer.size";
    public static final String MAX_BINARY_MSG_SIZE_KEY = "websocket.max.binary.msg.size";
    public static final String MAX_TEXT_MSG_SIZE_KEY = "websocket.max.text.msg.size";
    public static final String MAX_INBOUND_IDLE_TIME_MS_KEY = "websocket.inbound.connection.idle";
    public static final String MAX_OUTBOUND_IDLE_TIME_MS_KEY = "websocket.outbound.connection.idle";
    public static final String CONNECT_TIMEOUT_KEY = "websocket.outbound.client.connection.timeout";
    public static final String MAX_INBOUND_CONNECTIONS_KEY = "websocket.max.inbound.connections";
    public static final String MAX_OUTBOUND_THREADS_KEY = "websocket.max.outbound.threads";
    public static final String MIN_OUTBOUND_THREADS_KEY = "websocket.min.outbound.threads";
    public static final String MAX_INBOUND_THREADS_KEY = "websocket.max.inbound.threads";
    public static final String MIN_INBOUND_THREADS_KEY = "websocket.min.inbound.threads";
    public static final String ACCEPT_QUEUE_SIZE_KEY = "websocket.accept.queue.size";
    public static final String OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL_KEY = "websocket.outbound.only.connection.reconnect.interval";
    public static final String INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY = "websocket.inbound.copy.upgrade.request.subprotocol.header";

    //Defaults
    public static final int BUFFER_SIZE = 4096;
    public static final int MAX_BINARY_MSG_SIZE = 1048576;
    public static final int MAX_TEXT_MSG_SIZE = 1048576;
    public static final int MAX_INBOUND_IDLE_TIME_MS = 60000;
    public static final int MAX_OUTBOUND_IDLE_TIME_MS = 60000;
    public static final int CONNECT_TIMEOUT = 20; // seconds.
    public static final int MAX_INBOUND_CONNECTIONS = 4096;
    public static final int MAX_OUTBOUND_THREADS = 25;
    public static final int MIN_OUTBOUND_THREADS = 10;
    public static final int MAX_INBOUND_THREADS = 25;
    public static final int MIN_INBOUND_THREADS = 10;
    public static final int ACCEPT_QUEUE_SIZE = 100;
    public static final int OUTBOUND_ONLY_CONNECTION_RECONNECT_INTERVAL = 300000; // milliseconds
    public static final boolean INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER = true;

    public static final int MIN_LISTEN_PORT = 1025;
    public static final int MAX_LISTEN_PORT = 65535;
    public static final String SECURITY_WEBSOCKET_PROTOCOL_KEY = "Sec-WebSocket-Protocol";

    //GUI
    public static final String DELETE_CONN_CHALLENGE = "Delete Connection ";
    public static final String DELETE_CONN_CONFIRM = "Confirm Delete Connection";
    public static final String DELETE_CONN_ERROR = "Unable to Delete Connection";
    public static final String CONN_VALIDATION = "Invalid Connection: The Name and Listen Port must be unique";
    public static final String CONN_SAVE_ERROR = "Unable to save connection: ";
    public static final String CONN_LOAD_ERROR = "Unable to load connections: ";

    public static final String AUTHENTICATION_CONTEXT_REQ_ATTRIB = "AuthenticationContextRequestAttributes";
    public static final String OUTBOUND_URL = "OutboundUrl";

    public static final String[] DEFAULT_TLS_PROTOCOL_LIST = {"TLSv1","TLSv1.1","TLSv1.2"};
    // DE248803- WebSocket Assertion default TLS protocol list must be modfied

    public static void setClusterProperty(String key, int value) {
        clusterprops.put(key, value);
    }

    public static void setClusterProperty(String key, boolean value) {
        clusterprops.put(key, value);
    }

    public static int getClusterProperty(String key) {
        Object prop = clusterprops.get(key);
        if (prop instanceof Integer) {
            return (Integer) prop;
        }
        logger.log(Level.FINE, "Unable to get Cluster Property for key: "+ key);
        throw new IllegalArgumentException("Cluster Property for key: "+ key + " is not an Integer");

    }

    public static boolean getBooleanClusterProperty(String key) {
        Object prop = clusterprops.get(key);
        if (prop instanceof Boolean) {
            return (Boolean) prop;
        }
        logger.log(Level.FINE, "Unable to get Cluster Property for key: "+ key);
        throw new IllegalArgumentException("Cluster Property for key: "+ key +" is not a Boolean");
    }
}
