/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.JmsEndpointHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.HibernateGoidEntityManager;
import org.apache.http.util.EntityUtils;
import org.springframework.dao.DataAccessException;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsEndpointManagerImpl
        extends HibernateGoidEntityManager<JmsEndpoint, JmsEndpointHeader>
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
    public JmsEndpoint[] findEndpointsForConnection(Goid connectionOid) throws FindException {
        StringBuffer sql = new StringBuffer("select endpoint.goid, endpoint.name, endpoint.destinationName, endpoint.disabled ");
        sql.append("from endpoint in class ");
        sql.append(JmsEndpoint.class.getName());
        sql.append(" where endpoint.connectionGoid = ?");
        ArrayList<JmsEndpoint> result = new ArrayList<JmsEndpoint>();
        try {
            List results = getHibernateTemplate().find(sql.toString(), connectionOid);
            for (Object result1 : results) {
                Object[] row = (Object[]) result1;
                if (row[0]instanceof Goid) {
                    Goid goid = ((Goid) row[0]);
                    result.add(findByPrimaryKey(goid));
                }
            }

        } catch (DataAccessException e) {
            throw new FindException(e.toString(), e);
        }
        return result.toArray(new JmsEndpoint[0]);
    }

    public JmsEndpointHeader[] findEndpointHeadersForConnection(Goid connectionOid) throws FindException {
        StringBuffer sql = new StringBuffer("select endpoint.goid, endpoint.name, endpoint.destinationName, endpoint.version, endpoint.messageSource ");
        sql.append("from endpoint in class ");
        sql.append(JmsEndpoint.class.getName());
        sql.append(" where endpoint.connectionGoid = ?");
        ArrayList<JmsEndpointHeader> result = new ArrayList<JmsEndpointHeader>();
        try {
            List results = getHibernateTemplate().find(sql.toString(),  connectionOid);
            for (Object result1 : results) {
                Object[] row = (Object[]) result1;
                if (row[0]instanceof Goid) {
                    JmsEndpointHeader header = new JmsEndpointHeader(row[0].toString(), String.valueOf(row[1]), String.valueOf(row[2]), Integer.parseInt(String.valueOf(row[3])), Boolean.valueOf(String.valueOf(row[4])));
                    header.setConnectionGoid(connectionOid);
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

    @Override
    protected JmsEndpointHeader newHeader(final JmsEndpoint entity) {
        JmsEndpointHeader header = null;
        if (entity != null) {
            final EntityHeader fromEntity = EntityHeaderUtils.fromEntity(entity);
            if (fromEntity instanceof JmsEndpointHeader) {
                header = (JmsEndpointHeader)fromEntity;
            }
        }
        return header != null ? header : super.newHeader(entity);
    }

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
