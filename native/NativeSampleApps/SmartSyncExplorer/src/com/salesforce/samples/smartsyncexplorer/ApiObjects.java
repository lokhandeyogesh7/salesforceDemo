package com.salesforce.samples.smartsyncexplorer;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;

import org.json.JSONObject;

import java.util.HashMap;

@Entity
public class ApiObjects {

    @PrimaryKey(autoGenerate = true)
    private int slNo;
    private String objectType;
    private String fieldList;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getFieldList() {
        return fieldList;
    }

    public void setFieldList(String fieldList) {
        this.fieldList = fieldList;
    }

    public int getSlNo() {
        return slNo;
    }

    public void setSlNo(int slNo) {
        this.slNo = slNo;
    }

}
