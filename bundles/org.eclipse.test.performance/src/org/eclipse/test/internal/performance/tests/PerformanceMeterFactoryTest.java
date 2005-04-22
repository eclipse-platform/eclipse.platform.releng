package org.eclipse.test.internal.performance.tests;

import org.eclipse.test.internal.performance.OSPerformanceMeter;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import junit.framework.TestCase;

public class PerformanceMeterFactoryTest extends TestCase {

    public void testPerformanceMeterFactory() {
		System.setProperty("PerformanceMeterFactory", "org.eclipse.test.performance:org.eclipse.test.internal.performance.OSPerformanceMeterFactory"); //$NON-NLS-1$ //$NON-NLS-2$
		
		PerformanceMeter pm= Performance.getDefault().createPerformanceMeter("scenarioId"); //$NON-NLS-1$
		
		assertTrue(pm instanceof OSPerformanceMeter);
	}
}
