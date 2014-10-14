package com.l7tech.server.service;

import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.EntityManagerStub;

import java.util.ArrayList;
import java.util.List;

/**
 * Class SampleMessageManagerStub.
 * 
 */
public class SampleMessageManagerStub extends EntityManagerStub<SampleMessage, EntityHeader> implements SampleMessageManager {

    public SampleMessageManagerStub(final SampleMessage ... entitiesIn){
        super(entitiesIn);
    }
    public SampleMessageManagerStub(){
        super();
    }

    @Override
    public Class<? extends SampleMessage> getImpClass() {
        return SampleMessage.class;
    }

    @Override
    public EntityHeader[] findHeaders(Goid serviceId, String operationName) throws FindException {
        List<EntityHeader> found = new ArrayList<>();
        for(SampleMessage message: entities.values()){
            if(message.getServiceGoid().equals(serviceId) && message.getOperationName().equals(operationName)){
                found.add(EntityHeaderUtils.fromEntity(message));
            }
        }
        return found.toArray(new EntityHeader[found.size()]);
    }
}
