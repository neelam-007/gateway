package com.l7tech.gateway.common.spring.remoting.http;

/**
 * Support class for holding session info for a thread.
 */
class SessionSupport {


    //- PACKAGE

    SessionInfo getSessionInfo() {
        return infoHolder.getSessionInfo();
    }

    static final class SessionInfo {
        String host;
        int port;
        String sessionId;
    }

    //- PRIVATE

    private SessionInfoHolder infoHolder = new ThreadLocalSessionInfoHolder();

    private interface SessionInfoHolder {
        SessionInfo getSessionInfo();
    }

    private static class ThreadLocalSessionInfoHolder implements SessionInfoHolder {
        private final ThreadLocal<SessionInfo> threadSessionInfo = new ThreadLocal<SessionInfo>(){
            @Override
            protected SessionInfo initialValue() {
                return new SessionInfo();
            }
        };

        public SessionInfo getSessionInfo() {
            return threadSessionInfo.get();
        }
    }

}
