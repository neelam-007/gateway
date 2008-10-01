package com.l7tech.server;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Entity manager for {@link TrustedEms}.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEmsManagerImpl extends HibernateEntityManager<TrustedEms, EntityHeader> implements TrustedEmsManager {
    public Class<? extends Entity> getImpClass() {
        return TrustedEms.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEms.class;
    }

    public String getTableName() {
        return "trusted_ems";
    }
}
