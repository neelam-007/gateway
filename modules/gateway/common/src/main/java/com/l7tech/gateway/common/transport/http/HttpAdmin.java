/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */
package com.l7tech.gateway.common.transport.http;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin class for testing methods of @{link HttpRoutingAssertion}
 *
 * @author rraquepo
 */
@Administrative
@Secured
public interface HttpAdmin {

    @Transactional(readOnly = true)
    String testConnection(String[] serverUrls, String testMessage, HttpRoutingAssertion assertion) throws HttpAdminException;

    public class HttpAdminException extends Exception {
        private String _sessionLog;
        private String _response;

        public HttpAdminException() {
            super();
        }

        public HttpAdminException(String message, String _sessionLog, String _response) {
            super(message);
            this._sessionLog = _sessionLog;
            this._response = _response;
        }

        public HttpAdminException(String message, Exception e, String _sessionLog, String _response) {
            super(message, e);
            this._sessionLog = _sessionLog;
        }

        /**
         * @return a session log  or other info
         */
        public String getSessionLog() {
            return _sessionLog;
        }

        /**
         * @return a response
         */
        public String getResponse() {
            return _response;
        }

    }
}
