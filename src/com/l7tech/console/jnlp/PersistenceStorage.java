package com.l7tech.console.jnlp;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;


import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.FileContents;
import javax.jnlp.PersistenceService;


/**
 * The <code>PersistenceStorage</code> is a wrapper arround
 * the JNLP PersistenceService.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PersistenceStorage {

  private final PersistenceService persistenceService;
  private final BasicService basicService;

  /**
   * the max value size set to 2K
   */
  private final long MAX_SIZE =2048L;

  /**
   * default constructor
   * instantiate the PersistenceStorage with the JNLP BasicService
   * and PersistenceService
   * 
   * @param basicService
   * @param persistenceService
   */
  public PersistenceStorage(BasicService basicService,
                            PersistenceService persistenceService) {
    this.basicService = basicService;
    this.persistenceService = persistenceService;
  }

  /**
   * Saves the given object at the given key location.
   * The default object max size is MAX_SIZE.
   * 
   * @param keyString the given key
   * @param value     the key value
   * @exception IOException
   *                   thrown if an I/O error occurs
   */
  public void write(String keyString,Object value) throws IOException {
    write(keyString,value,MAX_SIZE);
  }

  /**
   * Saves the given object at the given key location soecifying the
   * max size.
   * 
   * @param keyString the given key
   * @param value     the key value
   * @param maxLength the max size of the value
   */
  public void write(String keyString,Object value,long maxLength) 
  throws IOException {
    write(getUrl(keyString), value, maxLength);
  }

  /**
   * Saves the given object at the given key location.
   * 
   * Note that if the URL is not based on the codebase
   * value (that is, it is not rooted on the Web server
   * that deployed the application), the service won't
   * work.
   * 
   * @param url       the codebase for the service
   * @param value     the value to save
   * @param maxLength the max length of the value
   * @exception IOException
   *                   thrown if an I/O error occurs
   */
  public void write(URL url,Object value,long maxLength) 
  throws IOException {
    if (exists(url)) {
      removeEntry(url);
    }
    persistenceService.create(url,maxLength);
    FileContents fc = persistenceService.get(url);
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(fc.getOutputStream(false));
      oos.writeObject(value);
    } finally {
      if (oos !=null) {
        try {
          oos.close();
        } catch (IOException e) {
          ;
        }
      }
    }
  }

  /**
   * @param keyString the key
   * @return the object for a given key.
   * @exception IOException
   *                   thrown if an I/O error occurs
   * @exception ClassNotFoundException
   *                   if the Class of a serialized object
   *                   cannot be found
   */
  public Object read(String keyString) 
  throws IOException, ClassNotFoundException {
    return read(getUrl(keyString));
  }

  /**
   * 
   * @param url    the URL
   * @return the object for a given URL.
   * @exception IOException
   *                   if an I/O error occurred
   * @exception ClassNotFoundException
   *                   if the Class of a serialized object
   *                   cannot be found
   */
  public Object read(URL url) 
  throws IOException, ClassNotFoundException {
    if (!exists(url))
      return null;
    Object object = null;
    ObjectInputStream ois = null;
    try {
      FileContents fc = persistenceService.get(url);
      ois = new ObjectInputStream(fc.getInputStream());
      return ois.readObject();
    } finally {
      if (ois !=null) {
        try {
          ois.close();
        } catch (IOException e) {
          ;
        }
      }
    }
  }

  /**
   * Returns true if there are some data cached with
   * the given URL as a key.
   * 
   * @param url    the URL to verify if it does exist
   * @return true if exists, false otherwise
   * @exception MalformedURLException
   *                   thrown if an URL error occured
   * @exception IOException
   *                   if an I/O error occurred
   */
  public boolean exists(URL url) throws MalformedURLException, IOException {
    try {
      return persistenceService.getNames(url).length > 0 ; 
    } catch(NullPointerException e) { // ugly, thrown when store empty
      return false;
    }
  }

  /**
   * Tags the persistent data store entry associated with the given
   * key as cached.
   * 
   * @param url    the URL representing the persistent data store entry
   *               for which to set the tag value.
   * @exception MalformedURLException
   *                   thrown if an URL error occured
   * @exception IOException
   *                   if an I/O error occurred
   */
  public void synchronize(String keyString) 
  throws MalformedURLException, IOException {
    synchronize(getUrl(keyString));
  }

  /**
   * Tags the persistent data store entry associated with the given
   * URL as cached.
   * 
   * @param url    the URL representing the persistent data store entry
   *               for which to set the tag value.
   * @exception MalformedURLException
   *                   thrown if an URL error occured
   * @exception IOException
   *                   if an I/O error occurred
   */
  public void synchronize(URL url) 
  throws MalformedURLException, IOException   {
    if (persistenceService.getTag(url)==PersistenceService.DIRTY) {
      persistenceService.setTag(url, persistenceService.CACHED);
    }
  }

  /**
   * delete the given URL from the persistent storage
   * 
   * @param url    the URL representing the entry to delete
   *               from the persistent data store.
   * @exception MalformedURLException
   *                   if an URL error occured.
   * @exception IOException
   *                   if an I/O error occurs
   */
  public void removeEntry(URL url) 
  throws MalformedURLException, IOException {
    persistenceService.delete(url);
  }

  /**
   * Returns the a List with all the entries stored at
   * the given key in the codebase URL.
   * 
   * @param keyString the URL to examine
   * @return the List of entries or empty if no entries exist.
   * @exception MalformedURLException
   *                   if an URL error occured.
   * @exception IOException
   *                   if an I/O error occurred
   */
  public List getEntries(String keyString) 
  throws MalformedURLException, IOException {
    return getEntries(getUrl(keyString));
  }

  /**
   * Returns the a List with all the entries stored at
   * the given URL.
   * 
   * @param url    the URL to list
   * @return the List of entries or empty if no entries exist.
   * @exception MalformedURLException
   *                   if an URL error occured.
   * @exception IOException
   *                   if an I/O error occurred
   */
  public List getEntries(URL url) 
  throws MalformedURLException, IOException {
    String[] names = {};
    String[] urlNames = persistenceService.getNames(url);
    if (urlNames !=null) {
      names = urlNames; 
    }
    return 
      Collections.
      unmodifiableList(Arrays.asList(names));
  }

  /** @return the wrapped persistence service */
  public PersistenceService getPersistenceService() {
    return persistenceService;
  }

  /** @return the BasicService associated (this app codebase) */
  public BasicService getBasicService() {
    return basicService;
  }

  public void touchStore(String touchName) {
    try {
      URL codebase = basicService.getCodeBase();
      URL url = new URL(codebase, touchName);
      persistenceService.create(url,MAX_SIZE);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * translate the key strings in URLs using the BasicService
   * codebase.
   * 
   * @param keyString to generate the URL for.
   * @return the URL for the key
   * @exception MalformedURLException
   *                   thrown if an URL error has occured
   */
  public URL getUrl(String keyString) 
  throws MalformedURLException {
    URL codebase = basicService.getCodeBase();
    return new URL(codebase,keyString);
  }
}
