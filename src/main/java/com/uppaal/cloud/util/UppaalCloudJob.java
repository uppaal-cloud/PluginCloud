package com.uppaal.cloud.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UppaalCloudJob {
    public String name;
    public String description;
    public String xml;
    public String _id;
    public String status;
    public List<UppaalCloudJobQuery> queries;
    public UppaalCloudJobUsage usage;
    public Date start_time;
    public Date end_time;
}

class UppaalCloudJobsResponse extends ArrayList<UppaalCloudJob> {}