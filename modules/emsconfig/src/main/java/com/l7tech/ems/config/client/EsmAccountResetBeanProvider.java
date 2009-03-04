package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Account reset for esm
 */
public class EsmAccountResetBeanProvider extends EsmDbConfigurationBeanProvider {

    //- PUBLIC

    EsmAccountResetBeanProvider() {
        super( logger );        
    }

    @Override
    public void storeConfiguration( final Collection<ConfigurationBean> configuration ) throws ConfigurationException {
        String username = null;
        String password = null;

        for ( ConfigurationBean bean : configuration ) {
            if ( CONFIG_ADMIN_USER.equals( bean.getConfigName() ) ) {
                username = (String)bean.getConfigValue();
            } else if ( CONFIG_ADMIN_PASS.equals( bean.getConfigName() ) ) {
                password = (String)bean.getConfigValue();
            }
        }

        if ( username != null &&
             password != null ) {
            String errorMessage = resetAdmin( username, password );
            if ( errorMessage != null ) {
                throw new ConfigurationException(errorMessage);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EsmAccountResetBeanProvider.class.getName() );

    private static final String CONFIG_ADMIN_USER = "admin.user";
    private static final String CONFIG_ADMIN_PASS = "admin.pass";

    /**
     * Update the admin account (oid : 3) with  
     */
    private String resetAdmin( final String username, final String password ) {
        return (String) this.doWithConnection( new Functions.Unary<Object, Connection>(){
            @Override
            public Object call( final Connection connection ) {
                PreparedStatement statement = null;
                try {
                    statement = connection.prepareStatement(
                            "UPDATE internal_user " +
                            "SET password=?, expiration=-1, password_expiry=0, change_password=0 " +
                            "WHERE login = ?"
                    );
                    statement.setString(1, HexUtils.encodePasswd(username, password, "L7SSGDigestRealm"));
                    statement.setString(2, username);
                    int result = statement.executeUpdate();
                    if ( result != 1 ) {
                        return "User account not found '"+username+"'.";
                    }
                } catch ( SQLException se ) {
                    logger.log( Level.WARNING, "Error updating or creating administrative account.", se );
                    return "Unable to update or create administrative account.";
                } finally {
                    ResourceUtils.closeQuietly(statement);
                }

                return null;
            }
        } );
    }
}
