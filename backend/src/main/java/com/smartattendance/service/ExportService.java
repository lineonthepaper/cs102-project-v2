package com.smartattendance.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void exportSectionAsXLSX(int section){
        List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM courses");
        for(Map<String, Object> row : data){
            System.out.println(row);
        }
    }
    
}