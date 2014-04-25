package com.l7tech.server.licensing;

import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityManagerStub;

public class LicenseDocumentManagerStub extends EntityManagerStub<LicenseDocument, EntityHeader> implements LicenseDocumentManager {

    public LicenseDocumentManagerStub() {
        super();
    }

    public LicenseDocumentManagerStub(final LicenseDocument... entitiesIn) {
        super( entitiesIn ); 
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return LicenseDocument.class;
    }

    @Override
    public Goid saveWithImmediateFlush(LicenseDocument entity) throws SaveException {
        return save(entity);
    }
}
