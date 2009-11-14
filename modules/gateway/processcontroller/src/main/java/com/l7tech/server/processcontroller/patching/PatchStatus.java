package com.l7tech.server.processcontroller.patching;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.objectmodel.JaxbMapType;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Properties;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import java.text.SimpleDateFormat;

/**
 * Properties that define a patch'es status.
 *
 * @author jbufu
 */
@XmlJavaTypeAdapter(PatchStatus.TypeAdapter.class)
public class PatchStatus {

    public static final String PATCH_STATUS_SUFFIX = ".status";

    public enum Field {
        ID(true) {
            @Override
            public String displayValue(String value) {
                return "Patch ID " + value;
            }},

        DESCRIPTION(true) {
            @Override
            public String displayValue(String value) {
                return " (" + value + ")";
            }},

        STATE(true)  {
            @Override
            public String displayValue(String value) {
                return " is " + value;
            }},

        ROLLBACK_FOR_ID(false) {
            @Override
            public String displayValue(String value) {
                return " (rollback for patch ID " + value + ")";
            }},

        LAST_MOD(false) {
            @Override
            public String displayValue(String value) {
                try {
                    return ", last modified on " + (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date(Long.parseLong(value)));
                } catch (NumberFormatException e) {
                    return ", last modified on " + value;
                }
            }},

        STATUS_MSG(false) {
            @Override
            public String displayValue(String value) {
                return value == null || value.isEmpty() ? "" : ", Status message is: \n" + value + "\n";
            }};


        public abstract String displayValue(String value);
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
            return newPatchStatus(patchId, "", State.NONE);
        } else {
            PatchStatus status = loadPatchStatus(statusFile);
            if(!patchId.equals(status.getField(Field.ID)))
                throw new PatchException("Patch package status file name does not match ID property: " + patchId);
            return status;
        }
    }

    public static PatchStatus newPatchStatus(String patchId, String description, State state) throws PatchException {
        PatchStatus status = new PatchStatus(patchId, description, state);
        status.checkRequiredFields();
        logger.log(Level.FINE, "New status created for patch ID: " + patchId + ", state: " + state);
        return status;
    }

    public static PatchStatus loadPatchStatus(File statusFile) throws PatchException {
        InputStream is = null;
        PatchStatus status = new PatchStatus();
        try {
            is = new FileInputStream(statusFile);
            status.properties.load(is);
            status.checkRequiredFields();
            logger.log(Level.FINE, "Loaded package status for patch ID: " + status.getField(Field.ID));
            return status;
        } catch (IOException e) {
            throw new PatchException("Error reading patch status from: " + (statusFile == null ? null : statusFile.getName()), e);
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
            logger.log(Level.FINE, "Patch status updated for patch ID: " + patchId);
        } catch (IOException e) {
            throw new PatchException("Error saving package status for patch: " + patchId + " : " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    public boolean allowDelete() {
        return State.valueOf(getField(Field.STATE)).allowDelete();
    }

    public String toString(String outputFormat) {
        if (outputFormat == null) {
            return toString();
        } else {
            StringBuilder output = new StringBuilder();
            String[] format = outputFormat.split(FORMAT_DELIMITER);
            for(String token : format) {
                try {
                    output.append(getField(Field.valueOf(token.toUpperCase())));
                } catch (Exception e) {
                    output.append(token);
                }
            }
            return output.toString();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for(Field field : Field.values()) {
            String value = getField(field);
            if (value != null) {
                result.append(field.displayValue(value));
            }
        }
        return result.toString();
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

    private static final Logger logger = Logger.getLogger(PatchStatus.class.getName());

    private Properties properties = new Properties();
    private static final String FORMAT_DELIMITER = ":";

    private PatchStatus() {
    }

    private PatchStatus(String patchId, String description, State state) throws PatchException {
        properties.setProperty(Field.ID.name(), patchId);
        properties.setProperty(Field.DESCRIPTION.name(), description);
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
