package com.example.duan1.services;




import com.example.duan1.model.Response;
import com.example.duan1.model.User;

import java.util.ArrayList;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface ApiServices {

    public  String Url="http://192.168.0.114:3000/";
    @POST("api/register")
    Call<Response<User>> register(@Body Map<String, String> body);

    @POST("api/login")
    Call<Response<User>> login(@Body Map<String, String> body);

}
