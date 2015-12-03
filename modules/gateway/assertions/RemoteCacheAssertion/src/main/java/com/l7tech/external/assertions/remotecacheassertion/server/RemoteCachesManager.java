package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.objectmodel.Goid;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 21/01/13
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RemoteCachesManager {

    public void connectionAdded(RemoteCacheEntity remoteCacheEntity);

    public void connectionUpdated(RemoteCacheEntity remoteCacheEntity);

    public void connectionRemoved(RemoteCacheEntity remoteCacheEntity);

    public RemoteCache getRemoteCache(Goid cacheGoid) throws RemoteCacheConnectionException;
}
