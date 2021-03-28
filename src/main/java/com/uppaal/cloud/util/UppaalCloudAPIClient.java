package com.uppaal.cloud.util;

import java.util.List;
import okhttp3.*;
import com.google.gson.Gson;

public class UppaalCloudAPIClient {
    private String email;
    private String password;
    private String token;
    private String lastPushedJob;
    private Gson gson = new Gson();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    public static final String API_URL = "http://127.0.0.1:8080";

    private OkHttpClient client = new OkHttpClient();

    public UppaalCloudAPIClient(String email, String pass) {
        // ? Read and save token in a file
        this.email = email;
        this.password = pass;
    }

    public UppaalCloudAPIClient(String token) {
        this.token = token;
    }

    public UppaalCloudAPIClient() {
    }

    public void setCredentials(String email, String pass) {
        this.email = email;
        this.password = pass;
    }

    public String getToken() {
        return this.token;
    }
    public String getEmail() {
        return this.email;
    }

    public void login() throws Exception {
        // HTTP request to login and get token
        // if token exists, verify it
        // else login and get new token

        LoginCredentials cred = new LoginCredentials(this.email, this.password);
        RequestBody body = RequestBody.create(gson.toJson(cred), JSON);
        Request request = new Request.Builder()
                .url(API_URL+"/v1/auth/login")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        String serverRsp = response.body().string();
        LoginResponse rsp = gson.fromJson(serverRsp, LoginResponse.class);

        if (response.code() != 200) {
            throw new Exception(rsp.message);
        } else {
            this.token = rsp.token;
        }
    }

    public List<UppaalCloudJob> getJobs() {
        // return list of CloudJob types
        List<UppaalCloudJob> res = java.util.Collections.emptyList();
        try {
            Request request = new Request.Builder()
                    .url(API_URL+"/v1/job")
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
        RequestBody body = RequestBody.create(gson.toJson(job), JSON);
        try {
            Request request = new Request.Builder()
                    .url(API_URL+"/v1/job")
                    .addHeader("X-Access-Token", this.token)
                    .post(body)
                    .build();

            Response response = client.newCall(request).execute();
            // Would probably have the extra quotes
            this.lastPushedJob = response.body().string();
        } catch(Exception e) {
        }

        return this.lastPushedJob;
    }
}

class LoginCredentials {
    String email;
    String password;

    public LoginCredentials(String email, String pass) {
        this.email = email;
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