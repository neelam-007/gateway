package com.l7tech.server.security.keystore;

import com.l7tech.server.upgrade.UpgradeTask;
import com.l7tech.server.upgrade.NonfatalUpgradeException;
import com.l7tech.server.upgrade.FatalUpgradeException;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.UpdateException;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.hibernate.Session;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Runs on startup to create the initial rows in keystore_file, if it's currently empty.
 */
// Normally, an UpgradeTask can't just extend HibernateDaoSupport.  We can get away with this only because we get wired up by Spring
public class DefaultKeystoreFilePopulator extends HibernateDaoSupport implements UpgradeTask {
    protected static final Logger logger = Logger.getLogger(DefaultKeystoreFilePopulator.class.getName());

    private static final String PREFIX = "insert into keystore_file (goid, version, name, format, databytes) values ";
    public static final String INSERT_ROW_0 = PREFIX + "(toGoid(0,0), 0, 'Software Static', 'ss', null)";     // placeholder, never loaded or saved
    public static final String INSERT_ROW_1 = PREFIX + "(toGoid(0,1), 0, 'HSM', 'hsm.sca.targz', null)";      // tar.gz of items in sca 6000 keydata directory
    public static final String INSERT_ROW_2 = PREFIX + "(toGoid(0,2), 0, 'Software DB', 'sdb.pkcs12', null)"; // bytes of a PKCS#12 keystore

    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        // First, see if there is anything for us to do.  We'll take no action if there is already anything
        // in the table.

        boolean alreadyPopulated = false;

        Session session = null;
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            session = getSession();
            conn = session.connection();

            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            rs = st.executeQuery("SELECT COUNT(*) FROM keystore_file");
            if (rs.next()) {
                long numRows = rs.getLong(1);
                if (numRows > 0) {
                    // No need to take action -- table already has stuff in it.
                    alreadyPopulated = true;
                }
            }

            if (!alreadyPopulated) {
                // Need to initialize keystore_file
                logger.info("Creating initial keystore_file entries");
                st.execute(INSERT_ROW_0);
                st.execute(INSERT_ROW_1);
                st.execute(INSERT_ROW_2);
            }

        } catch (SQLException e) {
            throw new NonfatalUpgradeException("Failed to populate keystore_file: " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(st);
            ResourceUtils.closeQuietly(conn);
            if (session != null) releaseSession(session);
        }

        KeystoreFileManager keystoreFileManager = (KeystoreFileManager) applicationContext.getBean("keystoreFileManager");
        try {
            keystoreFileManager.initializeHsmKeystorePasswordFromFile();
        } catch (UpdateException e) {
            logger.severe("Could not initialize the password From the HSM init file: " + ExceptionUtils.getMessage(e));
            throw new FatalUpgradeException(e);
        }
    }
}
