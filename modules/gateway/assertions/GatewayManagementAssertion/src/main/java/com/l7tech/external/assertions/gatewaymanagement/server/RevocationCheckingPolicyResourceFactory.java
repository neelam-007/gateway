package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyItemMO;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.Functions;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@ResourceFactory.ResourceType(type=RevocationCheckingPolicyMO.class)
public class RevocationCheckingPolicyResourceFactory extends SecurityZoneableEntityManagerResourceFactory<RevocationCheckingPolicyMO, RevocationCheckPolicy, EntityHeader> {

    //- PUBLIC

    public RevocationCheckingPolicyResourceFactory( final RbacServices services,
                                                    final SecurityFilter securityFilter,
                                                    final PlatformTransactionManager transactionManager,
                                                    final RevocationCheckPolicyManager revocationCheckPolicyManager,
                                                    final SecurityZoneManager securityZoneManager ) {
        super( false, true, services, securityFilter, transactionManager, revocationCheckPolicyManager, securityZoneManager );
    }

    //- PROTECTED

    @Override
    public RevocationCheckingPolicyMO asResource( final RevocationCheckPolicy entity ) {
        final RevocationCheckingPolicyMO revocationCheckingPolicy = ManagedObjectFactory.createRevocationCheckingPolicy();

        revocationCheckingPolicy.setName( entity.getName() );
        revocationCheckingPolicy.setDefaultPolicy(entity.isDefaultPolicy());
        revocationCheckingPolicy.setDefaultSuccess(entity.isDefaultSuccess());
        revocationCheckingPolicy.setContinueOnServerUnavailable(entity.isContinueOnServerUnavailable());

        List<RevocationCheckingPolicyItemMO> items = new ArrayList<>();
        for(RevocationCheckPolicyItem item: entity.getRevocationCheckItems()){
            RevocationCheckingPolicyItemMO itemMO = ManagedObjectFactory.createRevocationCheckingPolicyItem();
            switch (item.getType()){
                case CRL_FROM_CERTIFICATE:
                    itemMO.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
                    break;
                case CRL_FROM_URL:
                    itemMO.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_URL);
                    break;
                case OCSP_FROM_CERTIFICATE:
                    itemMO.setType(RevocationCheckingPolicyItemMO.Type.OCSP_FROM_CERTIFICATE);
                    break;
                case OCSP_FROM_URL:
                    itemMO.setType(RevocationCheckingPolicyItemMO.Type.OCSP_FROM_URL);
                    break;
                default:
                    throw new ResourceAccessException("Unknown revocation checking policy item type '"+item.getType()+"'.");
            }
            itemMO.setUrl(item.getUrl());
            itemMO.setAllowIssuerSignature(item.isAllowIssuerSignature());
            itemMO.setTrustedSigners(Functions.map(item.getTrustedSigners(), new Functions.Unary<String, Goid>() {
                @Override
                public String call(final Goid goid) {
                    return goid.toString();
                }
            }));
            items.add(itemMO);
        }
        revocationCheckingPolicy.setRevocationCheckItems(items);

        // handle SecurityZone
        doSecurityZoneAsResource( revocationCheckingPolicy, entity );

        return revocationCheckingPolicy;
    }

    @Override
    public RevocationCheckPolicy fromResource(Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof RevocationCheckingPolicyMO)) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected RevocationCheckingPolicy");
        }
        final RevocationCheckingPolicyMO revocationCheckingPolicyMO = (RevocationCheckingPolicyMO) resource;
        final RevocationCheckPolicy revocationCheckPolicy = new RevocationCheckPolicy();

        revocationCheckPolicy.setName(revocationCheckingPolicyMO.getName());
        revocationCheckPolicy.setDefaultPolicy(revocationCheckingPolicyMO.isDefaultPolicy());
        revocationCheckPolicy.setDefaultSuccess(revocationCheckingPolicyMO.isDefaultSuccess());
        revocationCheckPolicy.setContinueOnServerUnavailable(revocationCheckingPolicyMO.isContinueOnServerUnavailable());

        if(revocationCheckingPolicyMO.getRevocationCheckItems()!=null) {
            List<RevocationCheckPolicyItem> items = new ArrayList<>();
            for (RevocationCheckingPolicyItemMO itemMO : revocationCheckingPolicyMO.getRevocationCheckItems()) {
                RevocationCheckPolicyItem item = new RevocationCheckPolicyItem();
                switch (itemMO.getType()) {
                    case CRL_FROM_CERTIFICATE:
                        item.setType(RevocationCheckPolicyItem.Type.CRL_FROM_CERTIFICATE);
                        break;
                    case CRL_FROM_URL:
                        item.setType(RevocationCheckPolicyItem.Type.CRL_FROM_URL);
                        break;
                    case OCSP_FROM_CERTIFICATE:
                        item.setType(RevocationCheckPolicyItem.Type.OCSP_FROM_CERTIFICATE);
                        break;
                    case OCSP_FROM_URL:
                        item.setType(RevocationCheckPolicyItem.Type.OCSP_FROM_URL);
                        break;
                    default:
                        throw new ResourceAccessException("Unknown revocation checking policy item type '" + item.getType() + "'.");
                }
                item.setUrl(itemMO.getUrl());
                item.setAllowIssuerSignature(itemMO.isAllowIssuerSignature());
                item.setTrustedSigners(Functions.map(itemMO.getTrustedSigners(), new Functions.Unary<Goid, String>() {
                    @Override
                    public Goid call(final String string) {
                        return Goid.parseGoid(string);
                    }
                }));
                items.add(item);
            }
            revocationCheckPolicy.setRevocationCheckItems(items);
        }

        return revocationCheckPolicy;
    }


    @Override
    protected void updateEntity(RevocationCheckPolicy oldEntity, RevocationCheckPolicy newEntity) throws InvalidResourceException {
        oldEntity.setName(newEntity.getName());
        oldEntity.setDefaultPolicy(newEntity.isDefaultPolicy());
        oldEntity.setDefaultSuccess(newEntity.isDefaultSuccess());
        oldEntity.setContinueOnServerUnavailable(newEntity.isContinueOnServerUnavailable());
        oldEntity.setRevocationCheckItems(newEntity.getRevocationCheckItems());
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }
}
