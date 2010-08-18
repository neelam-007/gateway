package com.l7tech.external.assertions.saml2attributequery;

import com.l7tech.util.HexUtils;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 6-Feb-2009
 * Time: 8:49:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamlToLdapMap {
    public static class Entry implements Serializable {
        public String samlName;
        public String nameFormat;
        public String ldapName;

        public Entry(String samlName, String nameFormat, String ldapName) {
            this.samlName = samlName;
            this.nameFormat = nameFormat;
            this.ldapName = ldapName;
        }
    }

    public static final String PREFIX = "samlToLdap.attribute.map";

    private List<Entry> entries;

    public SamlToLdapMap() {
        entries = new ArrayList<Entry>();
    }

    public SamlToLdapMap(String data) throws IOException {
        byte[] bytes = HexUtils.decodeBase64(data);

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            entries = (List<Entry>)ois.readObject();
        } catch(ClassNotFoundException cnfe) {
            throw new IOException();
        }
    }

    public Object[][] asObjectArray() {
        Object[][] retval = new Object[entries.size()][3];

        for(int i = 0;i < entries.size();i++) {
            retval[i][0] = entries.get(i).samlName;
            retval[i][1] = entries.get(i).nameFormat;
            retval[i][2] = entries.get(i).ldapName;
        }

        return retval;
    }

    public String asString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(entries);
            oos.close();

            return HexUtils.encodeBase64(baos.toByteArray());
        } catch(IOException ioe) {
            return null;
        }
    }

    public String getSamlName(String ldapName) {
        for(Entry entry : entries) {
            if(ldapName.equals(entry.ldapName)) {
                return entry.samlName;
            }
        }

        return null;
    }

    public String getNameFormatFromSamlName(String samlName) {
        for(Entry entry : entries) {
            if(samlName.equals(entry.samlName)) {
                return entry.nameFormat;
            }
        }

        return null;
    }

    public String getNameFormatFromLdapName(String ldapName) {
        for(Entry entry : entries) {
            if(ldapName.equals(entry.ldapName)) {
                return entry.nameFormat;
            }
        }

        return null;
    }

    public String getLdapName(String samlName) {
        for(Entry entry : entries) {
            if(samlName.equals(entry.samlName)) {
                return entry.ldapName;
            }
        }

        return null;
    }

    public boolean containsSamlName(String samlName) {
        for(Entry entry : entries) {
            if(samlName.equals(entry.samlName)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsLdapName(String ldapName) {
        for(Entry entry : entries) {
            if(ldapName.equals(entry.ldapName)) {
                return true;
            }
        }

        return false;
    }

    public void add(String samlName, String nameFormat, String ldapName) {
        entries.add(new Entry(samlName, nameFormat, ldapName));
    }
}
