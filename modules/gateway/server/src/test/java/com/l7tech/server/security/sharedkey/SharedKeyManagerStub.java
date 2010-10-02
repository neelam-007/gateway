package com.l7tech.server.security.sharedkey;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
*
*/
public class SharedKeyManagerStub extends SharedKeyManagerImpl {
    private String passphrase;
    private SharedKeyRecord saved;

    public SharedKeyManagerStub(char[] clusterPassphrase) {
        super(clusterPassphrase);
        this.passphrase = new String(clusterPassphrase);
    }

    public SharedKeyManagerStub(SharedKeyManagerStub template) {
        super(template.passphrase.toCharArray());
        copyFrom(template);
    }

    protected void saveSharedKeyRecord(SharedKeyRecord sharedKeyToSave) throws DataAccessException {
        if (saved != null)
            throw new ConstraintViolationException("a row with primary key value " + sharedKeyToSave.getEncodingID() + " already exists", null, null);
        saved = sharedKeyToSave;
    }

    protected Collection<SharedKeyRecord> selectSharedKeyRecordsByEncodingId() {
        return saved == null ? Collections.<SharedKeyRecord>emptyList() : Arrays.asList(saved);
    }

    public void copyFrom(SharedKeyManagerStub from) {
        this.passphrase = from.passphrase;
        this.saved = new SharedKeyRecord();
        this.saved.setEncodingID(from.saved.getEncodingID());
        this.saved.setB64edKey(from.saved.getB64edKey());
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public SharedKeyRecord getSaved() {
        return saved;
    }

    public void setSaved(SharedKeyRecord saved) {
        this.saved = saved;
    }
}
