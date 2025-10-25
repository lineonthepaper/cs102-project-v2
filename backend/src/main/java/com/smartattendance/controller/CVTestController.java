package com.smartattendance.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartattendance.entity.ComparisonResult;
import com.smartattendance.service.FaceRecognitionService;


@RestController
public class CVTestController {
    FaceRecognitionService recognizer;

    public CVTestController(FaceRecognitionService recognizer){
        this.recognizer = recognizer;

        // call this to load the OpenCV CascadeClassifier and DNNS
        recognizer.loadRecognitionModels();
    }

    // form field name must match parameter name
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ComparisonResult handleImage(@RequestParam MultipartFile image) throws Exception {
        
        byte[] bytes = image.getBytes();
        ComparisonResult result = recognizer.matchFace(bytes);
        return result;
    }

}
