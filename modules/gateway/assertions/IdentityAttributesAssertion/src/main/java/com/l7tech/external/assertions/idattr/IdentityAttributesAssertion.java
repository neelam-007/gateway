package com.l7tech.external.assertions.idattr;

import com.l7tech.identity.mapping.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.wsp.*;
import com.l7tech.objectmodel.UsersOrGroups;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IdentityAttributesAssertion extends Assertion implements UsesVariables, SetsVariables, UsesEntities {
    private String variablePrefix;
    private long identityProviderOid;
    private IdentityMapping[] lookupAttributes;

    public static final String DEFAULT_VAR_PREFIX = "authenticatedUser";

    public String[] getVariablesUsed() {
        return new String[0];
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public void setIdentityProviderOid(long identityProviderOid) {
        this.identityProviderOid = identityProviderOid;
    }

    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        if(identityProviderOid > 0) {
            return new EntityHeader[] {new EntityHeader(identityProviderOid, EntityType.ID_PROVIDER_CONFIG, null, null)};
        } else {
            return new EntityHeader[0];
        }
    }

    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) && oldEntityHeader.getOid() == identityProviderOid &&
                newEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG))
        {
            identityProviderOid = newEntityHeader.getOid();
        }
    }

    @Migration(dependency = false)
    public IdentityMapping[] getLookupAttributes() {
        return lookupAttributes;
    }

    public void setLookupAttributes(IdentityMapping[] lookupAttributes) {
        this.lookupAttributes = lookupAttributes;
    }

    public VariableMetadata[] getVariablesSet() {
        if (lookupAttributes == null || lookupAttributes.length == 0) return new VariableMetadata[0];
        String vp = variablePrefix;
        if (vp == null) vp = DEFAULT_VAR_PREFIX;
        List<VariableMetadata> metas = new ArrayList<VariableMetadata>();
        for (IdentityMapping im : lookupAttributes) {
            final AttributeConfig ac = im.getAttributeConfig();
            metas.add(new VariableMetadata(vp + "." + ac.getVariableName(), false, im.isMultivalued(), null, false, ac.getType()));
        }
        return metas.toArray(new VariableMetadata[0]);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = IdentityAttributesAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Identity Attributes");
        meta.put(AssertionMetadata.LONG_NAME, "Set Context Variables based on attributes of the authenticated user");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.idattr.console.IdentityAttributesAssertionDialog");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/userAttrs16.png");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/userAttrs16.png");

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new ArrayTypeMapping(new IdentityMapping[0], "identityMappingArray"),
            new AbstractClassTypeMapping(IdentityMapping.class, "identityMapping"),
            new BeanTypeMapping(LdapAttributeMapping.class, "ldapAttributeMapping"),
            new BeanTypeMapping(InternalAttributeMapping.class, "internalAttributeMapping"),
            new BeanTypeMapping(FederatedAttributeMapping.class, "federatedAttributeMapping"),
            new BeanTypeMapping(AttributeConfig.class, "attributeConfig"),
            new WspEnumTypeMapping(DataType.class, "dataType"),
            new Java5EnumTypeMapping( UsersOrGroups.class, "usersOrGroups")
        )));

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:IdentityAttributes" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
