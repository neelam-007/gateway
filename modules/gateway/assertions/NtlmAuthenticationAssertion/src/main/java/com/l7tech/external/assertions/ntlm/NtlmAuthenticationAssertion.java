package com.l7tech.external.assertions.ntlm;

import com.l7tech.external.assertions.ntlm.console.NtlmAuthenticationPropertiesDialog;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * 
 */
public class NtlmAuthenticationAssertion extends HttpCredentialSourceAssertion implements UsesEntities, UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(NtlmAuthenticationAssertion.class.getName());

    private long maxConnectionDuration;

    private long maxConnectionIdleTime;

    private String variablePrefix;

    private long ldapProviderOid = -1;

    private String ldapProviderName = null;

    //
    // Metadata
    //
    private static final String META_INITIALIZED = NtlmAuthenticationAssertion.class.getName() + ".metadataInitialized";

    public static final long DEFAULT_MAX_CONNECTION_DURATION = 0L;
    public static final long DEFAULT_MAX_IDLE_TIMEOUT = 0L;
    public static final String DEFAULT_PREFIX = "ntlm";
    public static final String USER_LOGIN_NAME = "sAMAccountName";
    public static final String SID = "sid";
    public static final String ACCOUNT_FLAGS = "userAccountFlags";
    public static final String DOMAIN_NAME = "logonDomainName";
    public static final String SESSION_KEY = "session.key";
    public static final String ACCOUNT_FULL_NAME = "fullName";
    public static final String ACCOUNT_HOME_DIR ="homeDirectory";
    public static final String ACCOUNT_DIR_DRIVE = "homeDirectoryDrive";
    public static final String ACCOUNT_SIDS = "sidGroups";
    public static final String NTLM = "NTLM";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        //Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        //meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Require NTLM Authentication Credentials");
        meta.put(AssertionMetadata.LONG_NAME, "Requester must provide credentials using NTLM authentication method");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/authentication.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:NtlmAuthentication" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "assertion:NtlmAuthentication");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.ntlm.console.NtlmAuthenticationPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "NTLM Authentication Properties");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);


        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public long getMaxConnectionIdleTime() {
        return maxConnectionIdleTime;
    }

    public void setMaxConnectionIdleTime(long maxConnectionIdleTime) {
        this.maxConnectionIdleTime = maxConnectionIdleTime;
    }

    public long getMaxConnectionDuration() {
        return maxConnectionDuration;
    }

    public void setMaxConnectionDuration(long maxConnectionDuration) {
        this.maxConnectionDuration = maxConnectionDuration;
    }

    public String getVariablePrefix() {
        return StringUtils.isBlank(variablePrefix)?DEFAULT_PREFIX:variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public long getLdapProviderOid() {
        return ldapProviderOid;
    }

    public void setLdapProviderOid(long ldapProviderOid) {
        this.ldapProviderOid = ldapProviderOid;
    }

    public String getLdapProviderName() {
        return ldapProviderName;
    }

    public void setLdapProviderName(String ldapProviderName) {
        this.ldapProviderName = ldapProviderName;
    }


    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return  Syntax.getReferencedNames(getVariablePrefix());
    }

    @Override
    public VariableMetadata[] getVariablesSet() {

       return new VariableMetadata[] { new VariableMetadata(getVariablePrefix() + "." + USER_LOGIN_NAME, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + SID, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + SESSION_KEY, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + ACCOUNT_FLAGS, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + DOMAIN_NAME, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + ACCOUNT_FULL_NAME, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + ACCOUNT_HOME_DIR, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + ACCOUNT_DIR_DRIVE, false, false, null, false, DataType.STRING),
                                       new VariableMetadata(getVariablePrefix() + "." + ACCOUNT_SIDS, false, true, null, false, DataType.STRING)
       };
    }


    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        return new EntityHeader[] { new EntityHeader(Long.toString(ldapProviderOid), EntityType.ID_PROVIDER_CONFIG, ldapProviderName, null) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if(oldEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG) && oldEntityHeader.getOid() == ldapProviderOid &&
                newEntityHeader.getType().equals(EntityType.ID_PROVIDER_CONFIG))
        {
            ldapProviderOid = newEntityHeader.getOid();
            ldapProviderName = newEntityHeader.getName();
        }
    }
}
