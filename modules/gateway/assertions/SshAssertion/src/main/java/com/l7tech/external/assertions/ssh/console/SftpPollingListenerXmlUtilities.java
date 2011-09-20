package com.l7tech.external.assertions.ssh.console;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * SFTP polling listener XML utilities.
 */
public class SftpPollingListenerXmlUtilities {

    public SftpPollingListenerXmlUtilities() {
        super();
    }

    public String marshallToXMLString(List<SftpPollingListenerDialogSettings> listenerConfigurations) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        XMLEncoder en = new XMLEncoder(os);
        en.writeObject(listenerConfigurations);
        en.close();

        String xmlString = null;

        try {
            xmlString = new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }

        return xmlString;
    }

    public ArrayList<SftpPollingListenerDialogSettings> unmarshallFromXMLString(String xml) {
        InputStream is = null;
        try {
            is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            XMLDecoder decoder = new XMLDecoder(is);
            ArrayList<SftpPollingListenerDialogSettings> listenerConfigurations = (ArrayList<SftpPollingListenerDialogSettings>) decoder.readObject();
            decoder.close();
            if(listenerConfigurations!=null)
                return listenerConfigurations;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
