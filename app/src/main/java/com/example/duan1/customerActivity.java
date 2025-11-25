package com.example.duan1;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class customerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CustomerAdapter adapter;
    private List<Customer> customerList;
    private EditText edtSearch;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        // Ánh xạ view
        recyclerView = findViewById(R.id.Rvcustomer);
        edtSearch = findViewById(R.id.edtSearch);
        fabAdd = findViewById(R.id.fabAdd);

        // Khởi tạo danh sách khách hàng demo
        customerList = new ArrayList<>();
        customerList.add(new Customer("Nguyen Van A", "a@gmail.com", "0123456789", "Hanoi"));
        customerList.add(new Customer("Tran Thi B", "b@gmail.com", "0987654321", "HCM"));
        customerList.add(new Customer("Le Van C", "c@gmail.com", "0912345678", "Danang"));

        // Adapter và RecyclerView
        adapter = new CustomerAdapter(this, customerList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Sự kiện Edit/Delete
        adapter.setOnItemClickListener(new CustomerAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(int position) {
                Customer customer = customerList.get(position);
                showEditCustomerDialog(customer, position);
            }

            @Override
            public void onDeleteClick(int position) {
                Customer customer = customerList.get(position);
                customerList.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(customerActivity.this,
                        "Đã xóa: " + customer.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Thêm khách hàng bằng FAB
        fabAdd.setOnClickListener(v -> showAddCustomerDialog());
    }

    private void showAddCustomerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_khachhang);

        EditText edtName = dialog.findViewById(R.id.edtTEnKH);
        EditText edtPhone = dialog.findViewById(R.id.edtPhone);
        EditText edtAddress = dialog.findViewById(R.id.edtDiachi);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(customerActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            Customer customer = new Customer(name, "", phone, address);
            customerList.add(customer);
            adapter.notifyItemInserted(customerList.size() - 1);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditCustomerDialog(Customer customer, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_them_khachhang);

        EditText edtName = dialog.findViewById(R.id.edtTEnKH);
        EditText edtPhone = dialog.findViewById(R.id.edtPhone);
        EditText edtAddress = dialog.findViewById(R.id.edtDiachi);
        Button btnAdd = dialog.findViewById(R.id.btnThem);
        Button btnCancel = dialog.findViewById(R.id.btnHuy);

        edtName.setText(customer.getName());
        edtPhone.setText(customer.getPhone());
        edtAddress.setText(customer.getAddress());
        btnAdd.setText("Cập nhật");

        btnAdd.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(customerActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            customer.setName(name);
            customer.setPhone(phone);
            customer.setAddress(address);

            adapter.notifyItemChanged(position);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}