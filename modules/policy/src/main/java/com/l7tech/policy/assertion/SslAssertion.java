/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspEnumTypeMapping;
import com.l7tech.util.EnumTranslator;
import com.l7tech.util.Functions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author alex
 * @version $Revision$
 */
@ProcessesRequest
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Option)) return false;

            final Option option = (Option)o;

            if (_numeric != option._numeric) return false;
            if (_name != null ? !_name.equals(option._name) : option._name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = _numeric;
            result = 29 * result + (_name != null ? _name.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return _keyName;
        }

        // WspEnumTypeMapping finds this method by reflection
        public static EnumTranslator getEnumTranslator() {
            return new EnumTranslator() {
                @Override
                public Object stringToObject(String s) throws IllegalArgumentException {
                    return SslAssertion.Option.forKeyName(s);
                }

                @Override
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
    @Override
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

    @Override
    public String toString() {
        return super.toString() + " clientCert=" + isRequireClientAuthentication() + " option=" + getOption();
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SslAssertion>(){
        @Override
        public String getAssertionName( final SslAssertion assertion, final boolean decorate) {
            final String assertionName = " SSL or TLS Transport";
            final String sslOrTls = "SSL or TLS Transport";
            final String prefix;
            if (SslAssertion.FORBIDDEN.equals(assertion.getOption())){
                prefix = "Forbid";
            }else if (SslAssertion.OPTIONAL.equals(assertion.getOption())){
                prefix = "Optional";
            }else{
                prefix = "Require";
            }

            final String retStr;
            if (assertion.isRequireClientAuthentication()) {
                retStr = "Require " + sslOrTls + " with Client Certificate Authentication";
            }else{
                retStr = prefix +" "+sslOrTls;
            }

            return (decorate)? retStr: assertionName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = isRequireClientAuthentication() ? META_CLIENT : META_NOCLIENT;
        meta.put(AssertionMetadata.VARIANT_PROTOTYPES, new Assertion[] { META_CLIENT.getPrototype(), META_NOCLIENT.getPrototype() });
        return meta;
    }

    private static DefaultAssertionMetadata makeMeta(boolean requireClientCert) {
        DefaultAssertionMetadata meta = new DefaultAssertionMetadata(new SslAssertion(requireClientCert));

        meta.put(AssertionMetadata.SHORT_NAME, requireClientCert ? "Require SSL or TLS Transport with Client Authentication" : "Require SSL or TLS Transport");
        meta.put(AssertionMetadata.DESCRIPTION, "The incoming request must either use, optionally use, or is forbidden to use SSL/TLS transport. Client certificate authentication is optional if SSL / TLS is required.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/ssl.gif");

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { requireClientCert ? "accessControl" : "transportLayerSecurity" });
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new Functions.Unary<SslAssertion, SslAssertion>() {
            @Override
            public SslAssertion call(SslAssertion sslAssertion) {
                return new SslAssertion(sslAssertion.isRequireClientAuthentication());
            }
        });

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new WspEnumTypeMapping(Option.class, "optionValue")
        )));

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.SslPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SSL or TLS Transport Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");

        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/ssl.gif");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);

        return meta;
    }

    static final DefaultAssertionMetadata META_NOCLIENT = makeMeta(false);
    static final DefaultAssertionMetadata META_CLIENT = makeMeta(true);

    protected Set _cipherSuites = Collections.EMPTY_SET;
    protected Option _option = REQUIRED;

    private boolean requireClientAuthentication = false;
}
