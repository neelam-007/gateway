/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jul 14, 2005<br/>
 */
package com.l7tech.server.communityschemas;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.Collection;
import java.util.logging.Logger;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

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

    public Collection findAll() throws FindException {
        String queryall = "from " + TABLE_NAME + " in class " + CommunitySchemaEntry.class.getName();
        Collection output = getHibernateTemplate().find(queryall);
        return output;
    }

    public long save(CommunitySchemaEntry newSchema) throws SaveException {
        return ((Long)getHibernateTemplate().save(newSchema)).longValue();
    }

    public void update(CommunitySchemaEntry existingSchema) throws UpdateException {
        getHibernateTemplate().update(existingSchema);
    }

    private static final String TABLE_NAME = "community_schema";
    private final Logger logger = Logger.getLogger(CommunitySchemaManager.class.getName());
}
