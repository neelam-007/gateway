package com.l7tech.server.ems.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

public class MigrationRecordHeader extends EntityHeader {

    public MigrationRecordHeader(long oid, String name, int version) {
        super(oid, EntityType.ESM_MIGRATION_RECORD, name, "", version);
    }
}
