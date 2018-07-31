package com.l7tech.external.assertions.siteminder;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.search.Dependency;

import java.util.*;

/**
 *
 */
public class SiteMinderChangePasswordAssertion extends MessageTargetableAssertion implements UsesEntities, SiteMinderAgentReference {
    //
    // Metadata
    //
    private static final String META_INITIALIZED = SiteMinderChangePasswordAssertion.class.getName() + ".metadataInitialized";
    private static final String BASE_NAME = "Change CA Single Sign-On User Password";
    private static final int MAX_DISPLAY_LENGTH = 120;
    public static final String REASON_CODE_CONTEXT_VAR_NAME = "reasonCode";

    private Goid agentGoid;
    private String agentId;
    private String domOid;
    private String username;
    private String oldPassword;
    private String newPassword;

    public SiteMinderChangePasswordAssertion() {
        super();
    }

    @Dependency(type = Dependency.DependencyType.SITEMINDER_CONFIGURATION, methodReturnType = Dependency.MethodReturnType.GOID)
    @Override
    public Goid getAgentGoid() {
        return agentGoid;
    }

    @Override
    public void setAgentGoid(Goid agentID) {
        this.agentGoid = agentID;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    @Override
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDomOid() {
        return domOid;
    }

    public void setDomOid(String domOid) {
        this.domOid = domOid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    @Override
    public EntityHeader[] getEntitiesUsed() {
        if(agentGoid != null) {
            return new EntityHeader[] { new EntityHeader(Goid.toString(agentGoid), EntityType.SITEMINDER_CONFIGURATION, null, null) };
        }
        return new EntityHeader[0];
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.SITEMINDER_CONFIGURATION) &&
                oldEntityHeader.getGoid().equals(agentGoid) &&
                newEntityHeader.getType().equals(EntityType.SITEMINDER_CONFIGURATION)) {
            agentGoid = newEntityHeader.getGoid();
        }
    }

    @Override
    public VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions(domOid, username, oldPassword, newPassword);
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
                new VariableMetadata( REASON_CODE_CONTEXT_VAR_NAME, false, false, REASON_CODE_CONTEXT_VAR_NAME, false, DataType.INTEGER )
        );
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();

        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.LONG_NAME, BASE_NAME);

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.siteminder.console.SiteMinderChangePasswordPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, BASE_NAME + " Properties");

        // Add to palette folder
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/user16.png");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/user16.png");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:SiteMinder" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SiteMinderChangePasswordAssertion>(){
        @Override
        public String getAssertionName( final SiteMinderChangePasswordAssertion assertion, final boolean decorate) {
            if(!decorate) return BASE_NAME;

            StringBuilder name = new StringBuilder(assertion.getTargetName() + ": " + BASE_NAME + ": agent [");
            name.append(assertion.getAgentId());
            name.append(']');
            if(name.length() > MAX_DISPLAY_LENGTH) {
                name = name.replace(MAX_DISPLAY_LENGTH - 1, name.length() - 1, "...");
            }
            return name.toString();
        }
    };
}
