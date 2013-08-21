package com.l7tech.server.ems.user;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation for User Properties Management.
 */
@Transactional(propagation=Propagation.REQUIRED)
public class UserPropertyManagerImpl
        extends HibernateEntityManager<UserProperty, EntityHeader>
        implements UserPropertyManager {

    @Override
    public Class<UserProperty> getImpClass() {
        return UserProperty.class;
    }

    @Override
    public Class<UserProperty> getInterfaceClass() {
        return UserProperty.class;
    }

    @Override
    public String getTableName() {
        return "user_property";
    }

    @Override
    @SuppressWarnings({"unchecked"})
    @Transactional(readOnly=true)
    public Map<String, String> getUserProperties( final User user ) throws FindException {
        try {
            return (Map<String, String>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                @SuppressWarnings({"unchecked"})
                public Object doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                    // Prevent reentrant ClusterProperty lookups from flushing in-progress writes
                    Criteria criteria = session.createCriteria(UserProperty.class);
                    criteria.add(Restrictions.eq( "provider", user.getProviderId() ) );
                    criteria.add(Restrictions.eq( "userId", user.getId() ) );
                    List<UserProperty> props = criteria.list();

                    Map<String,String> properties = new HashMap<String,String>();
                    for ( UserProperty property : props ) {
                        properties.put( property.getName(), property.getValue() );
                    }

                    return properties;
                }
            });
        } catch ( HibernateException he ) {
            throw new FindException( "Error loading user properties.", he );
        }
    }

    @Override
    public void saveUserProperties( final User user, final Map<String, String> properties ) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                @SuppressWarnings({"unchecked"})
                public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(UserProperty.class);
                    criteria.add(Restrictions.eq( "provider", user.getProviderId() ) );
                    criteria.add(Restrictions.eq( "userId", user.getId() ) );
                    List<UserProperty> props = criteria.list();

                    // Find and update existing properties, delete removed props
                    Map<String,String> workingProperties = new HashMap<String,String>(properties);
                    for ( UserProperty property : props ) {
                        if ( workingProperties.containsKey( property.getName() ) ) {
                            property.setValue( workingProperties.get( property.getName() ) );
                            workingProperties.remove( property.getName() );
                            session.update( property );
                        } else {
                            session.delete( property );                            
                        }
                    }

                    // Save any new User Properties
                    for ( Map.Entry<String,String> propEntry : workingProperties.entrySet() ) {
                        UserProperty userProperty = new UserProperty();
                        userProperty.setProvider( user.getProviderId() );
                        userProperty.setUserId( user.getId() );
                        userProperty.setLogin( user.getLogin() );
                        userProperty.setName( propEntry.getKey() );
                        userProperty.setValue( propEntry.getValue() );                        
                        session.save( userProperty );
                    }

                    return null;
                }
            });
        } catch ( HibernateException he ) {
            throw new UpdateException( "Error saving user properties.", he );
        }
    }

}
