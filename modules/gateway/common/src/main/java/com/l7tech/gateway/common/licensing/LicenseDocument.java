package com.l7tech.gateway.common.licensing;

import com.l7tech.objectmodel.imp.GoidEntityImp;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * LicenseDocument represents the XML content of a Gateway License.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
@Entity
@Proxy(lazy=false)
@Table(name="license_document")
public class LicenseDocument extends GoidEntityImp {
    @Lob
    @Column(name = "contents", unique = true, nullable = false)
    private String contents;

    public LicenseDocument() {

    }

    public LicenseDocument(@NotNull String contents) {
        super();
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        LicenseDocument that = (LicenseDocument) o;

        return ObjectUtils.equals(this.getContents(), that.getContents()) &&
                ObjectUtils.equals(this.getVersion(), that.getVersion());
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 41 + (contents != null ? contents.hashCode() : 0);
    }
}
