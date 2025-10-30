package com.smartattendance.util.converter;

import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting between base64 strings and byte arrays.
 * Follows Single Responsibility Principle - handles only image data conversion.
 * 
 * This class cannot be instantiated (utility class pattern).
 */
public final class ImageConverter {
    
    private ImageConverter() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Converts a base64 encoded string to a byte array.
     * Handles data URL prefixes (e.g., "data:image/jpeg;base64,").
     * 
     * @param base64String the base64 encoded string (may include data URL prefix)
     * @return byte array representation of the image
     * @throws IllegalArgumentException if the string is null or invalid base64
     */
    public static byte[] base64ToBytes(String base64String) {
        if (base64String == null || base64String.isBlank()) {
            throw new IllegalArgumentException("Base64 string cannot be null or empty");
        }
        
        // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
        String base64Data = base64String;
        if (base64String.contains(",")) {
            base64Data = base64String.substring(base64String.indexOf(",") + 1);
        }
        
        try {
            return Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 encoded string: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a byte array to a base64 encoded string.
     * 
     * @param imageBytes the byte array representation of the image
     * @return base64 encoded string
     * @throws IllegalArgumentException if the byte array is null or empty
     */
    public static String bytesToBase64(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty");
        }
        
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    /**
     * Converts a list of base64 encoded strings to a list of byte arrays.
     * 
     * @param base64Strings list of base64 encoded strings
     * @return list of byte arrays
     * @throws IllegalArgumentException if the list contains invalid base64 strings
     */
    public static List<byte[]> base64ListToBytesList(List<String> base64Strings) {
        if (base64Strings == null) {
            return new ArrayList<>();
        }
        
        List<byte[]> bytesList = new ArrayList<>();
        for (String base64String : base64Strings) {
            bytesList.add(base64ToBytes(base64String));
        }
        return bytesList;
    }
    
    /**
     * Converts a list of byte arrays to a list of base64 encoded strings.
     * 
     * @param bytesList list of byte arrays
     * @return list of base64 encoded strings
     * @throws IllegalArgumentException if the list contains null or empty byte arrays
     */
    public static List<String> bytesListToBase64List(List<byte[]> bytesList) {
        if (bytesList == null) {
            return new ArrayList<>();
        }
        
        List<String> base64List = new ArrayList<>();
        for (byte[] imageBytes : bytesList) {
            base64List.add(bytesToBase64(imageBytes));
        }
        return base64List;
    }
}

