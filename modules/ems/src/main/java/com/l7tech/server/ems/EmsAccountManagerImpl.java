package com.l7tech.server.ems;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.User;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EmsAccountManagerImpl implements EmsAccountManager {

    //- PUBLIC

    public EmsAccountManagerImpl( final IdentityProviderFactory identityProviderFactory ) {
        this.identityProviderFactory = identityProviderFactory;            
    }

    public int getUserCount() throws FindException {
        return getUserManager().findAllHeaders().size();
    }

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

    public InternalUser findByLogin( final String login ) throws FindException {
        return getUserManager().findByLogin(login);
    }

    public void update( final InternalUser user ) throws UpdateException {
        try {
            getUserManager().update( user );
        } catch ( FindException fe ) {
            throw new UpdateException( fe );
        }
    }

    public String save( final InternalUser user ) throws SaveException {
        return getUserManager().save( user, Collections.<IdentityHeader>emptySet() );
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
