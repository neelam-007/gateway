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
import java.util.*;

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
     * Get grouping keys.
     *
     * @throws ReportException on error
     */
    @WebMethod(operationName="GetGroupingKeys")
    @WebResult(name="GroupingKeys", targetNamespace="http://www.layer7tech.com/management/report")
    Collection<GroupingKey> getGroupingKeys() throws ReportException;

    @XmlRootElement(name="GroupingKey", namespace="http://www.layer7tech.com/management/report")
    class GroupingKey {
        public enum GroupingKeyType { STANDARD, CUSTOM }

        private GroupingKeyType type;
        private String name;

        public GroupingKey() {
        }

        public GroupingKey( final GroupingKeyType type, final String name ) {
            this.type = type;
            this.name = name;
        }

        public GroupingKeyType getType() {
            return type;
        }

        public void setType(GroupingKeyType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GroupingKey that = (GroupingKey) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (type != that.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (type != null ? type.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    /**
     * Supported report types.
     */
    @XmlRootElement(name="ReportType", namespace="http://www.layer7tech.com/management/report")
    enum ReportType { PERFORMANCE_SUMMARY, PERFORMANCE_INTERVAL, USAGE_SUMMARY, USAGE_INTERVAL;

        public static String [] getApplicableParameters(ReportType reportType){
            if(reportType == PERFORMANCE_SUMMARY || reportType == USAGE_SUMMARY){
                return ReportParameters.COMMON_PARAMS;
            }else{
                List<String> returnList = new ArrayList<String>(Arrays.asList(ReportParameters.COMMON_PARAMS));
                returnList.addAll(Arrays.asList(ReportParameters.INTERVAL_PARAMS));
                return returnList.toArray(new String[]{});
            }
        }
    }

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
        private String message;
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

        @XmlElement
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
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

    class ReportParameters{

        //common
        public static final String SPECIFIC_TIME_ZONE = "SPECIFIC_TIME_ZONE";
        public static final String IS_RELATIVE = "IS_RELATIVE";
        public static final String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
        public static final String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";
        public static final String IS_ABSOLUTE = "IS_ABSOLUTE";
        public static final String ABSOLUTE_START_TIME = "ABSOLUTE_START_TIME";
        public static final String ABSOLUTE_END_TIME = "ABSOLUTE_END_TIME";
        public static final String REPORT_RAN_BY = "REPORT_RAN_BY";
        public static final String SERVICE_ID_TO_OPERATIONS_MAP = "SERVICE_ID_TO_OPERATIONS_MAP";
        public static final String SERVICE_ID_TO_NAME_MAP = "SERVICE_ID_TO_NAME_MAP";
        public static final String KEYS_TO_LIST_FILTER_PAIRS = "KEYS_TO_LIST_FILTER_PAIRS";
        /**
         * IS_CONTEXT_MAPPING tells the report that the new service_metric_details table is required. Used in
         * combination with IS_USING_KEYS.
         */
        public static final String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";
        public static final String IS_DETAIL = "IS_DETAIL";
        public static final String PRINT_CHART = "PRINT_CHART";

        //only needed for interval reports
        public static final String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
        public static final String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";

        //only supplied on a gateway
        //keeping here so they are known
        public static final String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
        public static final String SUBREPORT_DIRECTORY = "SUBREPORT_DIRECTORY";
        public static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
        public static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
        public static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
        public static final String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";

        /**
         * IS_USING_KEYS tells the performance statistics reports that keys other than operation are being used
         * in the report. This is used in combination with IS_CONTEXT_MAPPING
         */
        public static final String IS_USING_KEYS = "IS_USING_KEYS";

        /**
         * IS_IGNORE_PAGINATION when set to true, tells the Jasper engine to create the report as one long page
         */
        //public static final String IS_IGNORE_PAGINATION = "IS_IGNORE_PAGINATION";

        //interval only ssg params
        public static final String SUB_INTERVAL_SUB_REPORT = "SUB_INTERVAL_SUB_REPORT";
        public static final String SUB_REPORT = "SUB_REPORT";

        //usage interva only ssg param
        public static final String SUB_REPORT_HELPER = "SUB_REPORT_HELPER";
        
        //used for passing data around in ReportGenerator - //todo find another way of doing this
        public static final String MAPPING_GROUP_TO_DISPLAY_STRING = "MAPPING_GROUP_TO_DISPLAY_STRING";
        public static final String DISTINCT_MAPPING_SETS = "DISTINCT_MAPPING_SETS";


        public static final String [] RELATIVE_TIME_PARAMS = new String[]{RELATIVE_NUM_OF_TIME_UNITS,
        RELATIVE_TIME_UNIT};

        public static final String [] ABSOLUTE_TIME_PARAMS = new String[]{ABSOLUTE_START_TIME, ABSOLUTE_END_TIME};

        public static final String [] COMMON_PARAMS = new String[]{SPECIFIC_TIME_ZONE, IS_RELATIVE, IS_ABSOLUTE,
                REPORT_RAN_BY, SERVICE_ID_TO_NAME_MAP, SERVICE_ID_TO_OPERATIONS_MAP, KEYS_TO_LIST_FILTER_PAIRS,
                IS_CONTEXT_MAPPING, IS_DETAIL, PRINT_CHART};

        public static final String [] INTERVAL_PARAMS = new String []{INTERVAL_TIME_UNIT, INTERVAL_NUM_OF_TIME_UNITS};
    }


    /**
     * Records a filter value for a mapping key. Each instance represents one value and whether the value in the
     * database should be compared using = or like.
     */
    public static class FilterPair {
        /**
         * Record the unmodified value as entered by the user, so it can be displayed to them in reports
         */
        private final String displayValue;
        private final boolean checkForWildCard;

        public FilterPair(final String filterValue) {
            if(filterValue == null) throw new NullPointerException("filterValue cannot be null");
            checkForWildCard = true;
            displayValue = filterValue.trim();
        }

        /**
         * Has a very specific use in Utilities.createDistinctKeyToFilterMap, where the filter pair holds a value,
         * found at runtime from the database, and we don't want to modify the string found in case it contains
         * the wild card character
         * @param filterValue
         * @param checkForWildCard true if the wild card should be left alone, no translation into '%'
         */
        public FilterPair(final String filterValue, boolean checkForWildCard) {
            displayValue = filterValue;
            this.checkForWildCard = checkForWildCard;
        }

        public FilterPair() {
            displayValue = "";
            checkForWildCard = false;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public String getFilterValue() {
            if(isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < displayValue.length(); i++){
                char c = displayValue.charAt(i);
                if(c == '*' && checkForWildCard) sb.append('%');//translate
                else if(c == '%') sb.append("\\%");//escape
                else if(c == '_') sb.append("\\_");//escape
                else if(c == '\'') sb.append("\\'");//escape
                else sb.append(c);
            }

            return sb.toString();
        }

        public boolean isUseAnd() {
            return displayValue.indexOf('*') == -1;
        }

        public boolean isEmpty() {
            if(displayValue.equals("") || displayValue.equals("*")) return true;
            return false;
        }
    }
}
