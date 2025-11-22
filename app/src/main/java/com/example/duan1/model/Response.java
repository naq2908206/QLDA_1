package com.example.duan1.model;

public class Response<T> {
    private int status;
    private String messenger;
    private T data;
    private  String token;
    private  String refreshToken;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Response() {
    }

    public Response(int status, String refreshToken, String token, T data, String messenger) {
        this.status = status;
        this.refreshToken = refreshToken;
        this.token = token;
        this.data = data;
        this.messenger = messenger;
    }

    // Getter & Setter
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessenger() {
        return messenger;
    }

    public void setMessenger(String messenger) {
        this.messenger = messenger;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
