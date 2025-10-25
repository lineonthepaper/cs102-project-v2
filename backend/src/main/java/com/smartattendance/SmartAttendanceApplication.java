package com.smartattendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.smartattendance.util.opencv.OpenCVLoader;

@SpringBootApplication
public class SmartAttendanceApplication {

    public static void main(String[] args) {
        OpenCVLoader.init();
        SpringApplication.run(SmartAttendanceApplication.class, args);
    }

}