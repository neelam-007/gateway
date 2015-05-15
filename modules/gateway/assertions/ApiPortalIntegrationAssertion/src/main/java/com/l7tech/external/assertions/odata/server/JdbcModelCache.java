package com.l7tech.external.assertions.odata.server;

import com.l7tech.external.assertions.odata.ODataProducerAssertion;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel;
import com.l7tech.util.Background;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A cache for JdbcModel
 *
 * @author rraquepo, 1/15/14
 */
public class JdbcModelCache {
  private Map<String, JdbcModel> cache = new ConcurrentHashMap<>();

  private static final Logger logger = Logger.getLogger(JdbcModelCache.class.getName());

  private static JdbcModelCache instance;

  private Config config;

  public static JdbcModelCache getInstance() {
    if (instance == null) {
      instance = new JdbcModelCache();
    }
    return instance;
  }

  public JdbcModelCache() {
    config = ConfigFactory.getCachedConfig();
    scheduleCleanup();
  }

  public Map<String, JdbcModel> getCache() {
    return cache;
  }

  public JdbcModel get(String id) {
    return cache.get(id);
  }

  public void put(String id, JdbcModel model) {
    cache.put(id, model);
  }

  //exposed as protected only for testing purposes
  protected void scheduleCleanup() {
    // Clear cache daily to prevent it from growing without bound
    long wipeTime = config.getLongProperty(ODataProducerAssertion.PARAM_CACHE_WIPE_INTERVAL, ODataProducerAssertion.CACHE_WIPE_INTERVAL_DEFAULT);
    Background.scheduleRepeated(new TimerTask() {
      @Override
      public void run() {
        int beforeCount = cache.size();
        logger.log(Level.INFO, "Clearing JdbcModel {0}", new String[]{String.valueOf(beforeCount)});
        cache.clear();
      }
    }, wipeTime, wipeTime);
  }

  //exposed as protected only for testing purposes
  protected void setCache(Map<String, JdbcModel> cache) {
    this.cache = cache;
  }

  protected void setConfig(final Config config) {
    this.config = config;
  }
}
