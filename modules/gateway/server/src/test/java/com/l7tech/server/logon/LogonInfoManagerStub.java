package com.l7tech.server.logon;

import com.l7tech.identity.LogonInfo;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;
import java.util.logging.Logger;

/**
 * Stub for the LogonInfoManager
 *
 */
public class LogonInfoManagerStub extends EntityManagerStub<LogonInfo, EntityHeader> implements LogonInfoManager {

    private static final Logger logger = Logger.getLogger(LogonInfoManagerStub.class.getName());

    @Override
    public LogonInfo findByCompositeKey(final Goid providerId, final String login, final boolean lock) throws FindException {
        for(LogonInfo info : entities.values()){
            if(info.getProviderId().equals(providerId) && info.getLogin().equals(login)){
                return info;
            }
        }
        return null;
    }


    @Override
    public void delete(Goid providerId, String login) throws DeleteException {
        try {
            LogonInfo info = findByCompositeKey(providerId,login,false);
            if(info!=null){
                delete(info);
            }
        } catch (FindException e) {
            throw new DeleteException(e.getMessage());
        }

    }
}
