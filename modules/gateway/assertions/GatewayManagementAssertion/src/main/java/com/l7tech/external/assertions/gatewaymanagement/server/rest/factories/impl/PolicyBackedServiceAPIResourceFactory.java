package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyBackedServiceTransformer;
import com.l7tech.gateway.api.PolicyBackedServiceMO;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;

@Component
public class PolicyBackedServiceAPIResourceFactory extends
        EntityManagerAPIResourceFactory<PolicyBackedServiceMO, PolicyBackedService, EntityHeader> {

    @Inject
    private PolicyBackedServiceTransformer transformer;
    @Inject
    private PolicyBackedServiceManager policyBackedServiceManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.POLICY_BACKED_SERVICE;
    }

    @Override
    protected PolicyBackedService convertFromMO(PolicyBackedServiceMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected PolicyBackedServiceMO convertToMO(PolicyBackedService entity) {
        return transformer.convertToMO(entity, null);
    }

    @Override
    protected PolicyBackedServiceManager getEntityManager() {
        return policyBackedServiceManager;
    }

    @Override
    protected void beforeUpdateEntity(@NotNull final PolicyBackedService newEntity, @NotNull final PolicyBackedService oldEntity) throws ObjectModelException {
        super.beforeUpdateEntity(newEntity, oldEntity);

        //TODO: This logic is duplicated in com.l7tech.server.bundling.EntityBundleImporterImpl.beforeCreateOrUpdateEntities()
        //This is needed in order to avoid hibernate issues related to updating operations to a PolicyBackedService
        //update to existing operations to match the updated ones.
        final Set<PolicyBackedServiceOperation> operations = oldEntity.getOperations();
        final Iterator<PolicyBackedServiceOperation> operationsIterator = operations.iterator();
        while(operationsIterator.hasNext()){
            //This will update the existing operations to match the updated ones. And remove existing operations that are no longer in the updated PBS
            final PolicyBackedServiceOperation operation = operationsIterator.next();
            //Find the new operation with the same operation name a this existing operation
            final PolicyBackedServiceOperation newOperation = Functions.grepFirst(newEntity.getOperations(), new Functions.Unary<Boolean, PolicyBackedServiceOperation>() {
                @Override
                public Boolean call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                    return StringUtils.equals(policyBackedServiceOperation.getName(), operation.getName());
                }
            });
            if(newOperation != null) {
                //updated the existing operation policy id to match the new one
                operation.setPolicyGoid(newOperation.getPolicyGoid());
            } else {
                //remove the existing operation since it is not available in the new PBS.
                operationsIterator.remove();
            }
        }
        //This will add any new operations to the list of existing operations.
        for(final PolicyBackedServiceOperation operation : newEntity.getOperations()){
            //Find the existing operation with the same operation name a this new operation
            final PolicyBackedServiceOperation oldOperation = Functions.grepFirst(oldEntity.getOperations(), new Functions.Unary<Boolean, PolicyBackedServiceOperation>() {
                @Override
                public Boolean call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                    return StringUtils.equals(policyBackedServiceOperation.getName(), operation.getName());
                }
            });
            //If there is no existing operation with the same name then add a new one.
            if(oldOperation == null){
                operations.add(operation);
            }
        }
        newEntity.setOperations(operations);
    }
}
