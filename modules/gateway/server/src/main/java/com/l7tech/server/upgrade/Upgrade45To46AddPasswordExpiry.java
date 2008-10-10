package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;
import org.hibernate.SessionFactory;
import org.hibernate.Session;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import com.l7tech.util.ResourceUtils;

/**
 * Task to add STIG compilance password expiry value for existing internal uses only.  This way, during an upgrade, all
 * existing internal users wouldn't require to change their password right when they log into SSM.
 *
 * User: dlee
 * Date: Jul 9, 2008
 */
public class Upgrade45To46AddPasswordExpiry implements UpgradeTask {

    private static final String INTERNAL_USER_TABLE = "internal_user";
    private static final String PASSWORD_EXPIRY_COLUMN = "password_expiry";

    private static final String SQL_UPDATE_PASSWORD_EXPIRY = "UPDATE " + INTERNAL_USER_TABLE + " SET " + PASSWORD_EXPIRY_COLUMN + " = ?";

    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        SessionFactory sessionFactory;
        Session session;
        try {
            sessionFactory = (SessionFactory) applicationContext.getBean("sessionFactory");
            if (sessionFactory == null)
                throw new FatalUpgradeException("Couldn't get required components (sessionFactory)");
            session = sessionFactory.getCurrentSession();
            if (session == null) throw new FatalUpgradeException("Couldn't get required components (session)");
        } catch (BeansException e) {
            throw new FatalUpgradeException("Couldn't get required components");
        }

        //add STIG compilance passwordy expiry date for existing internal users only
        addPasswordExpiryValue(session);

    }

    private void addPasswordExpiryValue(Session session) throws FatalUpgradeException {
        PreparedStatement updateStmt = null;

        try {
            long now = System.currentTimeMillis();
            Calendar expireDate = Calendar.getInstance();
            expireDate.setTimeInMillis(now);
            expireDate.add(Calendar.DAY_OF_YEAR, 90);   //give the default expiry date for existing user (90 days)
            long expiryTime = expireDate.getTimeInMillis();

            updateStmt = session.connection().prepareStatement(SQL_UPDATE_PASSWORD_EXPIRY);
            updateStmt.setLong(1, expiryTime);
            updateStmt.execute();

        } catch (SQLException sqle) {
            throw new FatalUpgradeException("Failed to set passwordy expiry for existing internal users.", sqle);
        } finally {
            //clean up
            ResourceUtils.closeQuietly(updateStmt);
        }
    }
}
