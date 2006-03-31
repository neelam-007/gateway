/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public final class ServerResourceUtils {
    private static final Logger logger = Logger.getLogger(ServerResourceUtils.class.getName());

    private ServerResourceUtils() { }

    public static void closeQuietly(Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (HibernateException e) {
                logger.log(Level.INFO, "Exception when closing Session", e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when closing Session", e);
            }
        }
    }

    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.INFO, "Exception when closing PreparedStatement", e);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception when closing PreparedStatement", e);
            }
        }
    }
}
