package com.l7tech.server.folder;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.server.EntityCacheSupport;
import com.l7tech.util.Functions.Unary;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.reverse;

/**
 * Cache implementation for Folder entities
 */
public class FolderCacheImpl extends EntityCacheSupport<Folder, FolderHeader,FolderManager> implements FolderCache {

    //- PUBLIC

    public FolderCacheImpl( final FolderManager entityManager ) {
        super( entityManager );
    }

    /**
     * Get the path to the given folder.
     *
     * <p>The returned path from the root to the identified folder.</p>
     *
     * @param goid The identifier for the target folder
     * @return The path or an empty list if the folder was not found
     */
    @Override
    public List<Folder> findPathByPrimaryKey( final Goid goid ) {
        return doWithCacheReadOnly( new Unary<List<Folder>,Map<Goid,Folder>>(){
            @Override
            public List<Folder> call( final Map<Goid, Folder> cache ) {
                final List<Folder> path = new ArrayList<Folder>();

                Folder current = cache.get( goid );
                while ( current != null ) {
                    path.add( current );
                    if ( current.getFolder() != null ) {
                        current = cache.get( current.getFolder().getGoid() );
                    } else {
                        current = null;
                    }
                }

                reverse( path );
                return path;
            }
        } );
    }

    //- PROTECTED

    @Override
    protected Class<Folder> getEntityClass() {
        return Folder.class;
    }

    @Override
    protected Folder readOnly( final Folder entity ) {
        return new Folder( entity, true );
    }

    @PostConstruct
    protected void init() {
        initializeCache();
    }
}
