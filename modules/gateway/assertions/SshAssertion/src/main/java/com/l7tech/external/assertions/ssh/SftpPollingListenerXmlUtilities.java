package com.l7tech.external.assertions.ssh;

import com.l7tech.util.Charsets;
import static com.l7tech.util.CollectionUtils.cast;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * SFTP polling listener XML utilities.
 */
public class SftpPollingListenerXmlUtilities {

    private static final Logger logger = Logger.getLogger( SftpPollingListenerXmlUtilities.class.getName() );

    public static String marshallToXMLString(List<SftpPollingListenerDialogSettings> listenerConfigurations) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final XMLEncoder en = new XMLEncoder(os);
        en.writeObject(listenerConfigurations);
        en.close();
        return new String(os.toByteArray(), Charsets.UTF8);
    }

    public static List<SftpPollingListenerDialogSettings> unmarshallFromXMLString(String xml) {
        final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream is;
        try {
            Thread.currentThread().setContextClassLoader( SftpPollingListenerDialogSettings.class.getClassLoader() );
            is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF8));
            XMLDecoder decoder = new XMLDecoder(is);
            List<SftpPollingListenerDialogSettings> listenerConfigurations =
                    cast( decoder.readObject(), List.class, SftpPollingListenerDialogSettings.class, new ArrayList<SftpPollingListenerDialogSettings>() );
            decoder.close();
            return listenerConfigurations;
        } catch ( ArrayIndexOutOfBoundsException e ) {
            logger.fine("Empty configuration for polling listeners");
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextClassLoader);
        }
        return null;
    }
}
