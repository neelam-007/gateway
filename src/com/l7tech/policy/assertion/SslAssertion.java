/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.io.Serializable;

/**
 * @author alex
 * @version $Revision$
 */
public class SslAssertion extends ConfidentialityAssertion {
    public static final Option OPTIONAL = new Option(0, "SSL Optional", "Optional");
    public static final Option REQUIRED = new Option(1, "SSL Required", "Required");
    public static final Option FORBIDDEN = new Option(2, "SSL Forbidden", "Forbidden");

    public static class Option implements Serializable {
        protected int _numeric;
        protected String _name;
        protected String _keyName;

        Option(int numeric, String name, String keyName) {
            _numeric = numeric;
            _name = name;
            _keyName = keyName;
        }

        public static Option forKeyName(String wantName) {
            if (OPTIONAL.getKeyName().equals(wantName))
                return OPTIONAL;
            else if (REQUIRED.getKeyName().equals(wantName))
                return REQUIRED;
            else if (FORBIDDEN.getKeyName().equals(wantName))
                return FORBIDDEN;
            return null;
        }

        public String getName() {
            return _name;
        }

        public String getKeyName() {
            return _keyName;
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

    public void setOption(Option option) {
        _option = option;
    }

    public Option getOption() {
        return _option;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
    protected Option _option = REQUIRED;
}
