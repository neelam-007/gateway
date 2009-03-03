/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.dao.DataAccessException;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsEndpointManagerImpl
        extends HibernateEntityManager<JmsEndpoint, JmsEndpointHeader>
        implements JmsEndpointManager
{
    public Collection findMessageSourceEndpoints() throws FindException {
        StringBuffer query = new StringBuffer("from endpoint in class ");
        query.append(JmsEndpoint.class.getName());
        query.append(" where endpoint.messageSource = ?");
        try {
            return getHibernateTemplate().find(query.toString(), Boolean.TRUE);
        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public JmsEndpoint[] findEndpointsForConnection(long connectionOid) throws FindException {
        StringBuffer sql = new StringBuffer("select endpoint.oid, endpoint.name, endpoint.destinationName, endpoint.disabled ");
        sql.append("from endpoint in class ");
        sql.append(JmsEndpoint.class.getName());
        sql.append(" where endpoint.connectionOid = ?");
        ArrayList<JmsEndpoint> result = new ArrayList<JmsEndpoint>();
        try {
            List results = getHibernateTemplate().find(sql.toString(), new Long(connectionOid));
            for (Object result1 : results) {
                Object[] row = (Object[]) result1;
                if (row[0]instanceof Long) {
                    long oid = ((Long) row[0]).longValue();
                    result.add(findByPrimaryKey(oid));
                }
            }

        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
        return result.toArray(new JmsEndpoint[0]);
    }

    public JmsEndpointHeader[] findEndpointHeadersForConnection(long connectionOid) throws FindException {
        StringBuffer sql = new StringBuffer("select endpoint.oid, endpoint.name, endpoint.destinationName, endpoint.version, endpoint.messageSource ");
        sql.append("from endpoint in class ");
        sql.append(JmsEndpoint.class.getName());
        sql.append(" where endpoint.connectionOid = ?");
        ArrayList<JmsEndpointHeader> result = new ArrayList<JmsEndpointHeader>();
        try {
            List results = getHibernateTemplate().find(sql.toString(), new Long(connectionOid));
            for (Object result1 : results) {
                Object[] row = (Object[]) result1;
                if (row[0]instanceof Long) {
                    JmsEndpointHeader header = new JmsEndpointHeader(String.valueOf(row[0]), String.valueOf(row[1]), String.valueOf(row[2]), Integer.parseInt(String.valueOf(row[3])), Boolean.valueOf(String.valueOf(row[4])));
                    result.add(header);
                }
            }
            return result.toArray(new JmsEndpointHeader[result.size()]);
        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    public Collection<JmsEndpointHeader> findHeaders(int offset, int windowSize, Map<String, String> filters) throws FindException {
        Map<String,String> jmsFilters = filters;
        String defaultFilter = filters.get(DEFAULT_SEARCH_NAME);
        if (defaultFilter != null && ! defaultFilter.isEmpty()) {
            jmsFilters = new HashMap<String, String>(filters);
            jmsFilters.put("name", defaultFilter);
        }
        jmsFilters.remove(DEFAULT_SEARCH_NAME);

        return doFindHeaders( offset, windowSize, jmsFilters, false ); // conjunction
    }

/*
    public void update(final JmsEndpoint endpoint) throws VersionException, UpdateException {
        _logger.info("Updating JmsEndpoint" + endpoint);

        JmsEndpoint original;
        // check for original endpoint
        try {
            original = findByPrimaryKey(endpoint.getOid());
        } catch (FindException e) {
            throw new UpdateException("could not get original endpoint", e);
        }

        // check version
        if (original.getVersion() != endpoint.getVersion()) {
            logger.severe("db endpoint has version: " + original.getVersion() + ". requestor endpoint has version: "
              + endpoint.getVersion());
            throw new VersionException("the endpoint you are trying to update is no longer valid.");
        }

        // update
        original.copyFrom(endpoint);
        getHibernateTemplate().update(original);
        logger.info("Updated JmsEndpoint #" + endpoint.getOid());
    }
*/

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    public Class getImpClass() {
        return JmsEndpoint.class;
    }

    public Class getInterfaceClass() {
        return JmsEndpoint.class;
    }

    public String getTableName() {
        return "jms_endpoint";
    }

    public EntityType getEntityType() {
        return EntityType.JMS_ENDPOINT;
    }
}
