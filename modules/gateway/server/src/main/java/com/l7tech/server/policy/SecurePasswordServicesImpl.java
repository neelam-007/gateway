package com.l7tech.server.policy;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.ext.password.SecurePasswordServices;
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
    public String decryptPassword(String id) throws ServiceException {
        try {
            SecurePassword securePassword = securePasswordManager.findByPrimaryKey(new Goid(id));
            if (securePassword == null) {
                throw new ServiceException("Password with ID " + id + " not found.");
            } else {
                return new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
            }
        } catch (FindException | ParseException e) {
            throw new ServiceException(e);
        }
    }
}