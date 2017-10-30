package com.l7tech.external.assertions.portaldeployer.server.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An SSL SocketFactory Provider based on local files
 *
 * @author rraquepo, 7/25/13
 */
public class SSLSocketFactoryProviderFileImpl extends SSLSocketFactoryProviderImpl {
  public SSLSocketFactoryProviderFileImpl(String keystoreFilename, String keystorePassword, String privateKeyFile, String privteKeyPassword) throws SSLFactoryException {
    if (keystoreFilename == null) {
      throw new SSLFactoryException("keystoreFilename cannot be null");
    }
    if (privateKeyFile == null) {
      throw new SSLFactoryException("privateKeyFile cannot be null");
    }
    InputStream stream1 = null;
    try {
      stream1 = new FileInputStream(keystoreFilename);
      if (stream1 != null) {
        super.init(stream1, keystorePassword);
      }
    } catch (FileNotFoundException e) {
      throw new SSLFactoryException(e.getMessage());
    } finally {
      closeStream(stream1);
    }
    InputStream stream2 = null;
    try {
      stream2 = new FileInputStream(privateKeyFile);
      if (stream2 != null) {
        super.addPrivateKey("default", stream2, privteKeyPassword);
      }
    } catch (FileNotFoundException e) {
      throw new SSLFactoryException(e.getMessage());
    } finally {
      closeStream(stream2);
    }
  }

  private void closeStream(InputStream stream) {
    //cleanup
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
