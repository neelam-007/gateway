package com.l7tech.gateway.common.audit;

/**
 */
public class ExternalAuditsCommonUtils {
    public static String saveDetailQuery(String detailTable){
        return "insert into "+detailTable +"( audit_oid,time,component_id,ordinal,message_id,exception_message,properties) values " +
                "(${record.guid},${i.current.time},${i.current.componentId},${i.current.ordinal},${i.current.messageId},${i.current.exception},${i.current.properties});";
    }

    public  static String saveRecordQuery(String recordTable){
        return  "insert into "+recordTable+"(id,nodeid,time,type,audit_level,name,message,ip_address,user_name,user_id,provider_oid,signature,properties," +
                "entity_class,entity_id," +
                "status,request_id,service_oid,operation_name,authenticated,authenticationType,request_length,response_length,request_xml,response_xml,response_status,routing_latency," +
                "component_id,action)" +
                " values " +
                "(${record.guid},${audit.nodeId},${audit.time},${audit.type},${audit.level},${audit.name},${audit.message},${audit.ipAddress},${audit.user.name},${audit.user.id},${audit.user.idProv},${audit.signature},${audit.properties}," +
                "${audit.entity.class},${audit.entity.oid}," +
                "${audit.status},${audit.requestId},${audit.serviceOid},${audit.operationName},${audit.authenticated},${audit.authType},${audit.reqContentLength},${audit.resContentLength},${audit.reqZip},${audit.resZip},${audit.responseStatus},${audit.routingLatency}," +
                "${audit.componentId},${audit.action});";
    }
}
