package com.l7tech.external.assertions.ahttp;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.wsp.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 *
 */
public class AsyncHttpRoutingAssertion extends HttpRoutingAssertion implements UsesVariables, UsesEntities, PolicyReference {
    private String policyGuid;
    private transient Policy fragmentPolicy;

    /**
     * GUID of policy to invoke to deliver the async response.
     *
     * @return the policy GUID, or null if not set.
     */
    public String getPolicyGuid() {
        return policyGuid;
    }

    public void setPolicyGuid(String policyGuid) {
        this.policyGuid = policyGuid;
    }

    @Override
    public void setProtectedServiceUrl(String protectedServiceUrl) {
        super.setProtectedServiceUrl(protectedServiceUrl);
    }

    @Override
    public String getProtectedServiceUrl() {
        return super.getProtectedServiceUrl();
    }

    @Override
    public void setHttpMethod(HttpMethod httpMethod) {
        super.setHttpMethod(httpMethod);
    }

    @Override
    public HttpMethod getHttpMethod() {
        return super.getHttpMethod();
    }

    @Override
    public GenericHttpRequestParams.HttpVersion getHttpVersion() {
        return super.getHttpVersion();
    }

    @Override
    public void setHttpVersion(GenericHttpRequestParams.HttpVersion httpVersion) {
        super.setHttpVersion(httpVersion);
    }

    @Override
    public boolean initializesRequest() {
        return false;
    }

    @Override
    public boolean needsInitializedRequest() {
        return false;
    }

    @Override
    public boolean initializesResponse() {
        return false;
    }

    @Override
    public boolean needsInitializedResponse() {
        return false;
    }

    @Override
    public String retrievePolicyGuid() {
        return getPolicyGuid();
    }

    @Override
    public Policy retrieveFragmentPolicy() {
        return fragmentPolicy;
    }

    @Override
    public void replaceFragmentPolicy(Policy policy) {
        fragmentPolicy = policy;
    }

    @Override
    public void updateTemporaryData(Assertion assertion) {
        if(!(assertion instanceof AsyncHttpRoutingAssertion)) {
            return;
        }

        AsyncHttpRoutingAssertion includeAssertion = (AsyncHttpRoutingAssertion)assertion;
        fragmentPolicy = includeAssertion.retrieveFragmentPolicy();
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.OPTIONAL, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        List<EntityHeader> ret = new ArrayList<EntityHeader>(Arrays.asList(super.getEntitiesUsed()));

        GuidEntityHeader header = new GuidEntityHeader(policyGuid, EntityType.POLICY, null, null, null);
        header.setGuid( policyGuid );
        ret.add(header);

        return ret.toArray(new EntityHeader[ret.size()]);
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader instanceof PolicyHeader)
            policyGuid = ((GuidEntityHeader) newEntityHeader).getGuid();
        else
            super.replaceEntity(oldEntityHeader, newEntityHeader);
    }

    private static final String META_INITIALIZED = AsyncHttpRoutingAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(HttpMethod.class, "httpMethod"),
            new Java5EnumTypeMapping(GenericHttpRequestParams.HttpVersion.class, "httpVersion"),
            new CollectionTypeMapping(List.class, HttpRoutingServiceParameter.class, ArrayList.class, "parameters"),
            new BeanTypeMapping(HttpRoutingServiceParameter.class, "parameter")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
