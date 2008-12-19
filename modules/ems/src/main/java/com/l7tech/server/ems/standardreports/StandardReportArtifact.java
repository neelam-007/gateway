package com.l7tech.server.ems.standardreports;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Basic;
import javax.persistence.FetchType;

/**
 * 
 */
@Entity
@Table(name="report_artifact")
public class StandardReportArtifact extends PersistentEntityImp {

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

    //- PRIVATE

    private StandardReport report;    
    private String contentType;
    private byte[] reportData;
}
