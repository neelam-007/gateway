package com.l7tech.server.ems.user;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.User;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.ExceptionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EsmAccountManagerImpl implements EsmAccountManager {

    //- PUBLIC

    public EsmAccountManagerImpl( final IdentityProviderFactory identityProviderFactory ) {
        this.identityProviderFactory = identityProviderFactory;            
    }

    @Override
    public int getUserCount() throws FindException {
        return getUserManager().findAllHeaders().size();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Collection<InternalUser> getUserPage( final int startIndex,
                                                 final int count,
                                                 final String property,
                                                 final boolean ascending ) throws FindException {
        InternalUserManager manager = getUserManager();
        Collection<IdentityHeader> headers = manager.findAllHeaders();
        List<InternalUser> users = new ArrayList<InternalUser>(headers.size());

        for ( IdentityHeader header : headers ) {
            users.add( manager.findByPrimaryKey(header.getStrId()) );
        }
        Collections.sort( users, new ResolvingComparator( new Resolver(){
            @Override
            public Object resolve( Object key ) {
                User user = (User) key;
                String value = null;

                if ( "login".equals( property ) ) {
                    value = user.getLogin();
                } else if ( "firstName".equals( property ) ) {
                    value = user.getFirstName();
                } else if ( "lastName".equals( property ) ) {
                    value = user.getLastName();
                }

                if ( value == null ) {
                    value = "";
                }

                return value;
            }
        }, !ascending ));

        List<InternalUser> result = new ArrayList<InternalUser>(count);
        int start = 0;
        for ( InternalUser user : users ) {
            if ( start++ >= startIndex ) {
                if (result.size() >= count) break;
                result.add( user );
            }
        }

        return result;
    }

    @Override
    public InternalUser findByLogin( final String login ) throws FindException {
        return getUserManager().findByLogin(login);
    }

    @Override
    public InternalUser findByPrimaryKey(final String identifier) throws FindException {
        return getUserManager().findByPrimaryKey(identifier);
    }

    @Override
    public void update( final InternalUser user ) throws UpdateException {
        try {
            getUserManager().update( user );
        } catch ( FindException fe ) {
            throw new UpdateException( fe );
        }
    }

    @Override
    public String save( final InternalUser user ) throws SaveException {
        return getUserManager().save( user, Collections.<IdentityHeader>emptySet() );
    }

    @Override
    public void delete( final String login ) throws DeleteException {
        try {
            InternalUser user = getUserManager().findByLogin(login);
            if ( user != null ) {
                getUserManager().delete( user );
            }
        } catch ( FindException fe ) {
            throw new DeleteException( ExceptionUtils.getMessage(fe), fe );
        }
    }

    //- PRIVATE

    private final IdentityProviderFactory identityProviderFactory;

    private InternalUserManager getUserManager() {
        try {
            InternalIdentityProvider provider = (InternalIdentityProvider) identityProviderFactory.findAllIdentityProviders().iterator().next();
            return provider.getUserManager();
        } catch ( FindException fe ) {
            throw new RuntimeException( "Error accessing user manager", fe );
        }
    }
}
