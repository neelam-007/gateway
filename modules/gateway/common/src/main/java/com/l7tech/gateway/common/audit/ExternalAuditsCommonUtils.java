package com.l7tech.gateway.common.audit;


/**
 */
public class ExternalAuditsCommonUtils {

    public static String makeDefaultAuditSinkPolicyXml(String connection, String recordTable, String detailTable) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"Start default audit sink policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "        <L7p:UUIDGenerator>\n" +
                "            <L7p:TargetVariable stringValue=\"record.guid\"/>\n" +
                "        </L7p:UUIDGenerator>\n" +
                "        <L7p:JdbcQuery>\n" +
                "            <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "            <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "            <L7p:SqlQuery stringValue=\""+saveRecordQuery(recordTable)+"\"/>\n" +
                "            <L7p:ResolveAsObjectList stringListValue=\"included\">\n" +
                "                <L7p:item stringValue=\"audit.reqZip\"/>\n" +
                "                <L7p:item stringValue=\"audit.resZip\"/>\n" +
                "                <L7p:item stringValue=\"audit.componentId\"/>\n" +
                "                <L7p:item stringValue=\"audit.status\"/>\n" +
                "                <L7p:item stringValue=\"audit.authenticated\"/>\n" +
                "                <L7p:item stringValue=\"audit.requestContentLength\"/>\n" +
                "                <L7p:item stringValue=\"audit.responseContentLength\"/>\n" +
                "                <L7p:item stringValue=\"audit.responseStatus\"/>\n" +
                "                <L7p:item stringValue=\"audit.routingLatency\"/>\n" +
                "            </L7p:ResolveAsObjectList>\n" +
                "        </L7p:JdbcQuery>\n" +
                "        <L7p:ForEachLoop L7p:Usage=\"Required\"\n" +
                "            loopVariable=\"audit.details\" variablePrefix=\"i\">\n" +
                "            <L7p:UUIDGenerator>\n" +
                "                <L7p:TargetVariable stringValue=\"detail.guid\"/>\n" +
                "            </L7p:UUIDGenerator>\n" +
                "            <L7p:JdbcQuery>\n" +
                "                <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "                <L7p:SqlQuery stringValue=\""+saveDetailQuery(detailTable)+"\"/>\n" +
                "            </L7p:JdbcQuery>\n" +
                "        </L7p:ForEachLoop>\n" +
                "        <L7p:CommentAssertion>\n" +
                "            <L7p:Comment stringValue=\"End default audit sink policy\"/>\n" +
                "        </L7p:CommentAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n" +
                "";
    }

    public static String saveDetailQuery(String detailTable){
        return "insert into "+detailTable +"( audit_oid,time,component_id,ordinal,message_id,exception_message,properties) values " +
                "(${record.guid},${i.current.time},${i.current.componentId},${i.current.ordinal},${i.current.messageId},${i.current.exception},${i.current.properties})";
    }

    public  static String saveRecordQuery(String recordTable){
        return  "insert into "+recordTable+"(id,nodeid,time,type,audit_level,name,message,ip_address,user_name,user_id,provider_oid,signature,properties," +
                "entity_class,entity_id," +
                "status,request_id,service_oid,operation_name,authenticated,authenticationType,request_length,response_length,request_xml,response_xml,response_status,routing_latency," +
                "component_id,action)" +
                " values " +
                "(${record.guid},${audit.nodeId},${audit.time},${audit.type},${audit.level},${audit.name},${audit.message},${audit.ipAddress},${audit.user.name},${audit.user.id},${audit.user.idProv},${audit.signature},${audit.properties}," +
                "${audit.entity.class},${audit.entity.oid}," +
                "${audit.status},${audit.requestId},${audit.serviceOid},${audit.operationName},${audit.authenticated},${audit.authType},${audit.requestContentLength},${audit.responseContentLength},${audit.reqZip},${audit.resZip},${audit.responseStatus},${audit.routingLatency}," +
                "${audit.componentId},${audit.action})";
    }

    private static String defaultLookupQuery(String recordTable,String dbType) {
        return
                lookupPrefix(dbType)+" * from "+recordTable+" where " +
                        "time>=${audit.recordQuery.minTime} and time&lt;${audit.recordQuery.maxTime} " +
                        "and audit_level in (${audit.recordQuery.levels}) " +
                        "and "+ getColumnQuery("nodeid",dbType)+ " like ${audit.recordQuery.nodeId} " +
                        "and "+ getColumnQuery("type",dbType)+ " like ${audit.recordQuery.auditType} " +
                        "and lower("+ getColumnQuery("name",dbType)+ ") like lower(${serviceName}) escape '#'  " +
                        "and lower("+ getColumnQuery("user_name",dbType)+ ") like lower(${audit.recordQuery.userName}) " +
                        "and lower("+ getColumnQuery("user_id",dbType)+ ") like lower(${audit.recordQuery.userIdOrDn}) " +
                        "and lower("+ getColumnQuery("message",dbType)+ ") like lower(${audit.recordQuery.message}) " +
                        "and lower("+ getColumnQuery("entity_class",dbType)+ ") like lower(${audit.recordQuery.entityClassName}) " +
                        "and "+ getColumnQuery("entity_id",dbType)+ " like ${audit.recordQuery.entityId} " +
                        "and lower("+ getColumnQuery("request_id",dbType)+ ") like lower(${audit.recordQuery.requestId}) order by time desc "+lookupPostfix(dbType);
    }


    private static String lookupQueryWithAuditId(String recordTable,String dbType){
        return
                lookupPrefix(dbType)+ " * from "+recordTable+" where " +
                        "id in (${recordIdQuery.audit_oid}) " +
                        "and time>=${audit.recordQuery.minTime} and time&lt;=${audit.recordQuery.maxTime} " +
                        "and audit_level in (${audit.recordQuery.levels}) " +
                        "and "+ getColumnQuery("nodeid",dbType)+ " like ${audit.recordQuery.nodeId} " +
                        "and "+ getColumnQuery("type",dbType)+ " like ${audit.recordQuery.auditType} " +
                        "and lower("+ getColumnQuery("name",dbType)+ ") like lower(${serviceName}) escape '#'  " +
                        "and lower("+ getColumnQuery("user_name",dbType)+ ") like lower(${audit.recordQuery.userName}) " +
                        "and lower("+ getColumnQuery("user_id",dbType)+ ") like lower(${audit.recordQuery.userIdOrDn}) " +
                        "and lower("+ getColumnQuery("message",dbType)+ ") like lower(${audit.recordQuery.message}) " +
                        "and lower("+ getColumnQuery("entity_class",dbType)+ ") like lower(${audit.recordQuery.entityClassName}) " +
                        "and "+ getColumnQuery("entity_id",dbType)+ " like ${audit.recordQuery.entityId}  " +
                        "and lower("+ getColumnQuery("request_id",dbType)+ ") like lower(${audit.recordQuery.requestId}) order by time desc "+lookupPostfix(dbType);
    }

    private static String getColumnQuery(String columnName, String dbType) {
        if(dbType.equals("oracle")){
            return "nvl("+columnName+",' ') ";
        }
        return columnName;
    }

    private static String lookupPrefix(String dbType) {
        if(dbType.equals("mysql"))
            return "SELECT";
        else if(dbType.equals("sqlserver"))
            return "SET ROWCOUNT ${recordQueryLimit} SELECT";
        else if(dbType.equals("oracle"))
            return "SELECT *  FROM ( SELECT";
        else if(dbType.equals("db2"))
            return "SELECT";
        return null;
    }

    private static String lookupPostfix(String dbType) {
        if(dbType.equals("mysql"))
            return "limit ${recordQueryLimit}";
        else if(dbType.equals("sqlserver"))
            return "";
        else if(dbType.equals("oracle"))
            return " ) temp WHERE rownum &lt;= ${recordQueryLimit}  ORDER BY rownum";
        else if(dbType.equals("db2"))
            return "limit ${recordQueryLimit}";
        return null;
    }

    private static String messaageIdLookupQuery ( String detailTable, String dbType){
        return
                lookupPrefix(dbType)+ " distinct audit_oid,time from "+detailTable+" where message_id >=${messageIdMin}  and message_id &lt;=${messageIdMax} and time>=${audit.recordQuery.minTime} and time&lt;=${audit.recordQuery.maxTime}  order by time desc "+lookupPostfix(dbType);
    }

    private static String detailLookupQuery (String detailTable){
        return
                "select * from "+detailTable+" where audit_oid in (${recordQuery.id})";
    }

    public static String makeDefaultAuditLookupPolicyXml(String connection, String recordTable, String detailTable,String dbType) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References>\n" +
                "        <JdbcConnectionReference RefType=\"com.l7tech.console.policy.exporter.JdbcConnectionReference\">\n" +
                "            <ConnectionName>"+connection+"</ConnectionName>\n" +
                "        </JdbcConnectionReference>\n" +
                "    </exp:References>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:CommentAssertion>\n" +
                "                <L7p:Comment stringValue=\"Start Auto-Generated Lookup Policy\"/>\n" +
                "            </L7p:CommentAssertion>\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5zZXJ2aWNlTmFtZX0=\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"serviceName\"/>\n" +
                "            </L7p:SetVariable>\n" +
                "            <L7p:SetVariable>\n" +
                "                <L7p:Base64Expression stringValue=\"MTAw\"/>\n" +
                "                <L7p:DataType variableDataType=\"int\"/>\n" +
                "                <L7p:VariableToSet stringValue=\"recordQueryLimit\"/>\n" +
                "            </L7p:SetVariable>\n" +
                getServiceNameEscapingAssertions(dbType)+
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion>\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${audit.recordQuery.entityId}\"/>\n" +
                "                    <L7p:Operator operatorNull=\"null\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item dataType=\"included\">\n" +
                "                            <L7p:Type variableDataType=\"string\"/>\n" +
                "                        </L7p:item>\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:RightValue stringValue=\"%\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5lbnRpdHlJZH0=\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"entityIdMax\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5lbnRpdHlJZH0=\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"entityIdMin\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                </wsp:All>\n" +
                "            </wsp:OneOrMore>\n" +
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"messageId\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:ComparisonAssertion>\n" +
                "                        <L7p:Expression1 stringValue=\"${messageId}\"/>\n" +
                "                        <L7p:Expression2 stringValue=\"\"/>\n" +
                "                        <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                        <L7p:Predicates predicates=\"included\">\n" +
                "                            <L7p:item binary=\"included\">\n" +
                "                                <L7p:Operator operator=\"EMPTY\"/>\n" +
                "                                <L7p:RightValue stringValue=\"\"/>\n" +
                "                            </L7p:item>\n" +
                "                        </L7p:Predicates>\n" +
                "                    </L7p:ComparisonAssertion>\n" +
                "                    <L7p:JdbcQuery>\n" +
                "                        <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                        <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                        <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "                        <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                        <L7p:NamingMap mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"id\"/>\n" +
                "                                <L7p:value stringValue=\"id\"/>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:NamingMap>\n" +
                "                        <L7p:SqlQuery stringValue=\""+defaultLookupQuery(recordTable,dbType)+"\"/>\n" +
                "                        <L7p:VariablePrefix stringValue=\"recordQuery\"/>\n" +
                "                    </L7p:JdbcQuery>\n" +
                "                </wsp:All>\n" +
                "                <wsp:All wsp:Usage=\"Required\">\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"messageIdMax\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:SetVariable>\n" +
                "                        <L7p:Base64Expression stringValue=\"JHthdWRpdC5yZWNvcmRRdWVyeS5tZXNzYWdlSWR9\"/>\n" +
                "                        <L7p:VariableToSet stringValue=\"messageIdMin\"/>\n" +
                "                    </L7p:SetVariable>\n" +
                "                    <L7p:JdbcQuery>\n" +
                "                        <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                        <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                        <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "                        <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                        <L7p:NamingMap mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"audit_oid\"/>\n" +
                "                                <L7p:value stringValue=\"audit_oid\"/>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:NamingMap>\n" +
                "                        <L7p:SqlQuery stringValue=\""+messaageIdLookupQuery(detailTable,dbType)+" \"/>\n" +
                "                        <L7p:VariablePrefix stringValue=\"recordIdQuery\"/>\n" +
                "                    </L7p:JdbcQuery>\n" +
                "                    <L7p:JdbcQuery>\n" +
                "                        <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                        <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                        <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "                        <L7p:MaxRecords intValue=\"1000\"/>\n" +
                "                        <L7p:NamingMap mapValue=\"included\">\n" +
                "                            <L7p:entry>\n" +
                "                                <L7p:key stringValue=\"id\"/>\n" +
                "                                <L7p:value stringValue=\"id\"/>\n" +
                "                            </L7p:entry>\n" +
                "                        </L7p:NamingMap>\n" +
                "                        <L7p:SqlQuery stringValue=\""+lookupQueryWithAuditId(recordTable,dbType)+"\"/>\n" +
                "                        <L7p:VariablePrefix stringValue=\"recordQuery\"/>\n" +
                "                    </L7p:JdbcQuery>\n" +
                "                </wsp:All>\n" +
                "            </wsp:OneOrMore>\n" +
                "            <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
                "                <L7p:ComparisonAssertion>\n" +
                "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
                "                    <L7p:Expression1 stringValue=\"${recordQuery.queryresult.count}\"/>\n" +
                "                    <L7p:Operator operatorNull=\"null\"/>\n" +
                "                    <L7p:Predicates predicates=\"included\">\n" +
                "                        <L7p:item dataType=\"included\">\n" +
                "                            <L7p:Type variableDataType=\"int\"/>\n" +
                "                        </L7p:item>\n" +
                "                        <L7p:item binary=\"included\">\n" +
                "                            <L7p:RightValue stringValue=\"0\"/>\n" +
                "                        </L7p:item>\n" +
                "                    </L7p:Predicates>\n" +
                "                </L7p:ComparisonAssertion>\n" +
                "                <L7p:JdbcQuery>\n" +
                "                    <L7p:AllowMultiValuedVariables booleanValue=\"true\"/>\n" +
                "                    <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
                "                    <L7p:ConnectionName stringValue=\""+connection+"\"/>\n" +
                "                    <L7p:MaxRecords intValue=\"10000\"/>\n" +
                "                    <L7p:SqlQuery stringValue=\""+detailLookupQuery(detailTable)+"\"/>\n" +
                "                    <L7p:VariablePrefix stringValue=\"detailQuery\"/>\n" +
                "                </L7p:JdbcQuery>\n" +
                "            </wsp:OneOrMore>\n" +
                "            <L7p:CommentAssertion>\n" +
                "                <L7p:Comment stringValue=\"End Auto-Generated Lookup Policy\"/>\n" +
                "            </L7p:CommentAssertion>\n" +
                "            <L7p:CommentAssertion>\n" +
                "                <L7p:Comment stringValue=\"Add Additional Assertions Here\"/>\n" +
                "            </L7p:CommentAssertion>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n";
    }

    private static String getServiceNameEscapingAssertions(String dbType) {

        if(dbType.equals("db2") || dbType.equals("oracle"))
            return "";
        return
            "            <L7p:Regex>\n" +
            "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
            "                <L7p:OtherTargetMessageVariable stringValue=\"serviceName\"/>\n" +
            "                <L7p:Regex stringValue=\"\\[\"/>\n" +
            "                <L7p:Replace booleanValue=\"true\"/>\n" +
            "                <L7p:Replacement stringValue=\"#[\"/>\n" +
            "                <L7p:Target target=\"OTHER\"/>\n" +
            "            </L7p:Regex>\n" +
            "            <L7p:Regex>\n" +
            "                <L7p:AutoTarget booleanValue=\"false\"/>\n" +
            "                <L7p:OtherTargetMessageVariable stringValue=\"serviceName\"/>\n" +
            "                <L7p:Regex stringValue=\"\\]\"/>\n" +
            "                <L7p:Replace booleanValue=\"true\"/>\n" +
            "                <L7p:Replacement stringValue=\"#]\"/>\n" +
            "                <L7p:Target target=\"OTHER\"/>\n" +
            "            </L7p:Regex>\n";
    }
}
