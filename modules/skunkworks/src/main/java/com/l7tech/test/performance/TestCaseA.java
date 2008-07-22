/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.test.performance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A sample JUnit test case for testing the performance test framework.
 */
public class TestCaseA extends TestCase {

    private static int nextId = 1;
    private final int id = nextId;
    {
        ++ nextId;
    }

    private static void setUpClass() {
        System.out.println("             ***** TestCaseA.setUpClass()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    private static void tearDownClass() {
        System.out.println("             ***** TestCaseA.tearDownClass()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    private void setUpInstance() {
        System.out.println("             ***** TestCaseA.setUpInstance()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    private void tearDownInstance() {
        System.out.println("             ***** TestCaseA.tearDownInstance()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    public TestCaseA() {
        System.out.println("             ***** TestCaseA{instance #" + id + "}.TestCaseA()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    @Override
    protected void setUp() throws java.lang.Exception {
        System.out.println("             ***** TestCaseA{instance #" + id + "}.setUp()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    @Override
    protected void tearDown() throws java.lang.Exception {
        System.out.println("             ***** TestCaseA{instance #" + id + "}.tearDown()");
        try { Thread.sleep(150); } catch (InterruptedException e) {}
    }

    public static Test suite() {
        System.out.println("             ***** TestCaseA.suite()");
        return new TestSuite(TestCaseA.class);
    }

    public void testX() {
        System.out.println("             ***** TestCaseA{instance #" + id + "}.testX()");
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

    public void testY() {
        System.out.println("             ***** TestCaseA{instance #" + id + "}.testY()");
        try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}