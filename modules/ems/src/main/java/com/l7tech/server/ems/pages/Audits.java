package com.l7tech.server.ems.pages;

import org.apache.wicket.spring.injection.annot.SpringBean;
//import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
//import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
//import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
//import org.apache.wicket.spring.injection.annot.SpringBean;

//import java.util.Iterator;
//import java.util.List;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.logging.Logger;

//import com.l7tech.gateway.common.audit.AuditRecord;
//import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.server.audit.AuditRecordManager;
//import com.l7tech.objectmodel.FindException;

/**
 * 
 */
public class Audits extends EmsPage {
    
    //- PUBLIC    

//      <wicket:head>
//                    <style type="text/css">
//                        .table .navigation { background-color: #EFEFEF; }
//                        .table .headers {  background-color: #8F8F8F;  }
//                        .table .odd {  }
//                        .table .even { background-color: #EFEFEF; }
//                    </style>
//                </wicket:head>
//                <wicket:extend>
//                    <table wicket:id="audittable" width="100%" class="table"/>
//                </wicket:extend>
    
    public Audits() {
//        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
//        columns.add(new PropertyColumn(new Model("ID"), "id"));
//        columns.add(new PropertyColumn(new Model("Date"), "millis", "millis"));
//        columns.add(new PropertyColumn(new Model("Level"), "level", "level"));
//        columns.add(new PropertyColumn(new Model("Message"), "message", "message"));
//
//        AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("audittable", columns, new AuditDataProvider(auditRecordManager), 20);
//        add(table);
    }

    //- PRIVATE

//    private static final Logger logger = Logger.getLogger( Audits.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private AuditRecordManager auditRecordManager;

//    private class AuditDataProvider extends SortableDataProvider {
//        private final AuditRecordManager auditRecordManager;
//        private List<AuditRecord> list = new ArrayList<AuditRecord>();
//
//        public AuditDataProvider( final AuditRecordManager auditRecordManager ) {
//            this.auditRecordManager = auditRecordManager;
//            setSort("id", true);
//        }
//
//        public Iterator iterator(int first, int count) {
//            checkInit();
//            return newAuditIter(list, first,first+count,getSort().getProperty(),getSort().isAscending());
//        }
//
//        public int size() {
//            checkInit();
//            return list.size();
//        }
//
//        public IModel model(final Object auditObject) {
//            checkInit();
//             return new AbstractReadOnlyModel() {
//                public Object getObject() {
//                    return auditObject;
//                }
//            };
//        }
//
//        public void checkInit() {
//            if ( list.isEmpty() ) {
//                try {
//                    list.addAll(auditRecordManager.find(new AuditSearchCriteria(null, null, null, null, null, null, -1, -1, 1000)));
//                } catch (FindException fe) {
//                    logger.log( Level.WARNING, "Error searching for audit data.", fe );
//                }
//            }
//        }
//
//        public void detach() {
//            list.clear();
//        }
//    }

//    private Iterator newAuditIter(List<AuditRecord> list, int start, int end, final String sortBy, final boolean asc) {
////        auditAdmin.getSystemLog(logRequest.getNodeId(),
////                                    logRequest.getStartMsgNumber(),
////                                    logRequest.getEndMsgNumber(),
////                                    logRequest.getStartMsgDate(),
////                                    logRequest.getEndMsgDate(),
////                                    FilteredLogTableModel.MAX_MESSAGE_BLOCK_SIZE)
//        Collections.sort(list, new Comparator<AuditRecord>(){
//            @SuppressWarnings({"unchecked"})
//            public int compare(AuditRecord audit1,AuditRecord audit2) {
//                Comparable v1 = audit1.getId();
//                Comparable v2 = audit2.getId();
//
//                if ( "millis".equals(sortBy) ) {
//                    v1 = audit1.getMillis();
//                    v2 = audit2.getMillis();
//                } else if ( "level".equals(sortBy) ) {
//                    v1 = audit1.getLevel().intValue();
//                    v2 = audit2.getLevel().intValue();
//                } else if ( "message".equals(sortBy) ) {
//                    v1 = audit1.getMessage();
//                    v2 = audit2.getMessage();
//                }
//
//                return asc ?
//                    v1.compareTo(v2) :
//                    v2.compareTo(v1) ;
//            }
//
//        });
//        return list.subList(start, end).iterator();
//    }

}
