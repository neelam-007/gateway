package com.l7tech.skunkworks;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
public class SchemaUpdater {
    private final Configuration config;

    public SchemaUpdater() {
        config = new Configuration();
        config.addFile("etc/db/SSG.hbm.xml");
        config.addFile("etc/db/audit.hbm.xml");
        config.addFile("etc/db/rbac.hbm.xml");
    }

    public static void main(String[] args) {
        SchemaUpdater me = new SchemaUpdater();
        me.doIt();
    }

    private void doIt() {
        new SchemaUpdate(config).execute(false, true);
    }
}
