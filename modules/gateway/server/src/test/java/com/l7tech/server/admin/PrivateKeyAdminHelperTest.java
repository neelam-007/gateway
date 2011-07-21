package com.l7tech.server.admin;

import com.l7tech.gateway.common.security.SpecialKeyType;
import static com.l7tech.util.CollectionUtils.foreach;
import static com.l7tech.util.CollectionUtils.list;
import com.l7tech.util.Functions.UnaryVoid;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Unit tests for PrivateKeyAdminHelper
 */
public class PrivateKeyAdminHelperTest {

    @Test
    public void testClusterPropertyNamesForKeyTypes() {
        foreach( list( SpecialKeyType.values() ), true, new UnaryVoid<SpecialKeyType>(){
            @Override
            public void call( final SpecialKeyType specialKeyType ) {
                assertNotNull(
                        "Property for key type " + specialKeyType,
                        PrivateKeyAdminHelper.getClusterPropertyForSpecialKeyType( specialKeyType ) );
            }
        } );
    }
}
