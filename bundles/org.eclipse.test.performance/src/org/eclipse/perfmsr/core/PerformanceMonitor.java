/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.perfmsr.core;


/**
 * Some natives for PerformanceMonitorWindows.
 * (We cannot fold them into the PerformanceMonitorWindows because this would break the natives
 * and we don't want to rebuild them yet).
 */
public class PerformanceMonitor {
    	
	/** 
	 * ivjperf - name of the library that implements the native methods.
	 */
	private static final String NATIVE_LIBRARY_NAME= "ivjperf";
	
	/**
	 * Is the native library loaded? 0-don't know, 1-no, 2-yes
	 */
	private static int fgIsLoaded= 0;
	
	private boolean fgNativeGetPerformanceInfoNotAvailable;

	/**
	 * Answer true if the native library for this class has been successfully
	 * loaded. If the load has not been attempted yet, try to load it.
	 */
	public static boolean isLoaded() {
		if (fgIsLoaded == 0) {
			try {
				System.loadLibrary(NATIVE_LIBRARY_NAME);
				fgIsLoaded= 2;
			} catch (Throwable e) {
			    System.err.println("The DLL " + NATIVE_LIBRARY_NAME + " could not be loaded");
			    fgIsLoaded= 1;
			}
		}
		return fgIsLoaded == 2;
	}
	
	/**
	 * Calls the Windows GetPerformanceInfo function
	 * @param counters any array of counters that corresponds to the Windows
	 * PERFORMANCE_INFORMATION structure.
	 */
	public static native void nativeGetPerformanceInfo(long[] counters);
	
	public static native boolean nativeGetPerformanceCounters(long[] counters);
	
	public static native String nativeGetUUID();
}
