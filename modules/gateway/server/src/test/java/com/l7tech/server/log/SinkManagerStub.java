package com.l7tech.server.log;

import com.l7tech.gateway.common.log.LogFileInfo;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.gateway.common.log.LogSinkQuery;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;
import java.util.Collection;

/**
 * @author luiwy01, 2017-09-29
 */
public class SinkManagerStub extends EntityManagerStub<SinkConfiguration, EntityHeader> implements SinkManager {

  public SinkManagerStub(final SinkConfiguration... entitiesIn) {
    super(entitiesIn);
  }

  public SinkManagerStub() {
    super();
  }

  @Override
  public long getMaximumFileStorageSpace() {
    return 0;
  }

  @Override
  public long getReservedFileStorageSpace() {
    return 0;
  }

  @Override
  public boolean test(SinkConfiguration sinkConfiguration, String testMessage) {
    return false;
  }

  @Override
  public MessageSink getPublishingSink() {
    return null;
  }

  @Override
  public Collection<LogFileInfo> findAllFilesForSinkByNode(String nodeId, Goid sinkId) throws FindException {
    return null;
  }

  @Override
  public LogSinkData getSinkLogs(String nodeId, Goid sinkId, String file, LogSinkQuery query) throws FindException {
    return null;
  }
}

