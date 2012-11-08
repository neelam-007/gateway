package com.l7tech.server.upgrade;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.ObjectModelException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Upgrade task that modifies all existing federated users in database to canonicalize their Subject DN (Bug #10610).
 */
public class Upgrade62To70CanonicalizeFedUserSubjectDNs implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade62To70CanonicalizeFedUserSubjectDNs.class.getName());

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        SessionFactory sessionFactory = applicationContext.getBean("sessionFactory", SessionFactory.class);
        try {
            canonicalizeFedUserSubjectDns(sessionFactory);
        } catch (ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback and log, but continue boot, and try again another day
        }
    }

    private void canonicalizeFedUserSubjectDns(SessionFactory sessionFactory) throws ObjectModelException {
        logger.info("Canonicalizing all federated user subject DNs");

        new HibernateTemplate(sessionFactory).execute( new HibernateCallback<Void>(){
            @Override
            public Void doInHibernate( final Session session ) throws HibernateException, SQLException {
                Criteria fedUserCriteria = session.createCriteria( FederatedUser.class );
                final List fedUsers = fedUserCriteria.list();
                logger.info("Processing " + fedUsers.size() + " federated users");
                for ( Object fedUserObj : fedUsers) {
                    if ( fedUserObj instanceof FederatedUser ) {
                        FederatedUser fedUser = (FederatedUser) fedUserObj;
                        fedUser.setSubjectDn(CertUtils.formatDN(fedUser.getSubjectDn()));
                    }
                }

                return null;
            }
        } );
    }
}
