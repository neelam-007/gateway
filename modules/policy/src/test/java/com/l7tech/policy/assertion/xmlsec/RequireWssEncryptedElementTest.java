package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.xml.XencUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 *
 */
public class RequireWssEncryptedElementTest {
    RequireWssEncryptedElement ass = new RequireWssEncryptedElement();

    @Test
    public void testDefaultValues() {
        assertEquals(XencUtil.AES_128_CBC, ass.getXEncAlgorithm());
        assertNull(ass.getXEncAlgorithmList());
    }

    @Test
    public void testPreferredCipher() {
        assertEquals(XencUtil.AES_128_CBC, ass.getXEncAlgorithm());
        assertNull(ass.getXEncAlgorithmList());

        ass.setXEncAlgorithm(XencUtil.AES_192_CBC);
        assertEquals(XencUtil.AES_192_CBC, ass.getXEncAlgorithm());
        assertNotNull(ass.getXEncAlgorithmList());
        assertEquals(1, ass.getXEncAlgorithmList().size());
        assertEquals(XencUtil.AES_192_CBC, ass.getXEncAlgorithmList().iterator().next());

        ass.setXEncAlgorithm(XencUtil.TRIPLE_DES_CBC);
        assertEquals(XencUtil.TRIPLE_DES_CBC, ass.getXEncAlgorithm());
        assertNotNull(ass.getXEncAlgorithmList());
        assertEquals(2, ass.getXEncAlgorithmList().size());
        final Iterator<String> it = ass.getXEncAlgorithmList().iterator();
        assertEquals(XencUtil.TRIPLE_DES_CBC, it.next());
        assertEquals(XencUtil.AES_192_CBC, it.next());
    }

    @Test
    public void testMovePreferredToTopOfList() {
        ass.setXEncAlgorithmList(Arrays.asList(XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_128_CBC));
        assertTrue(Arrays.equals(new String[] { XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));

        ass.setXEncAlgorithm(XencUtil.AES_256_CBC);
        assertTrue(Arrays.equals(new String[] { XencUtil.AES_256_CBC, XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));

        ass.setXEncAlgorithm(XencUtil.TRIPLE_DES_CBC);
        assertTrue(Arrays.equals(new String[] { XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_192_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));
    }

    @Test
    public void testPreferenceListSurvivesWsp() {
        ass.setXEncAlgorithmList(Arrays.asList(XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_128_CBC));
        assertTrue(Arrays.equals(new String[] { XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));

        ass = reserialize();

        assertTrue(Arrays.equals(new String[] { XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));
        ass.setXEncAlgorithm(XencUtil.AES_256_CBC);
        assertTrue(Arrays.equals(new String[] { XencUtil.AES_256_CBC, XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));

        ass = reserialize();

        assertTrue(Arrays.equals(new String[] { XencUtil.AES_256_CBC, XencUtil.AES_192_CBC, XencUtil.TRIPLE_DES_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));
        ass.setXEncAlgorithm(XencUtil.TRIPLE_DES_CBC);
        assertTrue(Arrays.equals(new String[] { XencUtil.TRIPLE_DES_CBC, XencUtil.AES_256_CBC, XencUtil.AES_192_CBC, XencUtil.AES_128_CBC  },
                ass.getXEncAlgorithmList().toArray(new String[ass.getXEncAlgorithmList().size()])));
    }

    private RequireWssEncryptedElement reserialize() {
        String xml = WspWriter.getPolicyXml(ass);

        try {
            Assertion got = WspReader.getDefault().parseStrictly(xml, WspReader.Visibility.omitDisabled);
            return (RequireWssEncryptedElement)got; 
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
