package com.smartattendance.entity;

import org.apache.poi.ss.usermodel.Sheet;

public class XLSXSheetEntry {
    private Sheet sheet;
    private int currentRowIndex;

    public XLSXSheetEntry(Sheet sheet, int currentRowIndex) {
        this.sheet = sheet;
        this.currentRowIndex = currentRowIndex;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public int getCurrentRowIndex() {
        return currentRowIndex;
    }
    
    public void setCurrentRowIndex(int currentRowIndex) {
        this.currentRowIndex = currentRowIndex;
    }
}
