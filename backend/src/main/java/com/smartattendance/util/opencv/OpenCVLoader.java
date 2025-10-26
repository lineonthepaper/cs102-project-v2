package com.smartattendance.util.opencv;

import nu.pattern.OpenCV;

/**
 * OpenCV loader using OpenPnP's cross-platform native library loader.
 * Automatically handles native libraries for Windows, macOS (Intel & Apple Silicon), and Linux.
 */
public class OpenCVLoader {
    private static boolean loaded = false;

    static {
        if (!loaded) {
            try {
                // OpenPnP's loader automatically detects OS and architecture
                // and loads the correct native library
                OpenCV.loadLocally();
                loaded = true;
                System.out.println("OpenCV library loaded successfully for " + 
                                   System.getProperty("os.name") + " " + 
                                   System.getProperty("os.arch"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load OpenCV native library", e);
            }
        }
    }

    public static void init() {
        // triggers static block
        // supposed to be empty
    }
    
    public static boolean isLoaded() {
        return loaded;
    }
}
