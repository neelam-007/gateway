package com.l7tech.console.jnlp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;


import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.security.Security;
import javax.jnlp.*;

import com.l7tech.console.util.Preferences;

/**
 * The <code>JNLPPreferencs</code> class extends the default
 * PMC property manager <code>Preferencse</code>.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class JNLPPreferences extends Preferences {


  /** default constructor */
  public JNLPPreferences() {
  }

  /**
   * store the preferences.
   * This stores the preferences in the user home directory.
   * 
   * @exception IOException
   *                   thrown if an io error occurred
   */
  public void store() throws IOException {
    try {
      PersistenceStorage pstor = 
        Services.getInstance().getPersistenceStorage();
      pstor.write(STORE, props);
    } catch (UnavailableServiceException e) {
      throw new IOException(e.getMessage());
    }
  }

  /**
   * initialize the properties from the user properties ifrom JNLP
   * properties.
   * Note that this method is protected since it is invoked from
   * com.l7tech.console.util.Preferences
   * 
   * @exception IOException
   *                   thrown if an io error occurred
   * @see com.l7tech.console.util.Preferences
   */
  protected void initialize() throws IOException {
    try {
      PersistenceStorage pstor = 
        Services.getInstance().getPersistenceStorage();
      // warning!!! NPE thrown if empty store
      Object store = null;
      try {
        store = pstor.read(STORE);
        if (store == null) return;
      } catch(FileNotFoundException e) {
        return;
      } catch(NullPointerException e) {
        return;  // thrown when store not created (ugly)
      }
      // verify no junk, if yes delete
      if (!(store instanceof Properties)) {
        pstor.removeEntry(pstor.getUrl(STORE));
        return;
      }
      Properties storeProps = (Properties)store;
      props.putAll(storeProps);
    } catch (ClassNotFoundException e) {
      throw new IOException(e.getMessage());
    } catch (UnavailableServiceException e) {
      throw new IOException(e.getMessage());
    }
  }
}
