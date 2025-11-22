package com.example.duan1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.duan1.model.Response;
import com.example.duan1.model.User;
import com.example.duan1.services.ApiServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Dangnhap extends AppCompatActivity {
    EditText edtEmail,edtPass;
    Button btnLogin;
    TextView tvDangki;
    private ApiServices api;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dangnhap);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiServices.Url) // nhớ "/" cuối
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(ApiServices.class);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvDangki = findViewById(R.id.tvReg);
        tvDangki.setOnClickListener(v -> {
            Intent intent = new Intent(Dangnhap.this, Dangki.class);
            startActivityForResult(intent, 1);
        });
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        // Load thông tin nếu có
        loadUserInfo();

        btnLogin.setOnClickListener(v -> {

            String email = edtEmail.getText().toString().trim();
            String password = edtPass.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            api.login(body).enqueue(new Callback<Response<User>>() {
                @Override
                public void onResponse(Call<Response<User>> call, retrofit2.Response<Response<User>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Response<User> res = response.body();

                        if (res.getData() != null) {
                            User user = res.getData();

                            // Lưu token + user info
                            SharedPreferences sharedPref = getSharedPreferences("UserData", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("token", res.getToken());
                            editor.putString("refreshToken", res.getRefreshToken());
                            editor.putString("email", user.getEmail());
                            editor.putString("password", user.getPass()); // Lưu mật khẩu
                            editor.putString("sdt", user.getPhone());
                            editor.putString("name", user.getName());
                            editor.putString("id_taikhoan", user.getId());
                            editor.apply();
                            saveUserInfo(email, password);

                            Toast.makeText(Dangnhap.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                            // Chuyển sang màn hình chính
                            Intent intent = new Intent(Dangnhap.this, ManchinhAdmin.class);
                            startActivity(intent);
                            finish(); // đóng màn hình login
                        } else {
                            Toast.makeText(Dangnhap.this, "Đăng nhập thất bại: " + res.getMessenger(), Toast.LENGTH_SHORT).show();
                            Log.e("Lỗi", response.toString());

                        }
                    } else {
                        String errorBody = response.errorBody() != null ? response.errorBody().toString() : "Không có thông tin lỗi";
                        Log.e("Lỗi", errorBody);
                        Toast.makeText(Dangnhap.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Response<User>> call, Throwable t) {
                    Log.e("Lỗi",  t.toString());
                    Toast.makeText(Dangnhap.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });


        });


    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String username = data.getStringExtra("email");
            String password = data.getStringExtra("password");
            edtEmail.setText(username);
            edtPass.setText(password);
        }
    }
    private void saveUserInfo(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.apply();
    }

    private void loadUserInfo() {
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPass = sharedPreferences.getString("password", "");
        edtEmail.setText(savedEmail);
        edtPass.setText(savedPass);
    }
}