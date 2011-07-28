package com.l7tech.policy.assertion;

import static com.l7tech.policy.assertion.VariableUseSupport.expressions;
import static com.l7tech.policy.assertion.VariableUseSupport.variables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import static com.l7tech.util.Functions.map;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;

/**
 * Unit tests for VariableUseSupport
 */
public class VariableUseSupportTest {

    @Test
    public void testVariablesUsed() {
        assertArrayEquals( "Expressions used",
                new String[]{ "a", "b", "c", "d" },
                expressions( "${a} ${A}", "${b} ${c}" )
                        .withExpressions( "${d}" )
                        .asArray() );

        assertArrayEquals( "Expressions used nulls",
                new String[]{ "a", "b", "c", "d" },
                expressions( "${a} ${A}", "${b} ${c}" )
                        .withExpressions( (String[]) null )
                        .withExpressions( (Iterable<String>) null )
                        .withExpressions( new String[]{ null } )
                        .withExpressions( "${d}" ).asArray() );

        assertArrayEquals( "Variables used",
                new String[]{ "a", "b", "c", "d" },
                variables( "a", "A", "b", "c" )
                        .withVariables( "d" )
                        .asArray() );

        assertArrayEquals( "Variables used nulls",
                new String[]{ "a", "b", "c", "d" },
                variables( "a", "A", "b", "c" )
                        .withVariables( (String[])null )
                        .withVariables( (Iterable<String>)null )
                        .withVariables( new String[]{ null } )
                        .withVariables( "d" ).asArray() );

        assertArrayEquals( "Other variables used",
                new String[]{ "a", "b", "c", "d" },
                variables( "a" )
                        .with( variables( "b", "c", "d" ) )
                        .asArray() );

        assertArrayEquals( "Other variables used nulls",
                new String[]{ "a", "b", "c", "d" },
                variables( "a" )
                        .with( null )
                        .with( variables( "b", "c", "d" ) )
                        .asArray() );
    }

    @Test
    public void testVariablesSet() {
        assertArrayEquals( "Variables used",
                new String[]{ "a", "b", "c", "d" },
                names( variables( meta( "a" ), meta( "b" ), meta( "c" ) )
                        .withVariables( meta( "d" ) )
                        .asArray() ));

        assertArrayEquals( "Variables used nulls",
                new String[]{ "a", "b", "c", "d" },
                names( variables( meta( "a" ), meta( "b" ), meta( "c" ) )
                        .withVariables( (VariableMetadata[]) null )
                        .withVariables( new VariableMetadata[]{ null } )
                        .withVariables( meta( "d" ) ).asArray() ) );

        assertArrayEquals( "Other variables used",
                new String[]{ "a", "b", "c", "d" },
                names( variables( meta( "a" ) )
                        .with( variables( meta( "b" ), meta( "c" ), meta( "d" ) ) )
                        .asArray() ) );

        assertArrayEquals( "Other variables used nulls",
                new String[]{ "a", "b", "c", "d" },
                names( variables( meta( "a" ) )
                        .with( null )
                        .with( variables( meta( "b" ), meta( "c" ), meta( "d" ) ) )
                        .asArray() ) );
    }

    private VariableMetadata meta( final String name ) {
        return new VariableMetadata( name );
    }

    /**
     * Get names from VariableMetadata since it does not define equality
     */
    private String[] names( final VariableMetadata[] variableMetadatas ) {
        return map(
                Arrays.asList( variableMetadatas ),
                Functions.<String,VariableMetadata>propertyTransform( VariableMetadata.class, "name" ) )
                .toArray(new String[0]);
    }
}
