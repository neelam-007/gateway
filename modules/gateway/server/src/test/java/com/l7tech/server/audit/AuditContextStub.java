/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import java.util.*;

/**
 * An AuditContext stub implementation. Does absolutely nothing except manage minimal internal state.
 */
public class AuditContextStub implements AuditContextStubInt {

    //- PUBLIC

    public AuditContextStub() {
        details = new HashMap<Object,List<AuditDetail>>();
    }

    @Override
    public void setCurrentRecord(AuditRecord record) {
        this.record = record;
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail( detail, source, null );
    }

    @Override
    public void addDetail(AuditDetail detail, Object source, Throwable exception) {
        List<AuditDetail> details = this.details.get( source );
        if ( details == null ) {
            details = new ArrayList<AuditDetail>();
            this.details.put( source, details );
        }
        detail.setOrdinal( ordinal++ );
        details.add( detail );
    }

    @Override
    public boolean isUpdate() {
        return false;
    }

    @Override
    public void setUpdate(boolean update) {
    }

    @Override
    public Set<AuditDetailMessage.Hint> getHints() {
        return Collections.emptySet();
    }

    @Override
    public void flush() {
        if (record != null) {
            List<AuditDetail> detailList = new ArrayList<AuditDetail>();

            for ( List<AuditDetail> detailGroup : details.values() ) {
                detailList.addAll( detailGroup );
                for (AuditDetail detail : detailGroup) {
                    detail.setAuditRecord(record);
                }
            }

            Collections.sort( detailList, new ResolvingComparator<AuditDetail,Integer>(new Resolver<AuditDetail,Integer>(){
                @Override
                public Integer resolve( final AuditDetail key ) {
                    return key.getOrdinal();
                }
            }, false) );

            record.setDetails( new LinkedHashSet<AuditDetail>(detailList) );
            lastRecord = record;
        }

        clear();
    }

    @Override
    public void clear() {
        record = null;
        ordinal = 0;
        details = new HashMap<Object,List<AuditDetail>>();
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    @Override
    public AuditRecord getLastRecord() {
        return lastRecord;
    }

    @Override
    public String[] getContextVariablesUsed() {
        return new String[0];
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        // do nothing
    }

    //- PRIVATE

    private AuditRecord lastRecord;
    private AuditRecord record;
    private int ordinal = 0;
    private Map<Object,List<AuditDetail>> details;
}
