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

        @Override
        public String toString() {
            return "ReportStatus[id='"+id+"'; status='"+status+"']";
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReportStatus that = (ReportStatus) o;

            if (time != that.time) return false;
            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (status != that.status) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (id != null ? id.hashCode() : 0);
            result = 31 * result + (int) (time ^ (time >>> 32));
            result = 31 * result + (status != null ? status.hashCode() : 0);
            return result;
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

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReportResult that = (ReportResult) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (type != that.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (id != null ? id.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
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

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ReportSubmission that = (ReportSubmission) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
            if (type != that.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (name != null ? name.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
            return result;
        }

        @XmlRootElement(name="ReportParameter", namespace="http://www.layer7tech.com/management/report")
        public static class ReportParam {
            private String name;
            private Object value;
            private TypedValue typedValue;

            public ReportParam() {
            }

            public ReportParam( final String name, final Object value ) {
                setName( name );
                setValue( value );
            }

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

            @Override
            @SuppressWarnings({"RedundantIfStatement"})
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                ReportParam that = (ReportParam) o;

                if (name != null ? !name.equals(that.name) : that.name != null) return false;
                if (typedValue != null ? !typedValue.equals(that.typedValue) : that.typedValue != null) return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result;
                result = (name != null ? name.hashCode() : 0);
                result = 31 * result + (typedValue != null ? typedValue.hashCode() : 0);
                return result;
            }
        }
    }

    class ReportException extends Exception {
        public ReportException( final String message ) {
            super(message);
        }
    }
}
