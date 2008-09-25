package com.l7tech.server.util;

import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedOperation;

import javax.sql.DataSource;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.OpenDataException;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.l7tech.util.ResourceUtils;

/**
 * Enable access to DataSource via JMX.
 */
@ManagedResource(description="DataSource", objectName="l7tech:type=DataSource")
public class DataSourceExplorer {

    //- PUBLIC

    /**
     *
     */
    public DataSourceExplorer( final DataSource dataSource ) {
        this.dataSource = dataSource;    
    }

    /**
     * 
     */
    @ManagedOperation(description="Show Database Metadata")
    public TabularData listTables() throws Exception {
        Connection connection = null;
        ResultSet results = null;
        try {
            connection = dataSource.getConnection();

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            results = databaseMetaData.getTables(null, "%", "%", new String[]{"TABLE"});

            return buildTableForResults( results );           
        } finally {
            ResourceUtils.closeQuietly( results );
            ResourceUtils.closeQuietly( connection );
        }
    }

    /**
     *
     */
    @ManagedOperation(description="Show Table Metadata")
    public TabularData listTable( final String tableName ) throws Exception {
        Connection connection = null;
        ResultSet results = null;
        try {
            connection = dataSource.getConnection();

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            results = databaseMetaData.getColumns(null, "%", tableName, "%");

            return buildTableForResults( results );            
        } finally {
            ResourceUtils.closeQuietly( results );
            ResourceUtils.closeQuietly( connection );
        }
    }

    /**
     *
     */
    @ManagedOperation(description="Run Database Query")
    public TabularData query( final String query ) throws Exception {
        Connection connection = null;
        Statement statement = null;
        ResultSet results = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();            
            results = statement.executeQuery( query );

            return buildTableForResults( results );
        } finally {
            ResourceUtils.closeQuietly( results );
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }
    }

    /**
     *
     */
    @ManagedOperation(description="Run Database Update")
    public int update( final String update ) throws Exception {
        int count = 0;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            count = statement.executeUpdate( update );
        } finally {
            ResourceUtils.closeQuietly( statement );
            ResourceUtils.closeQuietly( connection );
        }

        return count;
    }


    //- PRIVATE

    /**
     * 
     */
    private final DataSource dataSource;


    /**
     * Build TabularData for a ResultSet.
     *
     * This could be enhanced to make better use of SQL types. 
     */
    private TabularData buildTableForResults( final ResultSet results ) throws SQLException, OpenDataException {
        Collection<String> names = new ArrayList<String>();
        names.add("#");
        ResultSetMetaData metaData = results.getMetaData();
        for ( int i=1; i<metaData.getColumnCount()+1; i++ ) {
            names.add( Integer.toString(i) + ":" + metaData.getColumnName(i) );
        }
        String[] nameData = names.toArray(new String[names.size()]);
        OpenType<?>[] typeData = new OpenType<?>[names.size()];
        Arrays.fill(typeData, SimpleType.STRING);
        CompositeType ctype = new CompositeType("Table", "Database Table", nameData, nameData, typeData);
        TabularType ttype = new TabularType("Database", "Database Schema", ctype, new String[]{"#"});
        TabularDataSupport data = new TabularDataSupport(ttype);

        int row = 1;
        while ( results.next() ) {
            Map<String,String> rowdata = new HashMap<String,String>();
            rowdata.put( "#", Integer.toString(row++) );
            for ( int i=1; i<metaData.getColumnCount()+1; i++ ) {
                Object value = results.getObject(i);
                rowdata.put( Integer.toString(i) + ":" + metaData.getColumnName(i), value==null ? "<NULL>" : value.toString() );
            }
            data.put( new CompositeDataSupport(ctype, rowdata) );
        }

        return data;
    }
}
