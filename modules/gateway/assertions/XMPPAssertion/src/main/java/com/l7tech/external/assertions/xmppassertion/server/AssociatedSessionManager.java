package com.l7tech.external.assertions.xmppassertion.server;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class AssociatedSessionManager {
    private static final Logger logger = Logger.getLogger(AssociatedSessionManager.class.getName());

    private HashMap<Long, Long> clientToServerMap = new HashMap<Long, Long>();
    private HashMap<Long, Long> serverToClientMap = new HashMap<Long, Long>();

    public AssociatedSessionManager() {
    }

    public void associateSessions(Long clientSessionId, Long serverSessionId) {
        clientToServerMap.put(clientSessionId, serverSessionId);
        serverToClientMap.put(serverSessionId, clientSessionId);
    }

    public void removeClientSession(Long clientSessionId) {
        Long serverSessionId = clientToServerMap.remove(clientSessionId);
        if(serverSessionId != null && clientSessionId.equals(serverToClientMap.get(serverSessionId))) {
            serverToClientMap.remove(serverSessionId);
        }
    }

    public void removeServerSession(Long serverSessionId) {
        Long clientSessionId = serverToClientMap.remove(serverSessionId);
        if(clientSessionId != null && serverSessionId.equals(clientToServerMap.get(clientSessionId))) {
            clientToServerMap.remove(clientSessionId);
        }
    }

    public Long getClientSessionFromServerSession(Long serverSessionId) {
        return serverToClientMap.get(serverSessionId);
    }

    public Long getServerSessionFromClientSession(Long clientSessionId) {
        return clientToServerMap.get(clientSessionId);
    }
}
