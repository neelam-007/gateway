package com.ca.datasources.cassandra;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 10/30/14
 */
public class CassandraQueryManager {

     PreparedStatement buildPreparedStatement(Session session, String queryString) {
          return session.prepare(queryString);
     }

}
