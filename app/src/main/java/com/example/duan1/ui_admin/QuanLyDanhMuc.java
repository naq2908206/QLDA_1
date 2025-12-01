package com.example.duan1.ui_admin;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duan1.AdapterAdmin.DanhMucAdapter;
import com.example.duan1.R;
import com.example.duan1.model.DanhMuc;
import com.example.duan1.model.Response;
import com.example.duan1.services.ApiServices;
import com.example.duan1.utils.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class QuanLyDanhMuc extends AppCompatActivity {

    private RecyclerView rvDanhMuc;
    private DanhMucAdapter adapter;
    private List<DanhMuc> danhMucList;
    private TextInputEditText edtSearchDanhMuc;
    private FloatingActionButton fabAddDanhMuc;
    private ImageView imgBack;
    private ApiServices apiServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_muc);

        // Khởi tạo Retrofit
        apiServices = RetrofitClient.getInstance().getApiServices();

        // Ánh xạ views
        rvDanhMuc = findViewById(R.id.rvDanhMuc);
        edtSearchDanhMuc = findViewById(R.id.edtSearchDanhMuc);
        fabAddDanhMuc = findViewById(R.id.fabAddDanhMuc);
        imgBack = findViewById(R.id.imgBack);

        // Xử lý nút back
        imgBack.setOnClickListener(v -> finish());

        // Khởi tạo danh sách
        danhMucList = new ArrayList<>();

        // Setup RecyclerView
        adapter = new DanhMucAdapter(this, danhMucList);
        rvDanhMuc.setLayoutManager(new LinearLayoutManager(this));
        rvDanhMuc.setAdapter(adapter);

        // Load danh mục từ API
        loadCategories();


        // Search
        edtSearchDanhMuc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Xử lý Edit/Delete/View Detail
        adapter.setOnItemClickListener(new DanhMucAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                DanhMuc danhMuc = danhMucList.get(position);
                showChiTietDanhMucDialog(danhMuc);
            }

            @Override
            public void onEditClick(int position) {
                DanhMuc danhMuc = danhMucList.get(position);
                showEditDanhMucDialog(danhMuc, position);
            }

            @Override
            public void onDeleteClick(int position) {
                DanhMuc danhMuc = danhMucList.get(position);
                deleteCategory(danhMuc.getId(), position);
            }
        });

        // Thêm danh mục
        fabAddDanhMuc.setOnClickListener(v -> showAddDanhMucDialog());
    }

    private void showAddDanhMucDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_danh_muc);

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        EditText edtName = dialog.findViewById(R.id.edtTenDanhMuc);
        EditText edtDescription = dialog.findViewById(R.id.edtMoTa);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            createCategory(name, description, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditDanhMucDialog(DanhMuc danhMuc, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_danh_muc);

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        EditText edtName = dialog.findViewById(R.id.edtTenDanhMuc);
        EditText edtDescription = dialog.findViewById(R.id.edtMoTa);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        edtName.setText(danhMuc.getName());
        edtDescription.setText(danhMuc.getDescription());
        btnAdd.setText("Cập nhật");

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            updateCategory(danhMuc.getId(), name, description, position, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showChiTietDanhMucDialog(DanhMuc danhMuc) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chi_tiet_danh_muc);

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        TextView tvTen = dialog.findViewById(R.id.tvChiTietTen);
        TextView tvMoTa = dialog.findViewById(R.id.tvChiTietMoTa);
        TextView tvId = dialog.findViewById(R.id.tvChiTietId);
        Button btnDong = dialog.findViewById(R.id.btnDong);

        tvTen.setText(danhMuc.getName() != null ? danhMuc.getName() : "Chưa có tên");
        tvMoTa.setText(danhMuc.getDescription() != null && !danhMuc.getDescription().isEmpty()
                ? danhMuc.getDescription() : "Chưa có mô tả");
        tvId.setText(danhMuc.getId() != null ? danhMuc.getId() : "Chưa có ID");

        btnDong.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadCategories() {
        apiServices.getAllCategories().enqueue(new Callback<Response<List<DanhMuc>>>() {
            @Override
            public void onResponse(Call<Response<List<DanhMuc>>> call, retrofit2.Response<Response<List<DanhMuc>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<DanhMuc>> res = response.body();
                    Log.d("API Response", "Success: " + res.isSuccess() + ", Data: " + (res.getData() != null ? res.getData().size() : "null"));
                    if (res.isSuccess() && res.getData() != null && !res.getData().isEmpty()) {
                        danhMucList.clear();
                        danhMucList.addAll(res.getData());
                        adapter.updateList(danhMucList);
                        Log.d("API", "Loaded " + danhMucList.size() + " categories");
                    } else {
                        String message = res.getMessage() != null ? res.getMessage() : "Không có danh mục nào";
                        Toast.makeText(QuanLyDanhMuc.this, message, Toast.LENGTH_SHORT).show();
                        Log.d("API", "No categories: " + message);
                    }
                } else {
                    String errorMsg = "Lỗi khi tải danh mục";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(QuanLyDanhMuc.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("API Error", "Response code: " + response.code() + ", Message: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Response<List<DanhMuc>>> call, Throwable t) {
                Toast.makeText(QuanLyDanhMuc.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Failure: " + t.getMessage(), t);
            }
        });
    }

    private void createCategory(String name, String description, Dialog dialog) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);

        apiServices.createCategory(body).enqueue(new Callback<Response<DanhMuc>>() {
            @Override
            public void onResponse(Call<Response<DanhMuc>> call, retrofit2.Response<Response<DanhMuc>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<DanhMuc> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Load lại danh sách từ API
                        loadCategories();
                    } else {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLyDanhMuc.this, "Lỗi khi tạo danh mục", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<DanhMuc>> call, Throwable t) {
                Toast.makeText(QuanLyDanhMuc.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }

    private void updateCategory(String id, String name, String description, int position, Dialog dialog) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("description", description);

        apiServices.updateCategory(id, body).enqueue(new Callback<Response<DanhMuc>>() {
            @Override
            public void onResponse(Call<Response<DanhMuc>> call, retrofit2.Response<Response<DanhMuc>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<DanhMuc> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Load lại danh sách từ API
                        loadCategories();
                    } else {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLyDanhMuc.this, "Lỗi khi cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<DanhMuc>> call, Throwable t) {
                Toast.makeText(QuanLyDanhMuc.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }

    private void deleteCategory(String id, int position) {
        apiServices.deleteCategory(id).enqueue(new Callback<Response<Void>>() {
            @Override
            public void onResponse(Call<Response<Void>> call, retrofit2.Response<Response<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<Void> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        // Load lại danh sách từ API
                        loadCategories();
                    } else {
                        Toast.makeText(QuanLyDanhMuc.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLyDanhMuc.this, "Lỗi khi xóa danh mục", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<Void>> call, Throwable t) {
                Toast.makeText(QuanLyDanhMuc.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }
}

