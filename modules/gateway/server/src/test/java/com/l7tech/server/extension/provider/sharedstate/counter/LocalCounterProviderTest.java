package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;
import com.l7tech.util.Config;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class LocalCounterProviderTest {
    
    private LocalCounterProvider underTest;

    @Before
    public void setup() {
        underTest = new LocalCounterProvider();
    }

    @Test
    public void getCounterStore() {
        SharedCounterStore sc = underTest.getCounterStore("test");
        assertNotNull(sc);
    }
    
    @Test
    public void getExistingCounterStore() {
        SharedCounterStore sc = underTest.getCounterStore("test");
        SharedCounterStore scRefetch = underTest.getCounterStore("test");
        assertNotNull(scRefetch);
    }
}
