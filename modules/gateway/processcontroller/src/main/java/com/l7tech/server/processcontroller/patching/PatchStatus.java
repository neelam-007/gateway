package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.objectmodel.JaxbMapType;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Properties;
import java.io.*;

/**
 * Properties that define a patch'es status.
 *
 * @author jbufu
 */
@XmlJavaTypeAdapter(PatchStatus.TypeAdapter.class)
public class PatchStatus {

    public static final String PATCH_STATUS_SUFFIX = ".status";

    public enum Field {
        ID(true),
        STATE(true),
        ERROR_MSG(false),
        LAST_MOD(false),
        ROLLBACK_FOR_ID(false);

        Field(boolean required) {
            this.required = required;
        }

        public boolean isRequired() {
            return required;
        }

        private boolean required;
    }

    public enum State {
        NONE(true),
        UPLOADED(true),
        INSTALLED(false),
        ROLLED_BACK(true),
        ERROR(false);

        State(boolean allowDelete) {
            this.allowDelete = allowDelete;
        }

        public boolean allowDelete() {
            return allowDelete;
        }

        private final boolean allowDelete;
    }

    public static PatchStatus getPackageStatus(File repository, String patchId) throws PatchException {
        File statusFile = getStatusFile(repository, patchId);
        if (!statusFile.exists()) {
            return newPatchStatus(patchId, State.NONE);
        } else {
            PatchStatus status = loadPatchStatus(statusFile);
            if(!patchId.equals(status.getField(Field.ID)))
                throw new PatchException("Patch package status file name does not match ID property: " + patchId);
            return status;
        }
    }

    public static PatchStatus newPatchStatus(String patchId, State state) throws PatchException {
        PatchStatus status = new PatchStatus(patchId, state);
        status.checkRequiredFields();
        return status;
    }

    public static PatchStatus loadPatchStatus(File statusFile) throws PatchException {
        InputStream is = null;
        PatchStatus status = new PatchStatus();
        try {
            is = new FileInputStream(statusFile);
            status.properties.load(is);
            status.checkRequiredFields();
            return status;
        } catch (IOException e) {
            throw new PatchException("Error reading patch status from: " + statusFile.getName(), e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    public String getField(Field field) {
        return properties.getProperty(field.name());
    }

    public void setField(Field field, String value) {
        properties.setProperty(field.name(), value);
    }

    public void save(File repository) throws PatchException {
        String patchId = properties.getProperty(Field.ID.name());
        setField(Field.LAST_MOD, Long.toString(System.currentTimeMillis()));
        OutputStream os = null;
        try {
            os = new FileOutputStream(getStatusFile(repository, patchId));
            properties.store(os, null);
        } catch (IOException e) {
            throw new PatchException("Error saving package status for patch: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    public boolean allowDelete() {
        return State.valueOf(getField(Field.STATE)).allowDelete();
    }

    public static class TypeAdapter extends XmlAdapter<JaxbMapType, PatchStatus> {

        @Override
        public PatchStatus unmarshal(JaxbMapType v) throws Exception {
            PatchStatus status = new PatchStatus();
            status.properties.putAll(v.toMap());
            return status;
        }

        @Override
        public JaxbMapType marshal(PatchStatus v) throws Exception {
            return new JaxbMapType(v.properties);
        }
    }

    // - PRIVATE

    private Properties properties = new Properties();

    private PatchStatus() {
    }

    private PatchStatus(String patchId, State state) throws PatchException {
        properties.setProperty(Field.ID.name(), patchId);
        properties.setProperty(Field.STATE.name(), state.name());
    }

    private static File getStatusFile(File repository, String patchId) {
        return new File(repository, patchId + PATCH_STATUS_SUFFIX);
    }

    private void checkRequiredFields() throws PatchException {
        for(Field field : Field.values()) {
            if (field.isRequired() && ( !properties.containsKey(field.name()) || properties.getProperty(field.name()) == null))
                throw new PatchException("Required patch status field not found: " + field);
        }
    }
}
