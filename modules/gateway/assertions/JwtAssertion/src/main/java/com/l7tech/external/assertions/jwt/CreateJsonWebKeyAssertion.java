package com.l7tech.external.assertions.jwt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntryId;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.BeanTypeMapping;
import com.l7tech.policy.wsp.CollectionTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class CreateJsonWebKeyAssertion extends Assertion implements UsesVariables, SetsVariables, UsesPrivateKeys {

    private String targetVariable;

    private List<JwkKeyInfo> keys;

    public String getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(String targetVariable) {
        this.targetVariable = targetVariable;
    }

    public List<JwkKeyInfo> getKeys() {
        if (keys == null || keys.isEmpty()) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(keys);
    }

    public void setKeys(List<JwkKeyInfo> keys) {
        if (keys == null || keys.isEmpty()) {
            this.keys = ImmutableList.of();
        } else {
            this.keys = ImmutableList.copyOf(keys);
        }
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = EncodeJsonWebTokenAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Create Json Web Key");
        meta.put(AssertionMetadata.DESCRIPTION, "Creates a Json Web Key or Json Web Key Set.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.jwt.console.CreateJsonWebKeyPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Create Json Web Key Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set nice, informative policy node name for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/jsonwebtoken/console/resources/openidConnect.gif");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        Collection<TypeMapping> othermappings = new ArrayList<TypeMapping>();
        othermappings.add(new CollectionTypeMapping(List.class, JwkKeyInfo.class, ArrayList.class, "keys"));
        othermappings.add(new BeanTypeMapping(JwkKeyInfo.class, "key"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(getTargetVariable(), false, false, null, false)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        if (keys == null || keys.isEmpty()) {
            return new String[0];
        }
        final List<String> names = Lists.newArrayList();
        for (JwkKeyInfo i : keys) {
            names.add(i.getKeyId());
        }
        return Syntax.getReferencedNames(names.toArray(new String[names.size()]));
    }

    @Override
    public CreateJsonWebKeyAssertion clone() {
        final CreateJsonWebKeyAssertion clone = (CreateJsonWebKeyAssertion) super.clone();
        clone.setTargetVariable(targetVariable);
        clone.setKeys(keys);
        return clone;
    }

    @Override
    public SsgKeyHeader[] getPrivateKeysUsed() {
        List<SsgKeyHeader> headers = Lists.newArrayList();
        for (JwkKeyInfo k : keys) {
            headers.add(new SsgKeyHeader(k.getSourceKeyGoid() + ":" + k.getSourceKeyAlias(), k.getSourceKeyGoid(), k.getSourceKeyAlias(), k.getSourceKeyAlias()));
        }
        return headers.toArray(new SsgKeyHeader[headers.size()]);
    }

    @Override
    public void replacePrivateKeyUsed(@NotNull SsgKeyHeader oldEntityHeader, @NotNull SsgKeyHeader newEntityHeader) {
        for (JwkKeyInfo k : keys) {
            if (Goid.equals(oldEntityHeader.getKeystoreId(), k.getSourceKeyGoid()) && k.getSourceKeyAlias().equals(oldEntityHeader.getAlias())) {
                if (newEntityHeader instanceof SsgKeyHeader) {
                    k.setSourceKeyAlias(newEntityHeader.getAlias());
                    k.setSourceKeyGoid(newEntityHeader.getKeystoreId());
                } else {
                    SsgKeyEntryId keyId = new SsgKeyEntryId(newEntityHeader.getStrId());
                    k.setSourceKeyAlias(keyId.getAlias());
                    k.setSourceKeyGoid(keyId.getKeystoreId());
                }
            }
        }

    }
}

