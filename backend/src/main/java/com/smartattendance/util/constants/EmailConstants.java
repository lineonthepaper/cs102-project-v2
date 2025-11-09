package com.smartattendance.util.constants;

import org.springframework.beans.factory.annotation.Value;

public class EmailConstants {
    public static final String EMAILER_ADDRESS = "";
    public static final String EMAILER_HOST = "";
    public static final String EMAILER_PASSWORD = "";
    public static final String EMAILER_PORT = "587";

    private EmailConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
