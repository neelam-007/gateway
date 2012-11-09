package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.AuditLookupSearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Variable selector that supports audit search criteria of the current audit lookup policy.
 */
public class AuditSearchCriteriaSelector implements ExpandVariables.Selector<AuditLookupSearchCriteria> {

    private static final Logger logger = Logger.getLogger(AuditSearchCriteriaSelector.class.getName());
    private static final Level[] LEVELS_IN_ORDER = { Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE };

    @Override
    public Selection select(String contextName, AuditLookupSearchCriteria criteria, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        if(name.equals("guid")){
            return new Selection( criteria.getGuids() == null ? "" :criteria.getGuids());
        }
        if(name.equals("maxMessageSize")){
            return new Selection( criteria.getMaxMessageSize());
        }

        if (criteria.getCriteria() == null)
            return null;
        if(name.equals("minTime")){
            long minTime = criteria.getCriteria().fromTime.getTime();
            return new Selection(minTime);
        }
        else if(name.equals("maxTime")){
            long maxTime =  criteria.getCriteria().toTime==null ? System.currentTimeMillis(): criteria.getCriteria().toTime.getTime();
            return new Selection(maxTime);
        }else if(name.equals("levels")){
            int fromLevel = criteria.getCriteria().fromLevel == null ? Level.FINEST.intValue():  criteria.getCriteria().fromLevel.intValue();
            int toLevel = criteria.getCriteria().toLevel == null ?  Level.SEVERE.intValue():  criteria.getCriteria().toLevel.intValue();

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
            if(criteria.getCriteria().recordClass == null) {
                type = "%";
            } else if (criteria.getCriteria().recordClass.equals( MessageSummaryAuditRecord.class)) {
                type = AuditRecordUtils.TYPE_MESSAGE;
            } else if (criteria.getCriteria().recordClass.equals(  SystemAuditRecord.class)) {
                type = AuditRecordUtils.TYPE_SYSTEM;
            } else if (criteria.getCriteria().recordClass.equals( AdminAuditRecord.class))  {
                type = AuditRecordUtils.TYPE_ADMIN;
            } else {
                type = "%";
            }
            return new Selection(type);
        } else if(name.equals("nodeId")){
            return new Selection(criteria.getCriteria().nodeId==null?"%":criteria.getCriteria().nodeId);
        } else if(   name.equals("serviceName")){
            return new Selection(criteria.getCriteria().serviceName==null?"%":criteria.getCriteria().serviceName);
        } else if( name.equals("userName")){
            return new Selection(criteria.getCriteria().userName==null?"%":criteria.getCriteria().userName);
        } else if( name.equals("userIdOrDn")){
            return new Selection(criteria.getCriteria().userIdOrDn==null?"%":criteria.getCriteria().userIdOrDn);
        } else if(name.equals("entityClassName")){
            return new Selection(criteria.getCriteria().entityClassName==null?"%":criteria.getCriteria().entityClassName);
        } else if(name.equals("message")){
            return new Selection(criteria.getCriteria().message==null?"%":criteria.getCriteria().message);
        } else if(name.equals("entityId")){
            return new Selection(criteria.getCriteria().entityId==null?"%":criteria.getCriteria().entityId);
        } else if( name.equals("requestId")){
            return new Selection(criteria.getCriteria().requestId==null?"%":criteria.getCriteria().requestId);
        } else if (name.equals("messageId")){
            return new Selection(criteria.getCriteria().messageId);
        } else if( name.equals("operation")){
            return new Selection(criteria.getCriteria().operation==null?"%":criteria.getCriteria().operation);
        } else {
            return null;
        }

    }

    @Override
    public Class<AuditLookupSearchCriteria> getContextObjectClass() {
        return AuditLookupSearchCriteria.class;
    }
}
