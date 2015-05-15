package com.l7tech.external.assertions.odata.server;

import com.l7tech.external.assertions.odata.server.producer.datasource.BaseJdbc;
import com.l7tech.external.assertions.odata.server.producer.datasource.DataSourceProducer;
import com.l7tech.external.assertions.odata.server.producer.datasource.JdbcDataSource;
import com.l7tech.external.assertions.odata.server.producer.datasource.TransactionalJdbcDataSource;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModelToMetadata;
import com.l7tech.external.assertions.odata.server.producer.jdbc.LoggingCommand;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.odata4j.exceptions.ServerErrorException;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.command.ProducerCommandContext;

import javax.sql.DataSource;
import javax.ws.rs.ext.ContextResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Our custom JAX-RS ContextResolver implementation that returns our producer
 *
 * @author rraquepo, 8/22/13
 */
public class JaxRsContextResolver implements ContextResolver<ODataProducer> {
    private static final Logger logger = Logger.getLogger(JaxRsContextResolver.class.getName());
    private ODataProducer producer;
    private JdbcModelToMetadata modelToMetadata;
    private BaseJdbc jdbcHandler;

    public JaxRsContextResolver(final DataSource dataSource, final JdbcModelCache modelCache, final boolean transactional) {
        this(dataSource, modelCache, null, transactional);
    }

    public JaxRsContextResolver(final DataSource dataSource, final JdbcModelCache modelCache, final String customEntitiesString, final boolean transactional) {
        if (customEntitiesString != null && customEntitiesString.length() > 0) {
            QNameMap qmap = new QNameMap();
            qmap.setDefaultNamespace("http://ns.l7tech.com/2012/04/api-management");
            qmap.setDefaultPrefix("l7");
            StaxDriver staxDriver = new StaxDriver(qmap);
            XStream xstream = new XStream(staxDriver);
            xstream.alias("CustomEntities", java.util.List.class);
            xstream.alias("Entity", JdbcModel.JdbcTable.class);
            xstream.alias("Column", JdbcModel.JdbcColumn.class);
            xstream.alias("PrimaryKey", JdbcModel.JdbcPrimaryKey.class);
            List<JdbcModel.JdbcTable> jdbcTables = new ArrayList<>();
            try {
                jdbcTables = (List<JdbcModel.JdbcTable>) xstream.fromXML(customEntitiesString);
                for (JdbcModel.JdbcTable table : jdbcTables) {
                    if (!"VIEW".equalsIgnoreCase(table.tableType) && !"TABLE".equalsIgnoreCase(table.tableType))
                        table.tableType = "CUSTOM";
                }
            } catch (Exception e) {
                String errorMsg = "Found an Invalid CustomEntities Definition";
                throw new ServerErrorException(errorMsg);
            }
            modelToMetadata = new JdbcModelToMetadata(jdbcTables);
        } else {
            modelToMetadata = new JdbcModelToMetadata();
        }
        if (transactional) {
            jdbcHandler = new TransactionalJdbcDataSource(dataSource);
            producer = DataSourceProducer.newBuilder().jdbc(jdbcHandler, modelCache).insert(ProducerCommandContext.class, new LoggingCommand()).register(JdbcModelToMetadata.class, modelToMetadata).build();
        } else {
            jdbcHandler = new JdbcDataSource(dataSource, false);
            producer = DataSourceProducer.newBuilder().jdbc(jdbcHandler, modelCache).insert(ProducerCommandContext.class, new LoggingCommand()).register(JdbcModelToMetadata.class, modelToMetadata).build();
        }

        //TODO: if we go for JPA Producer, this is the place we put it in (after removing DataSourceProducer above ;))
    }

    @Override
    public ODataProducer getContext(Class<?> aClass) {
        return producer;
    }

    public JdbcModelToMetadata getModelToMetadata() {
        return modelToMetadata;
    }

    public void commit() {
        if (jdbcHandler != null && jdbcHandler instanceof TransactionalJdbcDataSource) {
            ((TransactionalJdbcDataSource) jdbcHandler).commit();
        }
    }

    public void rollback() {
        if (jdbcHandler != null && jdbcHandler instanceof TransactionalJdbcDataSource) {
            ((TransactionalJdbcDataSource) jdbcHandler).rollback();
        }
    }

    public void close() {
        if (jdbcHandler != null) {
            jdbcHandler.close();
        }
    }

}
