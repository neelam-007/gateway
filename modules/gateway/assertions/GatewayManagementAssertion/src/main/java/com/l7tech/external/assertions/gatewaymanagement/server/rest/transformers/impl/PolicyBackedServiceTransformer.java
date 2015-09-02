package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyBackedServiceMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.objectmodel.polback.PolicyBackedServiceOperation;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;

@Component
public class PolicyBackedServiceTransformer extends EntityManagerAPITransformer<PolicyBackedServiceMO, PolicyBackedService> implements EntityAPITransformer<PolicyBackedServiceMO, PolicyBackedService> {

    @Inject
    PolicyBackedServiceManager policyBackedServiceManager;

    @Inject
    SecurityZoneManager securityZoneManager;

    @NotNull
    @Override
    public PolicyBackedServiceMO convertToMO(@NotNull PolicyBackedService policyBackedService, SecretsEncryptor secretsEncryptor) {
        PolicyBackedServiceMO policyBackedServiceMO = ManagedObjectFactory.createPolicyBackedServiceMO();
        policyBackedServiceMO.setId(policyBackedService.getId());
        policyBackedServiceMO.setVersion(policyBackedService.getVersion());
        policyBackedServiceMO.setName(policyBackedService.getName());
        policyBackedServiceMO.setInterfaceName(policyBackedService.getServiceInterfaceName());
        policyBackedServiceMO.setPolicyBackedServiceOperations(Functions.map(policyBackedService.getOperations(), new Functions.Unary<PolicyBackedServiceMO.PolicyBackedServiceOperation, PolicyBackedServiceOperation>() {
            @Override
            public PolicyBackedServiceMO.PolicyBackedServiceOperation call(PolicyBackedServiceOperation policyBackedServiceOperation) {
                PolicyBackedServiceMO.PolicyBackedServiceOperation operation = new PolicyBackedServiceMO.PolicyBackedServiceOperation();
                operation.setOperationName(policyBackedServiceOperation.getName());
                operation.setPolicyId(policyBackedServiceOperation.getPolicyGoid().toString());
                return operation;
            }
        }));
        doSecurityZoneToMO(policyBackedServiceMO, policyBackedService);

        return policyBackedServiceMO;
    }

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.POLICY_BACKED_SERVICE.toString();
    }

    @NotNull
    @Override
    public PolicyBackedServiceMO convertToMO(@NotNull EntityContainer<PolicyBackedService> policyBackedServiceEntityContainer, SecretsEncryptor secretsEncryptor) {
        return convertToMO(policyBackedServiceEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<PolicyBackedService> convertFromMO(@NotNull PolicyBackedServiceMO policyBackedServiceMO, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(policyBackedServiceMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<PolicyBackedService> convertFromMO(@NotNull PolicyBackedServiceMO policyBackedServiceMO, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        final PolicyBackedService policyBackedService = new PolicyBackedService();
        policyBackedService.setId(policyBackedServiceMO.getId());
        if (policyBackedServiceMO.getVersion() != null) {
            policyBackedService.setVersion(policyBackedServiceMO.getVersion());
        }
        policyBackedService.setName(policyBackedServiceMO.getName());
        policyBackedService.setServiceInterfaceName(policyBackedServiceMO.getInterfaceName());
        policyBackedService.setOperations(new HashSet<>(Functions.map(policyBackedServiceMO.getPolicyBackedServiceOperations(), new Functions.Unary<PolicyBackedServiceOperation, PolicyBackedServiceMO.PolicyBackedServiceOperation>() {
            @Override
            public PolicyBackedServiceOperation call(PolicyBackedServiceMO.PolicyBackedServiceOperation operation) {
                PolicyBackedServiceOperation policyBackedServiceOperation = new PolicyBackedServiceOperation();
                policyBackedServiceOperation.setPolicyBackedService(policyBackedService);
                policyBackedServiceOperation.setPolicyGoid(Goid.parseGoid(operation.getPolicyId()));
                policyBackedServiceOperation.setName(operation.getOperationName());
                return policyBackedServiceOperation;
            }
        })));
        doSecurityZoneFromMO(policyBackedServiceMO, policyBackedService, strict);

        return new EntityContainer<>(policyBackedService);
    }

    @NotNull
    @Override
    public Item<PolicyBackedServiceMO> convertToItem(@NotNull PolicyBackedServiceMO policyBackedServiceMO) {
        return new ItemBuilder<PolicyBackedServiceMO>(policyBackedServiceMO.getName(), policyBackedServiceMO.getId(), EntityType.POLICY_BACKED_SERVICE.name())
                .setContent(policyBackedServiceMO)
                .build();
    }
}
