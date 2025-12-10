package com.example.duan1.ui_user;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.duan1.OrderAdapter;
import com.example.duan1.OrderDetailAdapter;
import com.example.duan1.R;
import com.example.duan1.model.Order;
import com.example.duan1.model.OrderDetail;
import com.example.duan1.model.Response;
import com.example.duan1.model.Review;
import com.example.duan1.services.ApiServices;
import com.example.duan1.utils.PollingHelper;
import com.example.duan1.utils.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class DonHangUserFragment extends Fragment {

    private RecyclerView rvDonHang;
    private TextView tvEmpty;
    private ApiServices apiServices;
    private List<Order> orderList;
    private List<Order> allOrderList; // Lưu tất cả đơn hàng
    private OrderAdapter orderAdapter;
    private String userId;
    private CardView btnTabDangCho, btnTabDaHoanThanh;
    private TextView tvTabDangCho, tvTabDaHoanThanh;
    private int selectedTab = 0; // 0 = Đang chờ/Đang giao, 1 = Đã hoàn thành
    private PollingHelper pollingHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_don_hang_user, container, false);

        // Khởi tạo Retrofit
        apiServices = RetrofitClient.getInstance().getApiServices();

        // Lấy user_id từ SharedPreferences
        SharedPreferences prefs = getContext().getSharedPreferences("UserData", getContext().MODE_PRIVATE);
        userId = prefs.getString("id_taikhoan", null);
        Log.d("DonHang", "User ID: " + userId);

        // Ánh xạ views
        rvDonHang = view.findViewById(R.id.rvDonHang);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnTabDangCho = view.findViewById(R.id.btnTabDangCho);
        btnTabDaHoanThanh = view.findViewById(R.id.btnTabDaHoanThanh);
        tvTabDangCho = view.findViewById(R.id.tvTabDangCho);
        tvTabDaHoanThanh = view.findViewById(R.id.tvTabDaHoanThanh);

        // Khởi tạo danh sách
        orderList = new ArrayList<>();
        allOrderList = new ArrayList<>();

        // Setup RecyclerView
        orderAdapter = new OrderAdapter(getContext(), orderList);
        // Không set filter cho user (user chỉ xem, không có action buttons)
        orderAdapter.setOrderStatusFilter(null);
        rvDonHang.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDonHang.setAdapter(orderAdapter);
        
        // Xử lý click vào đơn hàng để xem chi tiết
        orderAdapter.setOnDetailClickListener(order -> {
            showChiTietDonHangDialog(order);
        });

        // Xử lý click vào nút đánh giá
        orderAdapter.setOnActionClickListener(new OrderAdapter.OnActionClickListener() {
            @Override
            public void onXacNhanClick(Order order, int position) {}

            @Override
            public void onHuyClick(Order order, int position) {}

            @Override
            public void onBatDauGiaoClick(Order order, int position) {}

            @Override
            public void onGiaoThanhCongClick(Order order, int position) {}

            @Override
            public void onGiaoThatBaiClick(Order order, int position) {}

            @Override
            public void onDanhGiaClick(Order order, int position) {
                // Kiểm tra xem đơn hàng đã được đánh giá chưa
                checkReviewStatus(order, position);
            }
        });

        // Xử lý click vào tab buttons
        btnTabDangCho.setOnClickListener(v -> {
            selectedTab = 0;
            updateTabButtons();
            filterOrders();
        });

        btnTabDaHoanThanh.setOnClickListener(v -> {
            selectedTab = 1;
            updateTabButtons();
            filterOrders();
        });

        // Khởi tạo tab mặc định
        updateTabButtons();
        
        // Load đơn hàng
        if (userId != null) {
            loadOrders();
            
            // Khởi tạo PollingHelper để tự động cập nhật mỗi 5 giây
            pollingHelper = new PollingHelper("DonHangUser", 5000);
            pollingHelper.setRefreshCallback(() -> {
                loadOrders();
            });
            pollingHelper.startPolling();
        } else {
            Log.e("DonHang", "User ID is null");
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Vui lòng đăng nhập");
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pollingHelper != null) {
            pollingHelper.stopPolling();
        }
    }

    private void loadOrders() {
        if (userId == null) {
            Log.e("DonHang", "Cannot load orders: userId is null");
            return;
        }

        apiServices.getOrders(userId).enqueue(new Callback<Response<List<Order>>>() {
            @Override
            public void onResponse(Call<Response<List<Order>>> call, retrofit2.Response<Response<List<Order>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<Order>> res = response.body();
                    Log.d("DonHang", "Response success: " + res.isSuccess() + ", Data size: " + (res.getData() != null ? res.getData().size() : 0));
                    if (res.isSuccess() && res.getData() != null) {
                        allOrderList.clear();
                        allOrderList.addAll(res.getData());
                        filterOrders(); // Filter theo tab hiện tại
                        Log.d("DonHang", "Loaded " + allOrderList.size() + " orders");
                    } else {
                        allOrderList.clear();
                        orderList.clear();
                        orderAdapter.updateList(orderList);
                        updateEmptyState();
                        Log.d("DonHang", "No orders: " + res.getMessage());
                    }
                } else {
                    String errorMsg = "Lỗi khi tải đơn hàng";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Log.e("DonHang", "Load orders failed: " + errorMsg);
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Response<List<Order>>> call, Throwable t) {
                Log.e("DonHang", "Load orders failure: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (orderList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvDonHang.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvDonHang.setVisibility(View.VISIBLE);
        }
    }

    private void updateTabButtons() {
        if (selectedTab == 0) {
            // Tab "Đang chờ/Đang giao" được chọn
            btnTabDangCho.setCardBackgroundColor(Color.parseColor("#B5CBF3"));
            tvTabDangCho.setTextColor(Color.parseColor("#FFFFFF"));
            btnTabDaHoanThanh.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            tvTabDaHoanThanh.setTextColor(Color.parseColor("#000000"));
        } else {
            // Tab "Đã hoàn thành" được chọn
            btnTabDangCho.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            tvTabDangCho.setTextColor(Color.parseColor("#000000"));
            btnTabDaHoanThanh.setCardBackgroundColor(Color.parseColor("#B5CBF3"));
            tvTabDaHoanThanh.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void filterOrders() {
        orderList.clear();
        
        if (selectedTab == 0) {
            // Tab "Đang chờ/Đang giao": lọc các đơn hàng có status là chờ xác nhận, đang chờ, đang chuẩn bị, đang giao
            for (Order order : allOrderList) {
                String status = order.getStatus();
                if (status != null) {
                    String statusLower = status.toLowerCase();
                    if (statusLower.contains("chờ xác nhận") || 
                        statusLower.contains("đang chờ") || 
                        statusLower.contains("pending") ||
                        statusLower.contains("chờ lấy hàng") ||
                        statusLower.contains("đang chuẩn bị") ||
                        statusLower.contains("đang chuẩn bị đơn hàng") ||
                        statusLower.contains("preparing") ||
                        statusLower.contains("đang giao") ||
                        statusLower.contains("delivering")) {
                        orderList.add(order);
                    }
                }
            }
        } else {
            // Tab "Đã hoàn thành": lọc các đơn hàng có status là đã giao, đã nhận, đã hủy, người dùng hủy
            for (Order order : allOrderList) {
                String status = order.getStatus();
                if (status != null) {
                    String statusLower = status.toLowerCase();
                    if (statusLower.contains("đã nhận") || 
                        statusLower.contains("đã giao") || 
                        statusLower.contains("delivered") ||
                        statusLower.contains("đã hủy") ||
                        statusLower.contains("người dùng hủy") ||
                        statusLower.contains("user đã hủy") ||
                        statusLower.contains("admin đã hủy") ||
                        statusLower.contains("cancelled") ||
                        statusLower.contains("giao thành công")) {
                        orderList.add(order);
                    }
                }
            }
        }
        
        orderAdapter.updateList(orderList);
        updateEmptyState();
    }

    private void showChiTietDonHangDialog(Order order) {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chi_tiet_don_hang);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView tvOrderId = dialog.findViewById(R.id.tvOrderId);
        TextView tvStatus = dialog.findViewById(R.id.tvStatus);
        TextView tvOrderDate = dialog.findViewById(R.id.tvOrderDate);
        TextView tvTotalPrice = dialog.findViewById(R.id.tvTotalPrice);
        TextView tvSubtotal = dialog.findViewById(R.id.tvSubtotal);
        View layoutVoucher = dialog.findViewById(R.id.layoutVoucher);
        TextView tvVoucherTitle = dialog.findViewById(R.id.tvVoucherTitle);
        TextView tvVoucherCode = dialog.findViewById(R.id.tvVoucherCode);
        TextView tvVoucherDiscount = dialog.findViewById(R.id.tvVoucherDiscount);
        TextView tvUserName = dialog.findViewById(R.id.tvUserName);
        TextView tvUserEmail = dialog.findViewById(R.id.tvUserEmail);
        TextView tvUserPhone = dialog.findViewById(R.id.tvUserPhone);
        TextView tvReceiverName = dialog.findViewById(R.id.tvReceiverName);
        TextView tvReceiverAddress = dialog.findViewById(R.id.tvReceiverAddress);
        TextView tvReceiverPhone = dialog.findViewById(R.id.tvReceiverPhone);
        View layoutCancelReason = dialog.findViewById(R.id.layoutCancelReason);
        TextView tvCancelReason = dialog.findViewById(R.id.tvCancelReason);
        RecyclerView rvOrderDetails = dialog.findViewById(R.id.rvOrderDetails);
        Button btnHuyDonHang = dialog.findViewById(R.id.btnHuyDonHang);
        Button btnDong = dialog.findViewById(R.id.btnDong);

        // Hiển thị lý do hủy nếu có
        if (order.getCancelReason() != null && !order.getCancelReason().isEmpty()) {
            layoutCancelReason.setVisibility(View.VISIBLE);
            tvCancelReason.setText(order.getCancelReason());
        } else {
            layoutCancelReason.setVisibility(View.GONE);
        }

        // Hiển thị thông tin
        String orderIdText = order.getOrderId() != null ? order.getOrderId() : 
                            (order.getId() != null ? order.getId().substring(order.getId().length() - 6) : "N/A");
        tvOrderId.setText(orderIdText);
        
        String status = order.getStatus() != null ? order.getStatus() : "Chưa xác định";
        tvStatus.setText(status);
        tvStatus.setTextColor(order.getStatusColor());
        
        // Hiển thị thời gian đặt hàng
        String createdAtStr = order.getCreatedAt();
        android.util.Log.d("DonHangUser", "Order createdAt: " + createdAtStr);
        
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                // Parse ISO format từ server (yyyy-MM-dd'T'HH:mm:ss.SSS'Z' hoặc yyyy-MM-dd'T'HH:mm:ss'Z')
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = inputFormat.parse(createdAtStr);
                
                // Format lại thành dd/MM/yyyy HH:mm
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                tvOrderDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                // Thử parse format khác (không có milliseconds)
                try {
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                    inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = inputFormat.parse(createdAtStr);
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                    tvOrderDate.setText(outputFormat.format(date));
                } catch (Exception e2) {
                    // Thử parse format không có Z
                    try {
                        java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                        java.util.Date date = inputFormat.parse(createdAtStr);
                        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                        tvOrderDate.setText(outputFormat.format(date));
                    } catch (Exception e3) {
                        android.util.Log.e("DonHangUser", "Error parsing createdAt: " + createdAtStr, e3);
                        tvOrderDate.setText(createdAtStr); // Hiển thị raw string nếu không parse được
                    }
                }
            }
        } else {
            android.util.Log.w("DonHangUser", "Order createdAt is null or empty for order: " + order.getOrderId());
            tvOrderDate.setText("N/A");
        }

        // Hiển thị subtotal
        double subtotal = order.getSubtotal();
        long subtotalLong = Math.round(subtotal);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        String formattedSubtotal = df.format(subtotalLong).replace(",", ".") + "đ";
        tvSubtotal.setText(formattedSubtotal);

        // Hiển thị voucher nếu có - lấy thông tin từ API để hiển thị max discount
        if (order.getVoucherCode() != null && !order.getVoucherCode().isEmpty()) {
            layoutVoucher.setVisibility(View.VISIBLE);
            tvVoucherTitle.setText(order.getVoucherTitle() != null ? order.getVoucherTitle() : "Voucher");
            tvVoucherCode.setText(order.getVoucherCode());
            double discount = order.getDiscountAmount();
            long discountLong = Math.round(discount);
            String formattedDiscount = df.format(discountLong).replace(",", ".") + "đ";
            tvVoucherDiscount.setText("-" + formattedDiscount);
            
            // Lấy thông tin voucher từ API để hiển thị max discount
            loadVoucherInfoForOrder(order.getVoucherCode(), order.getDiscountAmount(), tvVoucherDiscount);
        } else {
            layoutVoucher.setVisibility(View.GONE);
        }

        // Hiển thị thành tiền (sau khi giảm giá)
        double totalPrice = order.getTotalPrice();
        long totalLong = Math.round(totalPrice);
        String formattedTotal = df.format(totalLong).replace(",", ".") + "đ";
        tvTotalPrice.setText(formattedTotal);

        // Hiển thị thông tin người đặt (chính là user hiện tại)
        SharedPreferences prefs = getContext().getSharedPreferences("UserData", getContext().MODE_PRIVATE);
        tvUserName.setText(prefs.getString("name", "N/A"));
        tvUserEmail.setText(prefs.getString("email", "N/A"));
        tvUserPhone.setText(prefs.getString("phone", "N/A"));
        
        tvReceiverName.setText(order.getReceiverName() != null ? order.getReceiverName() : "N/A");
        tvReceiverAddress.setText(order.getReceiverAddress() != null ? order.getReceiverAddress() : "N/A");
        tvReceiverPhone.setText(order.getReceiverPhone() != null ? order.getReceiverPhone() : "N/A");

        // Setup RecyclerView cho danh sách sản phẩm
        OrderDetailAdapter orderDetailAdapter = new OrderDetailAdapter(getContext(), new ArrayList<>());
        rvOrderDetails.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrderDetails.setAdapter(orderDetailAdapter);

        // Load order details
        loadOrderDetails(order.getId(), orderDetailAdapter);

        // Hiển thị nút hủy đơn hàng nếu đơn hàng chưa được xác nhận
        String statusLower = status.toLowerCase();
        if (statusLower.contains("chờ xác nhận") || statusLower.contains("pending") || statusLower.contains("đang chờ")) {
            btnHuyDonHang.setVisibility(View.VISIBLE);
            btnHuyDonHang.setOnClickListener(v -> {
                // Xác nhận hủy đơn hàng
                new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Xác nhận hủy đơn hàng")
                        .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
                        .setPositiveButton("Hủy đơn hàng", (dialog1, which) -> {
                            cancelOrder(order.getId(), dialog);
                        })
                        .setNegativeButton("Không", null)
                        .show();
            });
        } else {
            btnHuyDonHang.setVisibility(View.GONE);
        }

        btnDong.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadOrderDetails(String orderId, OrderDetailAdapter adapter) {
        apiServices.getOrderDetails(orderId).enqueue(new Callback<Response<List<OrderDetail>>>() {
            @Override
            public void onResponse(Call<Response<List<OrderDetail>>> call, retrofit2.Response<Response<List<OrderDetail>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<OrderDetail>> res = response.body();
                    if (res.isSuccess() && res.getData() != null) {
                        adapter.updateList(res.getData());
                        Log.d("DonHang", "Loaded " + res.getData().size() + " order details");
                    }
                } else {
                    Log.e("DonHang", "Load order details failed");
                }
            }

            @Override
            public void onFailure(Call<Response<List<OrderDetail>>> call, Throwable t) {
                Log.e("DonHang", "Load order details failure: " + t.getMessage());
            }
        });
    }

    private void loadVoucherInfoForOrder(String voucherCode, double discountAmount, TextView tvVoucherDiscount) {
        // Lấy mã voucher đầu tiên nếu có nhiều voucher
        String firstVoucherCode = voucherCode.split(",")[0].trim();
        
        apiServices.getAllVouchers().enqueue(new Callback<Response<List<com.example.duan1.model.Voucher>>>() {
            @Override
            public void onResponse(Call<Response<List<com.example.duan1.model.Voucher>>> call, retrofit2.Response<Response<List<com.example.duan1.model.Voucher>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<List<com.example.duan1.model.Voucher>> res = response.body();
                    if (res.isSuccess() && res.getData() != null) {
                        for (com.example.duan1.model.Voucher voucher : res.getData()) {
                            if (voucher.getVoucherCode().equalsIgnoreCase(firstVoucherCode)) {
                                // Cập nhật hiển thị với thông tin max discount
                                long discountLong = Math.round(discountAmount);
                                java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
                                String formattedDiscount = df.format(discountLong).replace(",", ".") + "đ";
                                
                                if (voucher.getMaxDiscountAmount() > 0) {
                                    long maxAmount = Math.round(voucher.getMaxDiscountAmount());
                                    String formattedMax = df.format(maxAmount).replace(",", ".") + "đ";
                                    tvVoucherDiscount.setText("-" + formattedDiscount + " (tối đa " + formattedMax + ")");
                                } else {
                                    tvVoucherDiscount.setText("-" + formattedDiscount);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Response<List<com.example.duan1.model.Voucher>>> call, Throwable t) {
                // Không làm gì nếu lỗi
            }
        });
    }

    private void cancelOrder(String orderId, Dialog dialog) {
        if (userId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("user_id", userId);

        apiServices.cancelOrder(orderId, body).enqueue(new Callback<Response<Order>>() {
            @Override
            public void onResponse(Call<Response<Order>> call, retrofit2.Response<Response<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<Order> res = response.body();
                    if (res.isSuccess()) {
                        Toast.makeText(getContext(), "Hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        // Reload orders
                        loadOrders();
                    } else {
                        Toast.makeText(getContext(), res.getMessage() != null ? res.getMessage() : "Hủy đơn hàng thất bại", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Hủy đơn hàng thất bại";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Response<Order>> call, Throwable t) {
                Log.e("DonHang", "Cancel order failure: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDanhGiaDialog(Order order) {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_danh_gia);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView star1 = dialog.findViewById(R.id.star1);
        ImageView star2 = dialog.findViewById(R.id.star2);
        ImageView star3 = dialog.findViewById(R.id.star3);
        ImageView star4 = dialog.findViewById(R.id.star4);
        ImageView star5 = dialog.findViewById(R.id.star5);
        TextInputEditText edtComment = dialog.findViewById(R.id.edtComment);
        Button btnHuy = dialog.findViewById(R.id.btnHuyDanhGia);
        Button btnGui = dialog.findViewById(R.id.btnGuiDanhGia);

        // Mảng để lưu rating
        final int[] rating = {0};

        // Xử lý click vào các sao
        View.OnClickListener starClickListener = v -> {
            int clickedRating = 0;
            if (v == star1) clickedRating = 1;
            else if (v == star2) clickedRating = 2;
            else if (v == star3) clickedRating = 3;
            else if (v == star4) clickedRating = 4;
            else if (v == star5) clickedRating = 5;

            rating[0] = clickedRating;
            updateStars(star1, star2, star3, star4, star5, clickedRating);
        };

        star1.setOnClickListener(starClickListener);
        star2.setOnClickListener(starClickListener);
        star3.setOnClickListener(starClickListener);
        star4.setOnClickListener(starClickListener);
        star5.setOnClickListener(starClickListener);

        // Xử lý nút hủy
        btnHuy.setOnClickListener(v -> dialog.dismiss());

        // Xử lý nút gửi đánh giá
        btnGui.setOnClickListener(v -> {
            if (rating[0] == 0) {
                Toast.makeText(getContext(), "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            // Gọi API tạo đánh giá
            Map<String, String> body = new HashMap<>();
            body.put("order_id", order.getId());
            body.put("user_id", userId);
            body.put("rating", String.valueOf(rating[0]));
            body.put("comment", edtComment.getText() != null ? edtComment.getText().toString().trim() : "");

            apiServices.createReview(body).enqueue(new Callback<Response<Review>>() {
                @Override
                public void onResponse(Call<Response<Review>> call, retrofit2.Response<Response<Review>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Response<Review> res = response.body();
                        if (res.isSuccess()) {
                            Toast.makeText(getContext(), "Đánh giá thành công", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // Reload orders để cập nhật UI (ẩn nút đánh giá nếu đã đánh giá)
                            loadOrders();
                        } else {
                            Toast.makeText(getContext(), res.getMessage() != null ? res.getMessage() : "Đánh giá thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMsg = "Đánh giá thất bại";
                        if (response.errorBody() != null) {
                            try {
                                errorMsg = response.errorBody().string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Response<Review>> call, Throwable t) {
                    Log.e("DonHang", "Create review failure: " + t.getMessage());
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void checkReviewsForOrders() {
        // Check review status cho tất cả đơn hàng đã nhận/đã giao
        for (int i = 0; i < orderList.size(); i++) {
            final int position = i;
            Order order = orderList.get(i);
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
            if (status.contains("đã nhận") || status.contains("đã giao") || status.contains("delivered") || status.contains("giao thành công")) {
                apiServices.getReviewByOrder(order.getId()).enqueue(new Callback<Response<com.example.duan1.model.Review>>() {
                    @Override
                    public void onResponse(Call<Response<com.example.duan1.model.Review>> call, retrofit2.Response<Response<com.example.duan1.model.Review>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Response<com.example.duan1.model.Review> res = response.body();
                            if (res.isSuccess() && res.getData() != null) {
                                // Đã đánh giá, cập nhật button text
                                if (position < orderList.size()) {
                                    orderAdapter.updateReviewButtonText(position, "Xem đánh giá");
                                }
                            }
                        }
                    }



                    @Override
                    public void onFailure(Call<Response<com.example.duan1.model.Review>> call, Throwable t) {
                        // Ignore
                    }
                });
            }
        }
    }
    
    private void checkReviewStatus(Order order, int position) {
        // Kiểm tra xem đơn hàng đã được đánh giá chưa
        apiServices.getReviewByOrder(order.getId()).enqueue(new Callback<Response<com.example.duan1.model.Review>>() {
            @Override
            public void onResponse(Call<Response<com.example.duan1.model.Review>> call, retrofit2.Response<Response<com.example.duan1.model.Review>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response<com.example.duan1.model.Review> res = response.body();
                    if (res.isSuccess() && res.getData() != null) {
                        // Đã đánh giá rồi, hiển thị dialog xem đánh giá
                        showXemDanhGiaDialog(res.getData());
                    } else {
                        // Chưa đánh giá, hiển thị dialog đánh giá
                        showDanhGiaDialog(order);
                    }
                } else {
                    // Chưa đánh giá, hiển thị dialog đánh giá
                    showDanhGiaDialog(order);
                }
            }

            @Override
            public void onFailure(Call<Response<com.example.duan1.model.Review>> call, Throwable t) {
                // Chưa đánh giá, hiển thị dialog đánh giá
                showDanhGiaDialog(order);
            }
        });
    }
    
    private void showXemDanhGiaDialog(com.example.duan1.model.Review review) {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_danh_gia);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView star1 = dialog.findViewById(R.id.star1);
        ImageView star2 = dialog.findViewById(R.id.star2);
        ImageView star3 = dialog.findViewById(R.id.star3);
        ImageView star4 = dialog.findViewById(R.id.star4);
        ImageView star5 = dialog.findViewById(R.id.star5);
        TextInputEditText edtComment = dialog.findViewById(R.id.edtComment);
        Button btnHuy = dialog.findViewById(R.id.btnHuyDanhGia);
        Button btnGui = dialog.findViewById(R.id.btnGuiDanhGia);

        // Hiển thị đánh giá đã có
        int rating = review.getRating();
        updateStars(star1, star2, star3, star4, star5, rating);
        edtComment.setText(review.getComment());
        edtComment.setEnabled(false); // Không cho chỉnh sửa
        
        // Vô hiệu hóa các sao
        star1.setEnabled(false);
        star2.setEnabled(false);
        star3.setEnabled(false);
        star4.setEnabled(false);
        star5.setEnabled(false);
        
        btnGui.setVisibility(View.GONE); // Ẩn nút gửi
        btnHuy.setText("Đóng");
        btnHuy.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateStars(ImageView star1, ImageView star2, ImageView star3, ImageView star4, ImageView star5, int rating) {
        star1.setImageResource(rating >= 1 ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
        star2.setImageResource(rating >= 2 ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
        star3.setImageResource(rating >= 3 ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
        star4.setImageResource(rating >= 4 ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
        star5.setImageResource(rating >= 5 ? android.R.drawable.star_big_on : android.R.drawable.star_big_off);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadOrders();
        }
    }
}

