/**
 * Copyright © 2009 HERA Consulting Ltd.  
 */

package com.gnugu.secretbox.tests;

import junit.framework.Test;
import junit.framework.TestSuite;
import android.test.suitebuilder.TestSuiteBuilder;

/**
 * Test suite for all tests here.
 * @author HERA Consulting Ltd.
 *
 */
public class AllTests extends TestSuite {

	public static Test suite() {
        return new TestSuiteBuilder(AllTests.class)
                .includeAllPackagesUnderHere()
                .build();
    }
	
}
