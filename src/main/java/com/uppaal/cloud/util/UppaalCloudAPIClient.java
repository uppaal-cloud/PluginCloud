package com.uppaal.cloud.util;

import java.util.List;
import okhttp3.*;
import com.google.gson.Gson;

public class UppaalCloudAPIClient {
    private String username;
    private String password;
    private String token;
    private Gson gson = new Gson();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    public static final String API_URL = "http://127.0.0.1:3000";

    private OkHttpClient client = new OkHttpClient();

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

    public void setCredentials(String user, String pass) {
        this.username = user;
        this.password = pass;
    }

    public String getToken() {
        return this.token;
    }

    public boolean login() {
        // HTTP request to login and get token
        // if token exists, verify it
        // else login and get new token
        boolean logged = true;

        try {
            LoginCredentials cred = new LoginCredentials(this.username, this.password);
            RequestBody body = RequestBody.create(gson.toJson(cred), JSON);
            Request request = new Request.Builder()
                    .url(API_URL+"/auth/login")
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            String serverRsp = response.body().string();

            LoginResponse rsp = gson.fromJson(serverRsp, LoginResponse.class);
            this.token = rsp.token;
        } catch(Exception e) {
            logged = false;
        }

        return logged;
    }

    public List<UppaalCloudJob> getJobs() {
        // return list of CloudJob types
        List<UppaalCloudJob> res = java.util.Collections.emptyList();
        try {
            Request request = new Request.Builder()
                    .url(API_URL+"/job")
                    .addHeader("X-Access-Token", this.token)
                    .build();

            Response response = client.newCall(request).execute();
            String serverRsp = response.body().string();

            res = gson.fromJson(serverRsp, UppaalCloudJobsResponse.class);
        } catch(Exception e) {
        }

        return res;
    }

    public String pushJob(UppaalCloudJob job) {
        // Push a job to the cloud
        String jobId = "UNKNOWN";
        RequestBody body = RequestBody.create(gson.toJson(job), JSON);
        try {
            Request request = new Request.Builder()
                    .url(API_URL+"/job")
                    .addHeader("X-Access-Token", this.token)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            // Would probably have the extra quotes
            jobId = response.body().string();
        } catch(Exception e) {
        }

        return jobId;
    }
}

class LoginCredentials {
    String email;
    String password;

    public LoginCredentials(String user, String pass) {
        this.email = user;
        this.password = pass;
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