package com.l7tech.external.assertions.oauthinstaller;

import java.io.Serializable;

/**
 * BundleInfo extracted from BundleInfo.xml inside a bundle folder.
 *
 * Each time the XML which makes up a bundle component is changed the version number should be bumped.
 */
public final class BundleInfo implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BundleInfo that = (BundleInfo) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    // - PRIVATE

    private final String id;
    private final String version;
    private final String name;
    private final String description;

}
