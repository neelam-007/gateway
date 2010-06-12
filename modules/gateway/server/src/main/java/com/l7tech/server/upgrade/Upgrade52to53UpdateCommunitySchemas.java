/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.schema.SchemaEntry;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Upgrade new name_hash column of community_schemas table for 5.2 to 5.3
 */
public class Upgrade52to53UpdateCommunitySchemas implements UpgradeTask {
    private ApplicationContext applicationContext;

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        //Cannot use the SchemaEntryManager as it will stop internal schemas from being updated. This manager is also only
        //available after the 'Starting' event, as it indirectly depends on bean httpClientFactory, which is still being
        //created when afterPropertiesSet() is called
        SessionFactory sessionFactory = getBean("sessionFactory", SessionFactory.class);
        try {
            new HibernateTemplate(sessionFactory).execute( new HibernateCallback(){
                @Override
                public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                    // Client certificate DNs
                    Criteria schemaCriteria = session.createCriteria(SchemaEntry.class);
                    for(Object schemaCriteriaObj: schemaCriteria.list()){
                        if(schemaCriteriaObj instanceof SchemaEntry){
                            SchemaEntry schemaEntry = (SchemaEntry) schemaCriteriaObj;
                            schemaEntry.setName(schemaEntry.getName());
                        }
                    }
                    return null;
                }
            } );
        } catch (Exception e) {
            throw new FatalUpgradeException(e);
            //this means that the name_hash column will equal the name column and those schemas will not be able to be found
        }
    }


    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    private <T> T getBean( final String name,
                           final Class<T> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            //noinspection unchecked
            return applicationContext.getBean(name, beanClass);
        } catch ( BeansException be ) {
            throw new FatalUpgradeException("Error accessing  bean '"+name+"' from ApplicationContext: " + ExceptionUtils.getMessage(be));
        }
    }

}
