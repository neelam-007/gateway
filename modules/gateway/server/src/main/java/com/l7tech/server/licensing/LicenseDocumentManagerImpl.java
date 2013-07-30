package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of LicenseDocument Entity management.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class LicenseDocumentManagerImpl extends HibernateEntityManager<LicenseDocument, EntityHeader> implements LicenseDocumentManager {

    @Override
    public Class<? extends Entity> getImpClass() {
        return LicenseDocument.class;
    }

    @Override
    public long save(LicenseDocument entity) throws SaveException {
        return super.save(entity);
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
