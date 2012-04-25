package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailEvent.AuditDetailWithInfo;
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

    void setCurrentRecord(AuditRecord record) {
        this.record = record;
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail( new AuditDetailWithInfo(  source, detail, null, null ) );
    }

    @Override
    public void addDetail( final AuditDetailWithInfo detailWithInfo ) {
        List<AuditDetail> details = this.details.get( detailWithInfo.getSource() );
        if ( details == null ) {
            details = new ArrayList<AuditDetail>();
            this.details.put( detailWithInfo.getSource(), details );
        }
        final AuditDetail detail = detailWithInfo.getDetail();
        detail.setOrdinal( ordinal++ );
        details.add( detail );
    }

    @Override
    public Set<AuditDetailMessage.Hint> getHints() {
        return Collections.emptySet();
    }

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
            globalLastRecord = record;
        }

        clear();
    }

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

    public static AuditRecord getGlobalLastRecord() {
        return globalLastRecord;
    }

    @Override
    public void setContextVariables(Map<String, Object> variables) {
        // do nothing
    }

    //- PRIVATE

    private static AuditRecord globalLastRecord;
    private AuditRecord lastRecord;
    private AuditRecord record;
    private int ordinal = 0;
    private Map<Object,List<AuditDetail>> details;
}
