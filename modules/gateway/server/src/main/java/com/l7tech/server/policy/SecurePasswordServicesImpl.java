package com.l7tech.server.policy;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ext.SecurePasswordServices;
import com.l7tech.policy.assertion.ext.ServiceException;
import com.l7tech.server.security.password.SecurePasswordManager;

import java.text.ParseException;

/**
 * Implementation of SecurePasswordServices interface.
 */
public class SecurePasswordServicesImpl implements SecurePasswordServices {

    private final SecurePasswordManager securePasswordManager;

    public SecurePasswordServicesImpl (SecurePasswordManager securePasswordManager) {
        this.securePasswordManager = securePasswordManager;
    }

    @Override
    public String decryptPassword(long oid) throws ServiceException {
        try {
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(oid);
            if (securePassword == null) {
                throw new ServiceException("Password with OID " + oid + " not found.");
            } else {
                return new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
            }
        } catch (FindException e) {
            throw new ServiceException(e);
        } catch (ParseException e) {
            throw new ServiceException(e);
        }
    }
}