package com.example.duan1.ui_admin;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.duan1.AdapterAdmin.SanPhamAdapter;
import com.example.duan1.R;
import com.example.duan1.model.DanhMuc;
import com.example.duan1.model.Product;
import com.example.duan1.model.Response;
import com.example.duan1.services.ApiServices;
import com.example.duan1.utils.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class QuanLySanPham extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private RecyclerView rvSanPham;
    private SanPhamAdapter adapter;
    private List<Product> sanPhamList;
    private List<DanhMuc> danhMucList;
    private TextInputEditText edtSearchSanPham;
    private FloatingActionButton fabAddSanPham;
    private ImageView imgBack;
    private Uri selectedImageUri;
    private ApiServices apiServices;
    private Dialog currentDialog;
    private ImageView currentImagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_san_pham);

        // Khởi tạo Retrofit
        apiServices = RetrofitClient.getInstance().getApiServices();

        // Ánh xạ views
        rvSanPham = findViewById(R.id.rvSanPham);
        edtSearchSanPham = findViewById(R.id.edtSearchSanPham);
        fabAddSanPham = findViewById(R.id.fabAddSanPham);
        imgBack = findViewById(R.id.imgBack);

        // Xử lý nút back
        imgBack.setOnClickListener(v -> finish());

        // Khởi tạo danh sách
        sanPhamList = new ArrayList<>();
        danhMucList = new ArrayList<>();

        // Setup RecyclerView
        adapter = new SanPhamAdapter(this, sanPhamList, danhMucList);
        rvSanPham.setLayoutManager(new LinearLayoutManager(this));
        rvSanPham.setAdapter(adapter);

        // Load sản phẩm từ API
        loadProducts();
        // Load danh mục để dùng cho spinner
        loadCategories();

        // Search
        edtSearchSanPham.addTextChangedListener(new TextWatcher() {
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
        adapter.setOnItemClickListener(new SanPhamAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Lấy product từ adapter (đã xử lý filter)
                List<Product> currentList = adapter.getProductList();
                if (position >= 0 && position < currentList.size()) {
                    Product product = currentList.get(position);
                    showChiTietSanPhamDialog(product);
                }
            }

            @Override
            public void onEditClick(int position) {
                // Lấy product từ adapter (đã xử lý filter)
                List<Product> currentList = adapter.getProductList();
                if (position >= 0 && position < currentList.size()) {
                    Product product = currentList.get(position);
                    // Tìm position thực tế trong sanPhamList để hiển thị đúng trong dialog edit
                    int realPosition = -1;
                    for (int i = 0; i < sanPhamList.size(); i++) {
                        if (sanPhamList.get(i).getId().equals(product.getId())) {
                            realPosition = i;
                            break;
                        }
                    }
                    showEditSanPhamDialog(product, realPosition >= 0 ? realPosition : 0);
                }
            }

            @Override
            public void onDeleteClick(int position) {
                // Lấy product từ adapter (đã xử lý filter)
                List<Product> currentList = adapter.getProductList();
                if (position >= 0 && position < currentList.size()) {
                    Product product = currentList.get(position);
                    deleteProduct(product.getId());
                }
            }
        });

        // Thêm sản phẩm
        fabAddSanPham.setOnClickListener(v -> showAddSanPhamDialog());
    }

    private void loadProducts() {
        apiServices.getAllProducts().enqueue(new Callback<Response<List<Product>>>() {
            @Override
            public void onResponse(Call<Response<List<Product>>> call, retrofit2.Response<Response<List<Product>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<Product>> res = response.body();
                    Log.d("API Response", "Success: " + res.isSuccess() + ", Data: " + (res.getData() != null ? res.getData().size() : "null"));
                    if (res.isSuccess() && res.getData() != null && !res.getData().isEmpty()) {
                        sanPhamList.clear();
                        sanPhamList.addAll(res.getData());
                        adapter.updateList(sanPhamList);
                        Log.d("API", "Loaded " + sanPhamList.size() + " products");
                    } else {
                        String message = res.getMessage() != null ? res.getMessage() : "Không có sản phẩm nào";
                        Toast.makeText(QuanLySanPham.this, message, Toast.LENGTH_SHORT).show();
                        Log.d("API", "No products: " + message);
                    }
                } else {
                    String errorMsg = "Lỗi khi tải sản phẩm";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(QuanLySanPham.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e("API Error", "Response code: " + response.code() + ", Message: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Response<List<Product>>> call, Throwable t) {
                Toast.makeText(QuanLySanPham.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", "Failure: " + t.getMessage(), t);
            }
        });
    }

    private void loadCategories() {
        apiServices.getAllCategories().enqueue(new Callback<Response<List<DanhMuc>>>() {
            @Override
            public void onResponse(Call<Response<List<DanhMuc>>> call, retrofit2.Response<Response<List<DanhMuc>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<DanhMuc>> res = response.body();
                    if (res.isSuccess() && res.getData() != null) {
                        danhMucList.clear();
                        danhMucList.addAll(res.getData());
                        // Cập nhật danh sách danh mục vào adapter
                        if (adapter != null) {
                            adapter.setDanhMucList(danhMucList);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Response<List<DanhMuc>>> call, Throwable t) {
                Log.e("API Error", "Failed to load categories: " + t.getMessage());
            }
        });
    }

    private void showAddSanPhamDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_san_pham);
        currentDialog = dialog;

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        ImageView imgProductPreview = dialog.findViewById(R.id.imgProductPreview);
        currentImagePreview = imgProductPreview;
        Spinner spinnerDanhMuc = dialog.findViewById(R.id.spinnerDanhMuc);
        Button btnChooseImage = dialog.findViewById(R.id.btnChooseImage);
        EditText edtName = dialog.findViewById(R.id.edtTenSanPham);
        EditText edtDescription = dialog.findViewById(R.id.edtMoTa);
        EditText edtPrice = dialog.findViewById(R.id.edtGia);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        selectedImageUri = null;

        // Setup spinner danh mục với text color đen
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Chọn danh mục"); // Thêm item mặc định
        for (DanhMuc danhMuc : danhMucList) {
            categoryNames.add(danhMuc.getName());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categoryNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                textView.setTextSize(18);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                textView.setTextSize(18);
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDanhMuc.setAdapter(spinnerAdapter);
        spinnerDanhMuc.setSelection(0); // Set về "Chọn danh mục"

        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            int selectedPosition = spinnerDanhMuc.getSelectedItemPosition();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPosition == 0 || selectedPosition < 0) {
                Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                // selectedPosition - 1 vì index 0 là "Chọn danh mục"
                String categoryId = danhMucList.get(selectedPosition - 1).getId();
                createProduct(name, description, price, categoryId, dialog);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showEditSanPhamDialog(Product product, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_san_pham);
        currentDialog = dialog;

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        ImageView imgProductPreview = dialog.findViewById(R.id.imgProductPreview);
        currentImagePreview = imgProductPreview;
        Spinner spinnerDanhMuc = dialog.findViewById(R.id.spinnerDanhMuc);
        Button btnChooseImage = dialog.findViewById(R.id.btnChooseImage);
        EditText edtName = dialog.findViewById(R.id.edtTenSanPham);
        EditText edtDescription = dialog.findViewById(R.id.edtMoTa);
        EditText edtPrice = dialog.findViewById(R.id.edtGia);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        edtName.setText(product.getName());
        edtDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        edtPrice.setText(String.valueOf((long)product.getPrice()));
        btnAdd.setText("Cập nhật");

        // Setup spinner danh mục với text color đen
        List<String> categoryNames = new ArrayList<>();
        int selectedCategoryIndex = 0;
        for (int i = 0; i < danhMucList.size(); i++) {
            DanhMuc danhMuc = danhMucList.get(i);
            categoryNames.add(danhMuc.getName());
            if (product.getCategoryId() != null && product.getCategoryId().equals(danhMuc.getId())) {
                selectedCategoryIndex = i;
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categoryNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                textView.setTextSize(18);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(getResources().getColor(android.R.color.black));
                textView.setTextSize(18);
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDanhMuc.setAdapter(spinnerAdapter);
        spinnerDanhMuc.setSelection(selectedCategoryIndex);

        // Load ảnh nếu có
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            String imageUrl = product.getImage();
            if (!imageUrl.startsWith("http")) {
                imageUrl = ApiServices.Url.replace("/api/", "") + imageUrl;
            }
            Glide.with(this).load(imageUrl).into(imgProductPreview);
        }

        selectedImageUri = null;

        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String priceStr = edtPrice.getText().toString().trim();
            int selectedPosition = spinnerDanhMuc.getSelectedItemPosition();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                String categoryId = danhMucList.get(selectedPosition).getId();
                updateProduct(product.getId(), name, description, price, categoryId, position, dialog);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void createProduct(String name, String description, double price, String categoryId, Dialog dialog) {
        // Tạo RequestBody cho các field
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody descriptionBody = RequestBody.create(MediaType.parse("text/plain"), description != null ? description : "");
        RequestBody priceBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
        RequestBody categoryIdBody = RequestBody.create(MediaType.parse("text/plain"), categoryId);

        // Tạo MultipartBody.Part cho ảnh (nếu có)
        MultipartBody.Part imagePart;
        if (selectedImageUri != null) {
            try {
                File imageFile = createFileFromUri(selectedImageUri);
                if (imageFile != null && imageFile.exists()) {
                    RequestBody imageRequestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), imageRequestBody);
                } else {
                    // Tạo empty part nếu không có file
                    RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
                    imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
                }
            } catch (Exception e) {
                Log.e("Image Upload", "Error creating file from URI: " + e.getMessage());
                RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
                imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
            }
        } else {
            // Tạo empty part nếu không chọn ảnh
            RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
            imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
        }

        apiServices.createProduct(nameBody, descriptionBody, priceBody, categoryIdBody, imagePart).enqueue(new Callback<Response<Product>>() {
            @Override
            public void onResponse(Call<Response<Product>> call, retrofit2.Response<Response<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<Product> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Load lại danh sách từ API
                        loadProducts();
                    } else {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLySanPham.this, "Lỗi khi tạo sản phẩm", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<Product>> call, Throwable t) {
                Toast.makeText(QuanLySanPham.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }

    private void updateProduct(String id, String name, String description, double price, String categoryId, int position, Dialog dialog) {
        // Tạo RequestBody cho các field
        RequestBody nameBody = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody descriptionBody = RequestBody.create(MediaType.parse("text/plain"), description != null ? description : "");
        RequestBody priceBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
        RequestBody categoryIdBody = RequestBody.create(MediaType.parse("text/plain"), categoryId);

        // Tạo MultipartBody.Part cho ảnh (nếu có ảnh mới)
        MultipartBody.Part imagePart;
        if (selectedImageUri != null) {
            try {
                File imageFile = createFileFromUri(selectedImageUri);
                if (imageFile != null && imageFile.exists()) {
                    RequestBody imageRequestBody = RequestBody.create(MediaType.parse("image/*"), imageFile);
                    imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), imageRequestBody);
                } else {
                    // Tạo empty part nếu không có file
                    RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
                    imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
                }
            } catch (Exception e) {
                Log.e("Image Upload", "Error creating file from URI: " + e.getMessage());
                RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
                imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
            }
        } else {
            // Tạo empty part nếu không chọn ảnh mới
            RequestBody emptyBody = RequestBody.create(MediaType.parse("image/*"), "");
            imagePart = MultipartBody.Part.createFormData("image", "", emptyBody);
        }

        apiServices.updateProduct(id, nameBody, descriptionBody, priceBody, categoryIdBody, imagePart).enqueue(new Callback<Response<Product>>() {
            @Override
            public void onResponse(Call<Response<Product>> call, retrofit2.Response<Response<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<Product> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Load lại danh sách từ API
                        loadProducts();
                    } else {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLySanPham.this, "Lỗi khi cập nhật sản phẩm", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<Product>> call, Throwable t) {
                Toast.makeText(QuanLySanPham.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }

    private void deleteProduct(String id) {
        apiServices.deleteProduct(id).enqueue(new Callback<Response<Void>>() {
            @Override
            public void onResponse(Call<Response<Void>> call, retrofit2.Response<Response<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<Void> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                        // Load lại danh sách từ API
                        loadProducts();
                    } else {
                        Toast.makeText(QuanLySanPham.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(QuanLySanPham.this, "Lỗi khi xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    Log.e("API Error", response.toString());
                }
            }

            @Override
            public void onFailure(Call<Response<Void>> call, Throwable t) {
                Toast.makeText(QuanLySanPham.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API Error", t.toString());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                if (currentImagePreview != null) {
                    currentImagePreview.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showChiTietSanPhamDialog(Product product) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chi_tiet_san_pham);

        // Set width cho dialog
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        ImageView imgChiTiet = dialog.findViewById(R.id.imgChiTietSanPham);
        TextView tvTen = dialog.findViewById(R.id.tvChiTietTen);
        TextView tvMoTa = dialog.findViewById(R.id.tvChiTietMoTa);
        TextView tvGia = dialog.findViewById(R.id.tvChiTietGia);
        TextView tvDanhMuc = dialog.findViewById(R.id.tvChiTietDanhMuc);
        TextView tvId = dialog.findViewById(R.id.tvChiTietId);
        Button btnDong = dialog.findViewById(R.id.btnDong);

        // Load ảnh
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            String imageUrl = product.getImage();
            if (!imageUrl.startsWith("http")) {
                imageUrl = ApiServices.Url.replace("/api/", "") + imageUrl;
            }
            Glide.with(this).load(imageUrl).into(imgChiTiet);
        } else {
            imgChiTiet.setImageResource(R.drawable.ic_home);
        }

        tvTen.setText(product.getName() != null ? product.getName() : "Chưa có tên");
        tvMoTa.setText(product.getDescription() != null && !product.getDescription().isEmpty()
                ? product.getDescription() : "Chưa có mô tả");
        tvGia.setText(product.getFormattedPrice() != null ? product.getFormattedPrice() : "0đ");
        tvId.setText(product.getId() != null ? product.getId() : "Chưa có ID");

        // Tìm tên danh mục
        String categoryName = "Chưa có danh mục";
        if (product.getCategoryId() != null) {
            for (DanhMuc danhMuc : danhMucList) {
                if (danhMuc.getId().equals(product.getCategoryId())) {
                    categoryName = danhMuc.getName();
                    break;
                }
            }
        }
        tvDanhMuc.setText(categoryName);

        btnDong.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private File createFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Tạo file tạm
            File tempFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e("File Creation", "Error creating file: " + e.getMessage());
            return null;
        }
    }
}
