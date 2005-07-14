/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.Collection;

/**
 * This manager gives access to the community schemas included in the
 * schema table. This is meant to be used by the server schema validation
 * implementation using tarari
 *
 * @author flascelles@layer7-tech.com
 */
public class CommunitySchemaManager extends HibernateDaoSupport {

    public CommunitySchemaManager() {
    }

    public Collection findAll() {
        // todo
        return null;
    }

    public void save(CommunitySchemaEntry newSchema) {
        // todo
    }

    public void update(CommunitySchemaEntry existingSchema) {
        // todo
    }
}
