package com.l7tech.server.management.api.node;

import javax.activation.DataHandler;
import javax.xml.transform.Source;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.ws.soap.MTOM;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebParam;
import java.util.Collection;

/**
 * EMS / Gateway reporting API
 */
@MTOM
@WebService(name="Report", targetNamespace="http://www.layer7tech.com/management/report")
public interface ReportApi {

    /**
     * Submit a report for generation of the given result types.
     *
     * @param submission The report submission details.
     * @param types The desired output formats.
     * @return The ID for this report submission.
     * @throws ReportException on error
     */
    @WebMethod(operationName="SubmitReport")
    @WebResult(name="SubmissionIdentifier", targetNamespace="http://www.layer7tech.com/management/report")
    String submitReport( @WebParam(name="ReportSubmission") ReportSubmission submission,
                         @WebParam(name="ReportOutputTypes") Collection<ReportOutputType> types ) throws ReportException;

    /**
     * Get the status for a set of reports.
     *
     * @param ids The set of ids to report on.
     * @return the set of report status (one per id)
     * @throws ReportException on error
     */
    @WebMethod(operationName="GetReportStatus")
    @WebResult(name="ReportStatus", targetNamespace="http://www.layer7tech.com/management/report")
    Collection<ReportStatus> getReportStatus( @WebParam(name="SubmissionIdentifiers") Collection<String> ids ) throws ReportException;

    /**
     * Get report result.
     *
     * @param id The report whose result is desired.
     * @return The report result
     * @throws ReportException on error
     */
    @WebMethod(operationName="GetReportResult")
    @WebResult(name="ReportResult", targetNamespace="http://www.layer7tech.com/management/report")
    ReportResult getReportResult( @WebParam(name="SubmissionIdentifier") String id,
                                  @WebParam(name="ReportOutputType") ReportOutputType type ) throws ReportException;

    /**
     * Supported report types.
     */
    @XmlRootElement(name="ReportType", namespace="http://www.layer7tech.com/management/report")
    enum ReportType { PERFORMANCE, INTERVAL }

    /**
     * Supported report output types.
     */
    @XmlRootElement(name="ReportOutputType", namespace="http://www.layer7tech.com/management/report")
    enum ReportOutputType { RAW, PDF, XML, HTML, XLS }

    /**
     * Report status data.
     */
    @XmlRootElement(name="ReportStatus", namespace="http://www.layer7tech.com/management/report")
    class ReportStatus {
        public enum Status { PENDING, RUNNING, COMPLETED, FAILED }

        private String id;
        private long time;
        private Status status;

        @XmlAttribute
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute
        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        @XmlAttribute
        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }

    /**
     * Report result data.
     */
    @XmlRootElement(name="ReportResult", namespace="http://www.layer7tech.com/management/report")
    class ReportResult {
        private String id;
        private ReportOutputType type;
        private DataHandler data;

        @XmlAttribute
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlAttribute
        public ReportOutputType getType() {
            return type;
        }

        public void setType(ReportOutputType type) {
            this.type = type;
        }

        @XmlElement
        public DataHandler getData() {
            return data;
        }

        public void setData(DataHandler data) {
            this.data = data;
        }
    }

    /**
     * Report submission data.
     */
    @XmlRootElement(name="ReportSubmission", namespace="http://www.layer7tech.com/management/report")
    class ReportSubmission {
        private String name;
        private ReportType type;
        private Collection<ReportParam> parameters; // report generation parameters
        private ReportTemplate template;

        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute
        public ReportType getType() {
            return type;
        }

        public void setType(ReportType type) {
            this.type = type;
        }

        @XmlElement
        public Collection<ReportParam> getParameters() {
            return parameters;
        }

        public void setParameters(Collection<ReportParam> parameters) {
            this.parameters = parameters;
        }

        @XmlElement
        public ReportTemplate getTemplate() {
            return template;
        }

        public void setTemplate(ReportTemplate template) {
            this.template = template;
        }

        @XmlRootElement(name="ReportParameter", namespace="http://www.layer7tech.com/management/report")
        public static class ReportParam {
            private String name;
            private Object value;

            @XmlAttribute
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            @XmlElement
            public Object getValue() {
                return value;
            }

            public void setValue(Object value) {
                this.value = value;
            }
        }
    }

    @XmlRootElement(name="ReportTemplate", namespace="http://www.layer7tech.com/management/report")
    class ReportTemplate {
        private Source masterSourceXml;
        private Source masterSourceXsl;
        private Collection<SubReport> subReports;

        @XmlElement
        public Source getMasterSourceXml() {
            return masterSourceXml;
        }

        public void setMasterSourceXml(Source masterSourceXml) {
            this.masterSourceXml = masterSourceXml;
        }

        @XmlElement
        public Source getMasterSourceXsl() {
            return masterSourceXsl;
        }

        public void setMasterSourceXsl(Source masterSourceXsl) {
            this.masterSourceXsl = masterSourceXsl;
        }

        @XmlElement
        public Collection<SubReport> getSubReports() {
            return subReports;
        }

        public void setSubReports(Collection<SubReport> subReports) {
            this.subReports = subReports;
        }

        // / sub jasper template * X  (o)
        // Map<paramkey, <xml,xsl>>
        // xsl * X  (input xml, param keys + db values)
        @XmlRootElement(name="SubReportTemplate", namespace="http://www.layer7tech.com/management/report")
        public static class SubReport {
            private String reportParamName;
            private Source sourceXml;
            private Source sourceXsl;

            @XmlAttribute
            public String getReportParamName() {
                return reportParamName;
            }

            public void setReportParamName(String reportParamName) {
                this.reportParamName = reportParamName;
            }

            @XmlElement
            public Source getSourceXml() {
                return sourceXml;
            }

            public void setSourceXml(Source sourceXml) {
                this.sourceXml = sourceXml;
            }

            @XmlElement
            public Source getSourceXsl() {
                return sourceXsl;
            }

            public void setSourceXsl(Source sourceXsl) {
                this.sourceXsl = sourceXsl;
            }
        }
    }

    class ReportException extends Exception {
        public ReportException( final String message ) {
            super(message);
        }
    }
}
