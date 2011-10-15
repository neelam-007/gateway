package com.l7tech.common.log;

import static com.l7tech.common.log.HybridDiagnosticContext.*;
import static com.l7tech.util.CollectionUtils.list;
import com.l7tech.util.Functions.Nullary;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for HybridDiagnosticContext
 */
public class HybridDiagnosticContextTest {

    @Test
    public void testMappedDiagnosticContext() {
        put( "test", "value" );
        assertEquals( "test value get 1", "value", getFirst( "test" ) );
        assertEquals( "test value get 2", list("value"), get( "test" ) );
        remove( "test" );
        assertNull( "test value get 3", getFirst( "test" ) );
        assertNull( "test value get 4", get( "test" ) );

        put( "test", list("value1", "value2") );
        assertEquals( "test multi-value get 1", "value1", getFirst( "test" ) );
        assertEquals( "test multi-value get 2", list("value1", "value2"), get( "test" ) );
        remove( "test" );
        assertNull( "test multi-value get 3", getFirst( "test" ) );
        assertNull( "test multi-value get 4", get( "test" ) );
    }

    @Test
    public void testNestedDiagnosticContext() {
        final String key = "nested";

        doInContext( key, "1", new Nullary<Void>(){
            @Override
            public Void call() {
                assertEquals( "test value get 1", "1", getFirst( key ) );
                assertEquals( "test value getAll 1", list( "1" ), getAll( key ) );

                doInContext( key, list("2", "3"), new Nullary<Void>(){
                    @Override
                    public Void call() {
                        assertEquals( "test value get 2", "2", getFirst( key ) );
                        assertEquals( "test value get 3", list("2","3"), get( key ) );
                        assertEquals( "test value getAll 2", list( "1", "2", "3" ), getAll( key ) );
                        return null;
                    }
                } );

                assertEquals( "test value get 1", "1", getFirst( key ) );
                assertEquals( "test value getAll 1", list( "1" ), getAll( key ) );

                return null;
            }
        } );
    }

    @Test
    public void testSaveAndRestore() {
        put( "test1", "value1" );
        put( "test2", "value2" );
        put( "test3", "value3" );
        final SavedDiagnosticContext saved = save();
        reset();

        assertNull( "test1 value get 1", getFirst( "test1" ) );
        assertNull( "test2 value get 1", getFirst( "test2" ) );
        assertNull( "test3 value get 1", getFirst( "test3" ) );

        doWithContext( saved, new Nullary<Void>(){
            @Override
            public Void call() {
                assertEquals( "test1 value get 2", "value1", getFirst( "test1" ) );
                assertEquals( "test2 value get 2", "value2", getFirst( "test2" ) );
                assertEquals( "test3 value get 2", "value3", getFirst( "test3" ) );
                return null;
            }
        } );

        assertNull( "test1 value get 3", getFirst( "test1" ) );
        assertNull( "test2 value get 3", getFirst( "test2" ) );
        assertNull( "test3 value get 3", getFirst( "test3" ) );

        restore( saved );

        assertEquals( "test1 value get 4", "value1", getFirst( "test1" ) );
        assertEquals( "test2 value get 4", "value2", getFirst( "test2" ) );
        assertEquals( "test3 value get 4", "value3", getFirst( "test3" ) );
    }
}
