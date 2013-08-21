package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.objectmodel.*;
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
    public Goid save(LicenseDocument entity) throws SaveException {
        return super.save(entity);
    }

    @Override
    public Goid saveWithImmediateFlush(LicenseDocument entity) throws SaveException {
        Goid key = super.save(entity);
        getHibernateTemplate().flush();

        return key;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }
}
