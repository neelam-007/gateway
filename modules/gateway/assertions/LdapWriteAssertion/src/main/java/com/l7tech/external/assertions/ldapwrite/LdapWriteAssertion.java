package com.l7tech.external.assertions.ldapwrite;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.*;

import java.io.Serializable;
import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * The LdapWriteAssertion
 * Create Mar, 2017
 */
public class LdapWriteAssertion extends Assertion implements UsesEntities, UsesVariables, SetsVariables, Serializable {

    private static final String BASE_NAME = "Write LDAP:";
    private static final String DEFAULT_VARIABLE_PREFIX = "ldapWrite";

    private Goid ldapProviderId = Goid.DEFAULT_GOID;
    private String dn;
    private LdapChangetypeEnum changetype;
    private String variablePrefix = DEFAULT_VARIABLE_PREFIX;
    private List<LdifAttribute> attributeList = new ArrayList<>();

    //
    // Metadata
    //
    private static final String META_INITIALIZED = LdapWriteAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {

        final DefaultAssertionMetadata meta = super.defaultMeta();

        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        // Temporary for tactical release.  Remove this section of code when moved to trunk.
        final Map<String, String[]> props = new HashMap<>();
        props.put(LdapWriteConfig.LDAP_IDENTITY_PROVIDER_LIST_WITH_WRITE_ACCESS, new String[]{"List of LDAP Identity Providers which have write access. " +
                "Format: [{\"idprovider\":\"<id>\",\"writebase\":\"<writebase>\"},...]"});

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
        meta.put(SHORT_NAME, "Write LDAP");
        meta.put(DESCRIPTION, "Modify attributes and entries on a LDAP server.");
        meta.put(PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Properties16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(SERVER_ASSERTION_CLASSNAME, "com.l7tech.external.assertions.ldapwrite.server.ServerLdapWriteAssertion");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ldapwrite.console.LdapWritePropertiesDialog");
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.ldapwrite.LdapWriteAssertionModuleLoadListener");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        final Collection<TypeMapping> othermappings = new ArrayList<>();
        othermappings.add(new Java5EnumTypeMapping(LdapChangetypeEnum.class, "changetype"));
        othermappings.add(new BeanTypeMapping(LdifAttribute.class, "attributeType"));
        othermappings.add(new CollectionTypeMapping(List.class, LdifAttribute.class, ArrayList.class, "attributeList"));
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(othermappings));

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[]{new EntityHeader(ldapProviderId, EntityType.ID_PROVIDER_CONFIG, null, null)};
    }

    @Override
    public void replaceEntity(final EntityHeader oldEntityHeader, final EntityHeader newEntityHeader) {

        if (EntityType.ID_PROVIDER_CONFIG.equals(oldEntityHeader.getType()) &&
                ldapProviderId.equals(oldEntityHeader.getGoid()) &&
                EntityType.ID_PROVIDER_CONFIG.equals(newEntityHeader.getType())) {
            ldapProviderId = newEntityHeader.getGoid();
        }
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {

        final StringBuilder sb = new StringBuilder();
        for (LdifAttribute attributeValuePair : attributeList) {
            sb.append(attributeValuePair.getValue());
        }
        return Syntax.getReferencedNames(dn, sb.toString());
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        final List<VariableMetadata> variableMetadatas = new ArrayList<>();
        variableMetadatas.add(new VariableMetadata(getVariablePrefix() + LdapWriteConfig.VARIABLE_OUTPUT_SUFFIX_ERROR_MSG, true, false, null, false));
        return variableMetadatas.toArray(new VariableMetadata[variableMetadatas.size()]);
    }

    @Override
    public LdapWriteAssertion clone()  {

        final LdapWriteAssertion clone = (LdapWriteAssertion) super.clone();

        clone.setLdapProviderId(this.getLdapProviderId().clone());
        clone.setDn(this.getDn());
        clone.setChangetype(this.getChangetype());
        final List<LdifAttribute> newLdifAttributeList = new ArrayList<>();
        for (LdifAttribute ldifAttribute : this.getAttributeList()) {
            newLdifAttributeList.add(ldifAttribute);
        }
        clone.setAttributeList(newLdifAttributeList);
        clone.setVariablePrefix(this.getVariablePrefix());

        return clone;
    }

    public Goid getLdapProviderId() {
        return ldapProviderId;
    }

    public void setLdapProviderId(Goid ldapProviderId) {
        this.ldapProviderId = ldapProviderId;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public LdapChangetypeEnum getChangetype() {
        return changetype;
    }

    public void setChangetype(LdapChangetypeEnum changetype) {
        this.changetype = changetype;
    }

    public List<LdifAttribute> getAttributeList() {
        return attributeList;
    }

    public void setAttributeList(List<LdifAttribute> attributeList) {
        this.attributeList = attributeList;
    }


    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<LdapWriteAssertion>() {

        @Override
        public String getAssertionName(final LdapWriteAssertion assertion, final boolean decorate) {
            if (!decorate) {
                return BASE_NAME;
            }

            final StringBuilder builder = new StringBuilder(BASE_NAME);

            if (assertion.getChangetype() == null) {
                builder.append(" Unknown");
            } else {
                builder.append(" ").append(assertion.getChangetype());
            }

            return builder.toString();
        }
    };

    public void setVariablePrefix(String variablePrefix) {

        this.variablePrefix = variablePrefix;

    }

    public String getVariablePrefix() {

        return this.variablePrefix;
    }

}
