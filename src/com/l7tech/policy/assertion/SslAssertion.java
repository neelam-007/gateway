/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion;

import com.l7tech.common.util.EnumTranslator;
import com.l7tech.policy.wsp.WspEnumTypeMapping;
import com.l7tech.policy.wsp.TypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

        public String toString() {
            return _keyName;
        }

        // WspEnumTypeMapping finds this method by reflection
        public static EnumTranslator getEnumTranslator() {
            return new EnumTranslator() {
                public Object stringToObject(String s) throws IllegalArgumentException {
                    return SslAssertion.Option.forKeyName(s);
                }

                public String objectToString(Object o) throws ClassCastException {
                    return ((SslAssertion.Option)o).getKeyName();
                }
            };
        }
    }

    /**
     * Test whether the assertion is a credential source. The SSL Assertion
     * is a credential source if the SSL is REQUIRED and client authentication has
     * been requested
     *
     * @return true if credential source, false otherwise
     */
    public boolean isCredentialSource() {
        return requireClientAuthentication && _option.equals(REQUIRED);
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
     * Constructs an SslAssertion with option REQUIRED and the client authentication
     * specified by boolean flag.
     */
    public SslAssertion(boolean requireClientAuthentication) {
        this(REQUIRED);
        this.requireClientAuthentication = requireClientAuthentication;

    }

    /**
     * Constructs an SslAssertion with a specific option.
     *
     * @param option
     */
    public SslAssertion(Option option) {
        _option = option;
    }

    public void setOption(Option option) {
        _option = option;
        if (_option.equals(REQUIRED)) { // client auth and SSL !REQUIRED do not mix
            setRequireClientAuthentication(false);
        }
    }

    public Option getOption() {
        return _option;
    }

    /**
     * Returns whether the client authentication has been requested
     *
     * @return true if client auth has been requested, false otherwise
     */
    public boolean isRequireClientAuthentication() {
        return requireClientAuthentication;
    }

    /**
     * Set whether the client authentication has been requested. Setting this to
     * <code>true</code> automatically enables the
     *
     * @param requireClientAuthentication the boolean flag indicating whether the client authentication is requested
     */
    public void setRequireClientAuthentication(boolean requireClientAuthentication) {
        this.requireClientAuthentication = requireClientAuthentication;
        if (requireClientAuthentication) {
            _option = REQUIRED;
        }
    }

    public String toString() {
        return super.toString() + " clientCert=" + isRequireClientAuthentication() + " option=" + getOption();
    }


    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new WspEnumTypeMapping(Option.class, "optionValue")
        )));

        return meta;
    }

    protected Set _cipherSuites = Collections.EMPTY_SET;
    protected Option _option = REQUIRED;

    private boolean requireClientAuthentication = false;
}
