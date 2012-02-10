package com.l7tech.gateway.common.export;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;


/**
 * @author ghuang
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured
@Administrative
public interface PolicyExporterImporterAdmin {
    /**
     * Find all ExternalReferenceFactory's, which have been registered when the gateway loads modular assertions.
     * @return a set of ExternalReferenceFactory's
     */
    Set<ExternalReferenceFactory> findAllExternalReferenceFactories();
}