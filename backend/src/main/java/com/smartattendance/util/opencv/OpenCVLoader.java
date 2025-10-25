package com.smartattendance.util.opencv;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OpenCVLoader {
    private static boolean loaded = false;

    static {
        if (!loaded) {
            try {
                String osName = System.getProperty("os.name").toLowerCase();
                String libPath;

                if (osName.contains("win"))
                    libPath = "/native/opencv_java4120.dll";
                else if (osName.contains("mac"))
                    libPath = "/native/libopencv_java4120.dylib";
                else
                    libPath = "/native/libopencv_java4120.so";

                try (InputStream in = OpenCVLoader.class.getResourceAsStream(libPath)) {
                    Path tempLib = Files.createTempFile("opencv", libPath.substring(libPath.lastIndexOf('.')));
                    Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);
                    System.load(tempLib.toAbsolutePath().toString());
                }

                loaded = true;
                System.out.println("OpenCV library loaded.");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load OpenCV native library", e);
            }
        }
    }

    public static void init() {
        // triggers static block
        // supposed to be empty
    }
}
