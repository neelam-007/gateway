/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.TransportMetadata;
import com.l7tech.message.TransportProtocol;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.logging.LogManager;

import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class SslAssertion extends ConfidentialityAssertion {
    public static final Option OPTIONAL = new Option(0, "SSL Optional");
    public static final Option REQUIRED = new Option(1, "SSL Required");
    public static final Option FORBIDDEN = new Option(2, "SSL Forbidden");

    public static class Option {
        protected int _numeric;
        protected String _name;

        Option(int numeric, String name) {
            _numeric = numeric;
            _name = name;
        }

        public String getName() {
            return _name;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Option)) return false;

            final Option option = (Option)o;

            if (_numeric != option._numeric) return false;
            if (_name != null ? !_name.equals(option._name) : option._name != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = _numeric;
            result = 29 * result + (_name != null ? _name.hashCode() : 0);
            return result;
        }

    }

    /**
     * @return the <code>List</code> containing the SSL options
     */
    public static final List options() {
        return
          Arrays.asList(
            new Option[]{
                OPTIONAL,
                REQUIRED,
                FORBIDDEN
            }
          );
    }

    /**
     * Constructs an SslAssertion with option = REQUIRED.
     */
    public SslAssertion() {
        this(REQUIRED);
    }

    /**
     * Constructs an SslAssertion with a specific option.
     * @param option
     */
    public SslAssertion(Option option) {
        _option = option;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        TransportMetadata tm = request.getTransportMetadata();
        boolean ssl = (tm.getProtocol() == TransportProtocol.HTTPS);
        AssertionStatus status;

        String message;
        Level level;
        if (_option == REQUIRED) {
            if (ssl) {
                status = AssertionStatus.NONE;
                message = "SSL required and present";
                level = Level.FINE;
            } else {
                status = AssertionStatus.FALSIFIED;
                message = "SSL required but not present";
                level = Level.INFO;
            }
        } else if (_option == FORBIDDEN) {
            if (ssl) {
                status = AssertionStatus.FALSIFIED;
                message = "SSL forbidden but present";
                level = Level.INFO;
            } else {
                status = AssertionStatus.NONE;
                message = "SSL forbidden and not present";
                level = Level.FINE;
            }
        } else {
            level = Level.FINE;
            status = AssertionStatus.NONE;
            message = ssl ? "SSL optional and present" : "SSL optional and not present";
        }

        _log.log(level, message);

        if (status == AssertionStatus.FALSIFIED) response.setPolicyViolated(true);

        return status;
    }

    public void setOption(Option option) {
        _option = option;
    }

    public Option getOption() {
        return _option;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        request.setSslRequired(true);
        return AssertionStatus.NONE;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
    protected Option _option = REQUIRED;

    protected transient Logger _log = LogManager.getInstance().getSystemLogger();
}
