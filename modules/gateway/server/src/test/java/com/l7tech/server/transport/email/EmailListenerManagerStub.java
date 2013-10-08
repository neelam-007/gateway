package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailListenerState;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.EntityManagerStub;
import com.l7tech.util.CollectionUtils;

import java.util.List;
import java.util.logging.Level;

/**
 */
public class EmailListenerManagerStub extends EntityManagerStub<EmailListener, EntityHeader> implements EmailListenerManager{

    public EmailListenerManagerStub( final EmailListener... entitiesIn ) {
        super( entitiesIn );
    }

    @Override
    public List<EmailListener> getEmailListenersForNode(String clusterNodeId) throws FindException {
        return CollectionUtils.toList(entities.values());
    }

    @Override
    public List<EmailListener> stealSubscriptionsFromDeadNodes(String clusterNodeId) {
        return CollectionUtils.toList(entities.values());
    }

    @Override
    public void updateLastPolled(Goid emailListenerGoid) throws UpdateException {
        EmailListener emailListener = entities.get(emailListenerGoid);
        emailListener.getEmailListenerState().setLastMessageId(System.currentTimeMillis());
    }

    @Override
    public void updateState(EmailListenerState state) throws UpdateException {
        Goid emailListenerGoid = state.getEmailListener().getGoid();
        EmailListener emailListener = entities.get(emailListenerGoid);
        if (emailListener != null) {
            EmailListenerState updateState = emailListener.getEmailListenerState();
            updateState.copyTo(state);
        }

    }

    @Override
    public Class<? extends PersistentEntityImp> getImpClass() {
        return EmailListener.class;
    }
}
