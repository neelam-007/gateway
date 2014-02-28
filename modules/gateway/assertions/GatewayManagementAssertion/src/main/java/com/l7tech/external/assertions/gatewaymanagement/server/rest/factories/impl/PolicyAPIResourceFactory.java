package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyTransformer;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyDetail;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.UUID;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class PolicyAPIResourceFactory extends WsmanBaseResourceFactory<PolicyMO, PolicyResourceFactory> {

    @Inject
    private PlatformTransactionManager transactionManager;

    public PolicyAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").put("parentFolder.id", "parentFolder.id").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("guid", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("guid", RestResourceFactoryUtils.stringConvert))
                        .put("type", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("type", RestResourceFactoryUtils.stringConvert))
                        .put("soap", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("soap", RestResourceFactoryUtils.booleanConvert))
                        .put("parentFolder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.POLICY.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public PolicyMO getResourceTemplate() {
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        policyMO.setGuid("guid-8757cdae-d1ad-4ad5-bc08-b16b2d370759");

        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyDetail.setGuid("guid-8757cdae-d1ad-4ad5-bc08-b16b2d370759");
        policyDetail.setFolderId("FolderID");
        policyDetail.setName("Policy Name");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("PropertyKey", "PropertyValue").map());

        policyMO.setPolicyDetail(policyDetail);
        return policyMO;
    }

    @Inject
    private PolicyVersionManager policyVersionManager;
    @Inject
    private PolicyManager policyManager;

    @Inject
    private PolicyTransformer policyTransformer;

    public String createResource(@NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        return createResourceInternal(null, resource, comment);
    }

    public void createResource(@NotNull final String id, @NotNull final PolicyMO resource, final String comment) throws ResourceFactory.InvalidResourceException {
        createResourceInternal(id, resource, comment);
    }

    private String createResourceInternal(@Nullable final String id, @NotNull final PolicyMO resource, @Nullable final String comment) {
        validateCreateResource(id, resource);
        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        String savedId = tt.execute( new TransactionCallback<String>(){
            @Override
            public String doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    Policy newPolicy = policyTransformer.convertFromMO(resource);
                    newPolicy.setVersion(0);
                    //generate a new Guid if none is set.
                    if(newPolicy.getGuid() == null) {
                        newPolicy.setGuid(UUID.randomUUID().toString());
                    }

                    final String savedId;
                    if(id == null){
                        savedId = policyManager.save(newPolicy).toString();
                    } else {
                        policyManager.save(Goid.parseGoid(id),newPolicy);
                        savedId = id;
                    }
                    policyVersionManager.checkpointPolicy(newPolicy, true , comment, true);

                    return savedId;
                } catch(ResourceFactory.InvalidResourceException e){
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch (IllegalArgumentException e) {
                    transactionStatus.setRollbackOnly();
                    Exception ire =  new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, ExceptionUtils.getMessage(e));
                    throw new ResourceFactory.ResourceAccessException(ire);
                } catch ( ObjectModelException ome ) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to save policy version when updating policy.", ome);
                }
            }
        });
        resource.setId(savedId);
        return savedId;
    }

    private void validateCreateResource(@Nullable String id, PolicyMO resource) {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when creating a new entity, or id must equal new entity id");
        }
    }

    public void updateResource(final @NotNull String id, final @NotNull PolicyMO resource, final String comment, final boolean active) throws ResourceFactory.ResourceNotFoundException, ResourceFactory.InvalidResourceException {
        if (resource.getId() != null && !StringUtils.equals(id, resource.getId())) {
            throw new IllegalArgumentException("Must not specify an ID when updating a new entity, or id must equal entity id");
        }

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly( false );
        tt.execute( new TransactionCallback(){
            @Override
            public Object doInTransaction( final TransactionStatus transactionStatus ) {
                try {
                    Policy newPolicy = policyTransformer.convertFromMO(resource);
                    policyManager.update(newPolicy);
                    policyVersionManager.checkpointPolicy(newPolicy, active, comment, false);

                    return policyTransformer.convertToMO(newPolicy);

                }catch(ResourceFactory.InvalidResourceException e){
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException(e);
                } catch ( ObjectModelException ome ) {
                    transactionStatus.setRollbackOnly();
                    throw new ResourceFactory.ResourceAccessException("Unable to save policy version when updating policy.", ome);
                }
            }
        });
    }
}