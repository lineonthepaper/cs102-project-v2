package com.smartattendance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CVTestPageController {

    // serves index.html file. This whole java file is temporary so the frontend 
    // can test the post and get requests
    @GetMapping("/")
    public String index(){
        return "index.html";
    }
    
    @GetMapping("/uploadimage")
    public String uploadImage(){
        return "uploadimage.html";
    }
}   
