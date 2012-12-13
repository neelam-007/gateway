package com.l7tech.policy.bundle;

import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * BundleInfo extracted from BundleInfo.xml inside a bundle folder.
 *
 * Each time the XML which makes up a bundle component is changed the version number should be bumped.
 */
public class BundleInfo implements Serializable {

    public BundleInfo(String id, String version, String name, String description) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void addJdbcReference(String connectionName) {
        jdbcConnectionReferences.add(connectionName);
    }

    public Set<String> getJdbcConnectionReferences() {
        return Collections.unmodifiableSet(jdbcConnectionReferences);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BundleInfo)) return false;

        BundleInfo that = (BundleInfo) o;

        if (!description.equals(that.description)) return false;
        if (!id.equals(that.id)) return false;
        if (!jdbcConnectionReferences.equals(that.jdbcConnectionReferences)) return false;
        if (!name.equals(that.name)) return false;
        if (!version.equals(that.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + jdbcConnectionReferences.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BundleInfo{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", jdbcConnectionReferences=" + jdbcConnectionReferences +
                '}';
    }

    /**
     * Validate an installation bundle prefix.
     *
     * @param prefix installation prefix to validate
     * @return null if prefix is ok to use otherwise an error message.
     */
    @Nullable
    public static String getPrefixedUrlErrorMsg(String prefix){

        // validate for XML chars
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">"};
        for (String invalidChar : invalidChars) {
            if (prefix.contains(invalidChar)) {
                return "Invalid character '" + invalidChar + "' is not allowed in the installation prefix.";
            }
        }

        String testUri = "http://ssg.com:8080/" + prefix + "/query";
        if (!ValidationUtils.isValidUrl(testUri)) {
            return "Invalid prefix '" + prefix + "'. It must be possible to construct a valid routing URI using the prefix.";
        }

        return null;
    }


    // - PRIVATE

    private final String id;
    private final String version;
    private final String name;
    private final String description;
    private final Set<String> jdbcConnectionReferences = new HashSet<String>();

}
