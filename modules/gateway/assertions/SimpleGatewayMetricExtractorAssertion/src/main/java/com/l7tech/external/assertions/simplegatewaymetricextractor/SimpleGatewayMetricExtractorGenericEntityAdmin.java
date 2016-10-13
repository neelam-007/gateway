package com.l7tech.external.assertions.simplegatewaymetricextractor;

import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.GENERIC;

@Secured(types=GENERIC)
public interface SimpleGatewayMetricExtractorGenericEntityAdmin {

    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_ENTITIES)
    Collection<SimpleGatewayMetricExtractorEntity> findAll() throws FindException;

    @Secured(stereotype=SAVE_OR_UPDATE)
    Goid save(SimpleGatewayMetricExtractorEntity entity) throws SaveException, UpdateException;

    @Secured(stereotype=DELETE_ENTITY)
    void delete(SimpleGatewayMetricExtractorEntity entity) throws DeleteException, FindException;
}
