package com.uppaal.cloud.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.google.gson.Gson;

public class UppaalCloudAPIClient {
    private String username;
    private String password;
    private String token;
    private Gson gson = new Gson();

    public UppaalCloudAPIClient(String user, String pass) {
        // ? Read and save token in a file
        this.username = user;
        this.password = pass;
    }

    public UppaalCloudAPIClient(String token) {
        this.token = token;
    }

    public UppaalCloudAPIClient() {
    }

    public String login() {
        // HTTP request to login and get token
        // if token exists, verify it
        // else login and get new token
        String sth = "{\"status\": 200, \"logged\": true, \"token\":\"1234567abcd\", \"message\":\"Sign in successful\"}";
        LoginResponse rsp = gson.fromJson(sth, LoginResponse.class);
        return rsp.message + rsp.token;
    }

    public List<UppaalCloudJob> getJobs() {
        // return list of CloudJob types
        return java.util.Collections.emptyList();
    }

    public boolean pushJob(UppaalCloudJob job) {
        // Push a job to the cloud
        return true;
    }
}

class LoginResponse {
    String status;
    boolean logged;
    String token;
    String message;

    public LoginResponse() {

    }

}