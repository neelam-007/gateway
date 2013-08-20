package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.imp.GoidEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.Arrays;

/**
 * 
 */
@Entity
@Proxy(lazy=false)
@Table(name="report_artifact")
public class StandardReportArtifact extends GoidEntityImp {

    //- PUBLIC

    @ManyToOne(optional=false)
    @JoinColumn(name="report_oid", nullable=false)
    public StandardReport getReport() {
        return report;
    }

    public void setReport( final StandardReport report ) {
        this.report = report;
    }

    @Column(name="content_type", nullable=false, length=128)
    public String getContentType() {
        return contentType;
    }

    public void setContentType( final String contentType ) {
        this.contentType = contentType;
    }

    @Basic(fetch=FetchType.LAZY)
    @Column(name="data", nullable=false, length=1024*1024*5)
    @Lob
    public byte[] getReportData() {
        return reportData;
    }

    public void setReportData( final byte[] reportData ) {
        this.reportData = reportData;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StandardReportArtifact that = (StandardReportArtifact) o;

        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (report != null ? !report.equals(that.report) : that.report != null) return false;
        if (!Arrays.equals(reportData, that.reportData)) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (report != null ? report.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (reportData != null ? Arrays.hashCode(reportData) : 0);
        return result;
    }

    //- PRIVATE

    private StandardReport report;    
    private String contentType;
    private byte[] reportData;
}
