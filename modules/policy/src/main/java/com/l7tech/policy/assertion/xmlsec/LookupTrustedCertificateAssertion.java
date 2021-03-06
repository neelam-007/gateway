package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.Functions;

import java.util.Arrays;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.VariableUseSupport.expressions;
import static com.l7tech.util.Option.optional;

/**
 * Assertion for certificate lookup.
 */
public class LookupTrustedCertificateAssertion extends Assertion implements SetsVariables, UsesVariables {

    private static final String META_INITIALIZED = LookupTrustedCertificateAssertion.class.getName() + ".metadataInitialized";

    private String trustedCertificateName;
    private String certSubjectKeyIdentifier;
    private String certThumbprintSha1;
    private String certIssuerDn;
    private String certSerialNumber;
    private String certSubjectDn;
    private boolean allowMultipleCertificates = true;//by default allow multiple certificates
    private String variableName = "certificates";
    private LookupType lookupType = LookupType.TRUSTED_CERT_NAME;

    public LookupTrustedCertificateAssertion(){
    }

    public String getTrustedCertificateName() {
        return trustedCertificateName;
    }

    public void setTrustedCertificateName( final String trustedCertificateName ) {
        this.trustedCertificateName = trustedCertificateName;
    }

    public String getCertSubjectKeyIdentifier() {
        return certSubjectKeyIdentifier;
    }

    public void setCertSubjectKeyIdentifier(String certSubjectKeyIdentifier) {
        this.certSubjectKeyIdentifier = certSubjectKeyIdentifier;
    }

    public String getCertThumbprintSha1() {
        return certThumbprintSha1;
    }

    public void setCertThumbprintSha1(String certThumbprintSha1) {
        this.certThumbprintSha1 = certThumbprintSha1;
    }

    public String getCertIssuerDn() {
        return certIssuerDn;
    }

    public void setCertIssuerDn(String certIssuerDn) {
        this.certIssuerDn = certIssuerDn;
    }

    public String getCertSerialNumber() {
        return certSerialNumber;
    }

    public void setCertSerialNumber(String certSerialNumber) {
        this.certSerialNumber = certSerialNumber;
    }

    public String getCertSubjectDn() {
        return certSubjectDn;
    }

    public void setCertSubjectDn(String certSubjectDn) {
        this.certSubjectDn = certSubjectDn;
    }

    public LookupType getLookupType() {
        return lookupType;
    }

    public void setLookupType(LookupType lookupType) {
        this.lookupType = lookupType;
    }

    public boolean isAllowMultipleCertificates() {
        return allowMultipleCertificates;
    }

    public void setAllowMultipleCertificates( final boolean allowMultipleCertificates ) {
        this.allowMultipleCertificates = allowMultipleCertificates;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName( final String variableName ) {
        this.variableName = variableName;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{ new VariableMetadata( variableName, false, true, variableName, true, DataType.CERTIFICATE ) };
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return expressions( trustedCertificateName, certSubjectKeyIdentifier, certSubjectDn, certThumbprintSha1, certIssuerDn, certSerialNumber ).asArray();
    }

    @Override
    public AssertionMetadata meta() {
        final DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Look Up Certificate");
        meta.put(DESCRIPTION, "Look up certificates for later use in policy.  Can look up trusted certificates by cert name, or any cert by SKI, ThumbprintSHA1, Issuer/Serial, or DN.");
        meta.put(PROPERTIES_ACTION_NAME, "Certificate Lookup Properties");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.LookupTrustedCertificateAssertionPropertiesDialog");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(LookupType.class, "security")
        )));

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<LookupTrustedCertificateAssertion>() {
            @Override
            public String getAssertionName(final LookupTrustedCertificateAssertion assertion, final boolean decorate) {
                final String displayName = meta.getString(AssertionMetadata.SHORT_NAME);
                if (!decorate)
                    return displayName;

                StringBuilder sb = new StringBuilder("Look Up ");
                final LookupType lookupType = optional(assertion.getLookupType()).orSome(LookupType.TRUSTED_CERT_NAME);
                sb.append(lookupType.getWhat());
                sb.append(" by ");
                sb.append(lookupType.getHow());
                sb.append(": ");
                sb.append(lookupType.getParamExtractor().call(assertion));

                return AssertionUtils.decorateName(assertion, sb);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public static enum LookupType {
        TRUSTED_CERT_NAME("Trusted Certificate", "Name", forField("trustedCertificateName")),
        CERT_SKI("Certificate", "Subject Key ID", forField("certSubjectKeyIdentifier")),
        CERT_THUMBPRINT_SHA1("Certificate", "ThumbprintSHA1", forField("certThumbprintSha1")),
        CERT_ISSUER_SERIAL("Certificate", "Issuer DN/Serial Number", forIssuerSerialFields()),
        CERT_SUBJECT_DN("Certificate", "Subject DN", forField("certSubjectDn")),
        ;

        private LookupType(String what, String how, Functions.Unary<String, LookupTrustedCertificateAssertion> paramExtractor) {
            this.paramExtractor = paramExtractor;
            this.how = how;
            this.what = what;
        }

        private final String what;
        private final String how;
        private final Functions.Unary<String, LookupTrustedCertificateAssertion> paramExtractor;

        public String getWhat() {
            return what;
        }

        public String getHow() {
            return how;
        }

        public Functions.Unary<String, LookupTrustedCertificateAssertion> getParamExtractor() {
            return paramExtractor;
        }

        private static Functions.Unary<String, LookupTrustedCertificateAssertion> forField(String field) {
            return Functions.propertyTransform(LookupTrustedCertificateAssertion.class, field);
        }

        private static Functions.Unary<String, LookupTrustedCertificateAssertion> forIssuerSerialFields() {
            return new Functions.Unary<String, LookupTrustedCertificateAssertion>() {
                @Override
                public String call(LookupTrustedCertificateAssertion ass) {
                    return ass.getCertIssuerDn() + "/" + ass.getCertSerialNumber();
                }
            };
        }
    }
}
