package com.l7tech.gui.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DocumentSizeFilterTest {
    @Mock
    private DocumentFilter.FilterBypass filterBypass;

    @Mock
    private AttributeSet attributeSet;

    @Mock
    private Document document;

    @Before
    public void setup() {
        when(filterBypass.getDocument()).thenReturn(document);
    }

    /**
     * DE279171 - Test that passing a null str param does not cause NPE
     * @throws Exception
     */
    @Test
    public void testReplaceWithNull() throws Exception {
        DocumentSizeFilter documentSizeFilter = new DocumentSizeFilter(10);
        documentSizeFilter.replace(filterBypass, 0, 1, null, attributeSet);
    }
}
