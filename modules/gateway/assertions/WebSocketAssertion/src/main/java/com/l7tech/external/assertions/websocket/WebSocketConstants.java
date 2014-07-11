package com.l7tech.external.assertions.websocket;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketConstants {

    private static ConcurrentHashMap<String, Integer> clusterprops = new ConcurrentHashMap<String, Integer>();

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
    //Defaults
    public static final int BUFFER_SIZE = 4096;
    public static final int MAX_BINARY_MSG_SIZE = 1048576;
    public static final int MAX_TEXT_MSG_SIZE = 1048576;
    public static final int MAX_INBOUND_IDLE_TIME_MS = 60000;
    public static final int MAX_OUTBOUND_IDLE_TIME_MS = 60000;
    public static final int CONNECT_TIMEOUT = 20;
    public static final int MAX_INBOUND_CONNECTIONS = 4096;
    public static final int MAX_OUTBOUND_THREADS = 25;
    public static final int MIN_OUTBOUND_THREADS = 10;
    public static final int MAX_INBOUND_THREADS = 25;
    public static final int MIN_INBOUND_THREADS = 10;

    public static final int MIN_LISTEN_PORT = 1025;
    public static final int MAX_LISTEN_PORT = 65535;

    //GUI
    public static final String DELETE_CONN_CHALLENGE = "Delete Connection ";
    public static final String DELETE_CONN_CONFIRM = "Confirm Delete Connection";
    public static final String DELETE_CONN_ERROR = "Unable to Delete Connection";
    public static final String CONN_VALIDATION = "Invalid Connection: The Name and Listen Port must be unique";
    public static final String CONN_SAVE_ERROR = "Unable to save connection: ";
    public static final String CONN_LOAD_ERROR = "Unable to load connections: ";

    public static void setClusterProperty(String key, int value) {
        clusterprops.put(key, value);
    }

    public static int getClusterProperty(String key) {
        return clusterprops.get(key);
    }
}
