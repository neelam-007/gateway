package com.l7tech.server.management.api.node;

import com.l7tech.server.management.api.TypedValue;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
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
    enum ReportType { PERFORMANCE_SUMMARY, PERFORMANCE_INTERVAL, USAGE_SUMMARY, USAGE_INTERVAL }

    /**
     * Supported report output types.
     */
    @XmlRootElement(name="ReportOutputType", namespace="http://www.layer7tech.com/management/report")
    enum ReportOutputType { PDF, HTML }

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

        @XmlRootElement(name="ReportParameter", namespace="http://www.layer7tech.com/management/report")
        public static class ReportParam {
            private String name;
            private Object value;
            private TypedValue typedValue;

            @XmlAttribute
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            @XmlTransient
            public Object getValue() {
                if ( value == null && typedValue !=null ) {
                    value = typedValue.value();                    
                }
                return value;
            }

            public void setValue( final Object value ) {
                this.value = value;
                this.typedValue = new TypedValue( value );
            }

            @XmlElement
            public TypedValue getTypedValue() {
                return this.typedValue;
            }

            public void setTypedValue( final TypedValue typedValue ) {
                this.typedValue = typedValue;
            }
        }
    }

    class ReportException extends Exception {
        public ReportException( final String message ) {
            super(message);
        }
    }
}
