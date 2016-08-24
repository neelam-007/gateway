package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.objectmodel.Goid;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 21/01/13
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RemoteCachesManager {
    void invalidateRemoteCache(Goid goid);

    RemoteCache getRemoteCache(Goid cacheGoid) throws RemoteCacheConnectionException;
}
