package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.Arrays;

/**
 * Represents raw keystore data stored in a row of the "keystore" table.
 * <p/>
 * This is a low-level class used only for holding keystore bytes as they are copied to and from the database.
 * <p/>
 * The movement in and out of the database is managed by an implementation of {@link SsgKeyStore} such
 * as {@link com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore} or
 * {@link com.l7tech.server.security.keystore.sca.ScaSsgKeyStore}.
 */
public class KeystoreFile extends NamedEntityImp {
    private static final long serialVersionUID = 7293792837442132345L;

    private String format;
    private byte[] databytes;


    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public byte[] getDatabytes() {
        return databytes;
    }

    public void setDatabytes(byte[] databytes) {
        this.databytes = databytes;
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        KeystoreFile that = (KeystoreFile)o;

        if (!Arrays.equals(databytes, that.databytes)) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (databytes != null ? Arrays.hashCode(databytes) : 0);
        return result;
    }
}
