package com.l7tech.server.util;

import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.ResourceUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway version of GoidUpgradeMapper that initializes the map from the database.
 */
public class ServerGoidUpgradeMapper extends GoidUpgradeMapper implements ApplicationContextAware, InitializingBean {
    private static final Logger logger = Logger.getLogger(GoidUpgradeMapper.class.getName());

    public static final String GOID_UPGRADE_MAP_TABLE = "goid_upgrade_map";
    public static final String PREFIX_COLUMN = "prefix";
    public static final String TABLE_NAME_COLUMN = "table_name";
    public static final String GET_BY_ENTITY_TYPE_STMT = "SELECT " + PREFIX_COLUMN + ", " + TABLE_NAME_COLUMN + " FROM " + GOID_UPGRADE_MAP_TABLE;

    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        SessionFactory sf = applicationContext.getBean("sessionFactory", SessionFactory.class);
        Session session;
        session = sf.openSession();

        //This will load any prefixes generated during an upgrade by looking at the goid_upgrade_map table
        //If the table doesn't exist no error if thrown and no prefixes are loaded.
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                Statement st = null;
                ResultSet rs = null;

                try {
                    st = connection.createStatement();
                    try {
                        rs = st.executeQuery(GET_BY_ENTITY_TYPE_STMT);
                        logger.log(Level.FINE, "Found Goid prefixes generated by an 8.0.0 upgrade, populating the GoidUpgradeMapper.");
                    } catch (SQLException e) {
                        // Do nothing here this means that the prefix table was never created and so this gateway never
                        // went through the 8.0 (Halibut) upgrade process
                        logger.log(Level.FINE, "Did not find any Goid prefixes generated by an 8.0.0 upgrade.");
                        return;
                    }
                    //Let errors be thrown from below. It is unexpected if they do get thrown at this point.
                    Map<String, Long> prefixes = new HashMap<>();
                    while (rs.next()) {
                        //we have a table, now check the contents
                        Long prefix = rs.getLong(PREFIX_COLUMN);
                        String entityType = rs.getString(TABLE_NAME_COLUMN);
                        prefixes.put(entityType, prefix);
                    }
                    setPrefixes(prefixes);
                } finally {
                    ResourceUtils.closeQuietly(rs);
                    ResourceUtils.closeQuietly(st);
                }
            }
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
