package com.l7tech.console.jnlp;

import javax.jnlp.BasicService;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

/**
 * Utility class for locating JNLP services.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class Services {

  /** hidden constructor, the class cannot be instantiated */
  private Services() {
  }

  /**
   * 
   * @return the sigleton instance 
   */
  public static Services getInstance() {
    return SingletonHolder.instance;
  }

  /**
   * singleton holder instance class.
   */
  private static class SingletonHolder {
    static final
      Services instance = new Services();
  }

  /**
   * Locate the JNLP BasicService
   * 
   * @return the service requested
   * @exception UnavailableServiceException
   *                   on error locating the service
   */
  public BasicService getBasicService() 
   throws UnavailableServiceException  {
    return (BasicService)getService("javax.jnlp.BasicService");
  }

  /**
   * JNLP service locator
   * 
   * @param name   service name
   * @return the service requested
   * @exception UnavailableServiceException
   *                   on error locating the service
   */
  public Object getService(String name) 
   throws UnavailableServiceException {
     return ServiceManager.lookup(name);
  }

  /**
   * @return the instance of PersistenceStorage wrapping the
   *         JNLP bPersistenceService
   */
  public PersistenceStorage getPersistenceStorage() 
  throws UnavailableServiceException {
    return new 
      PersistenceStorage(getBasicService(),
        (PersistenceService)getService("javax.jnlp.PersistenceService"));

  }
}



