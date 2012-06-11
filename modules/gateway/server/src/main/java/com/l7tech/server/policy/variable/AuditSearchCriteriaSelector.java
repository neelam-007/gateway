package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Variable selector that supports audit search criteria of the current audit lookup policy.
 */
public class AuditSearchCriteriaSelector implements ExpandVariables.Selector<AuditSearchCriteria> {

    private static final Logger logger = Logger.getLogger(AuditSearchCriteriaSelector.class.getName());
    private static final Level[] LEVELS_IN_ORDER = { Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE };

    @Override
    public Selection select(String contextName, AuditSearchCriteria criteria, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if (criteria == null)
            return null;
        if(name.equals("minTime")){
            long minTime = criteria.fromTime.getTime();
            return new Selection(minTime);
        }
        else if(name.equals("maxTime")){
            long maxTime =  criteria.toTime==null ? System.currentTimeMillis(): criteria.toTime.getTime();
            return new Selection(maxTime);
        }else if(name.equals("levels")){
            int fromLevel = criteria.fromLevel == null ? Level.FINEST.intValue():  criteria.fromLevel.intValue();
            int toLevel = criteria.toLevel == null ?  Level.SEVERE.intValue():  criteria.toLevel.intValue();

            List<Integer> levels = new ArrayList<Integer>();
            if (fromLevel == toLevel) {
                levels.add(fromLevel);
            } else {
                if (fromLevel > toLevel){
                   logger.warning("fromLevel " + fromLevel + " is not lower in value than toLevel " + toLevel);
                   return null;
                }
                for (Level level : LEVELS_IN_ORDER) {
                    if (level.intValue() >= fromLevel && level.intValue() <= toLevel) {
                        levels.add(level.intValue());
                    }
                }
            }
            return new Selection(levels);
        }
        else if(name.equals("auditType")){
            String type;
            if(criteria.recordClass == null) {
                type = "%";
            } else if (criteria.recordClass.equals( MessageSummaryAuditRecord.class)) {
                type = AuditRecordUtils.TYPE_MESSAGE;
            } else if (criteria.recordClass.equals(  SystemAuditRecord.class)) {
                type = AuditRecordUtils.TYPE_SYSTEM;
            } else if (criteria.recordClass.equals( AdminAuditRecord.class))  {
                type = AuditRecordUtils.TYPE_ADMIN;
            } else {
                type = "%";
            }
            return new Selection(type);
        } else if(name.equals("nodeId")){
            return new Selection(criteria.nodeId==null?"%":criteria.nodeId);
        } else if(   name.equals("serviceName")){
            return new Selection(criteria.serviceName==null?"%":criteria.serviceName);
        } else if( name.equals("userName")){
            return new Selection(criteria.userName==null?"%":criteria.userName);
        } else if( name.equals("userIdOrDn")){
            return new Selection(criteria.userIdOrDn==null?"%":criteria.userIdOrDn);
        } else if(name.equals("entityClassName")){
            return new Selection(criteria.entityClassName==null?"%":criteria.entityClassName);
        } else if(name.equals("message")){
            return new Selection(criteria.message==null?"%":criteria.message);
        } else if(name.equals("entityId")){
            return new Selection(criteria.entityId==null?"%":criteria.entityId);
        } else if( name.equals("requestId")){
            return new Selection(criteria.requestId==null?"%":criteria.requestId);
        }else if (name.equals("messageId")){
            return new Selection(criteria.messageId);
        }
        else {
            return null;
        }

    }

    @Override
    public Class<AuditSearchCriteria> getContextObjectClass() {
        return AuditSearchCriteria.class;
    }
}
