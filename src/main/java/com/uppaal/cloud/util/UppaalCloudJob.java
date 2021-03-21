package com.uppaal.cloud.util;

public class UppaalCloudJob {
    String name;
    String description;
    String xml;

    public UppaalCloudJob(String name, String description, String xml) {
        this.name = name;
        this.description = description;
        this.xml = xml;
    }
}