var express = require('express');
var router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { Users, Categories, Products, CartItems, Orders, OrderDetails, Reviews } = require('../models/database'); 

const JWT_SECRET = "NHOM6";
const JWT_EXPIRE = "1h";

// API register
router.post('/register', async (req, res) => {
  try {
    const { name, email, password, sdt } = req.body;
    console.log("Register body:", req.body);

    // Kiểm tra dữ liệu
    if (!name || !sdt || !email || !password) {
      return res.status(400).json({ success: false, message: "Thiếu dữ liệu khi đăng kí" });
    }

    // Kiểm tra username đã tồn tại chưa
    const existUser = await Users.findOne({ email });
    if (existUser) {
      return res.status(400).json({ success: false, message: "Email đã tồn tại" });
    }


    const newUser = await Users.create({ name, email, password, sdt });

    res.json({
      success: true,
      message: "Đăng ký thành công",
      data: newUser
    });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

// API login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return res.status(400).json({ success: false, message: "Thiếu dữ liệu" });

    const user = await Users.findOne({ email });
    if (!user) return res.status(400).json({ success: false, message: "Người dùng không tồn tại" });

    if (password !== user.password) {
        return res.status(400).json({ success: false, message: "Sai mật khẩu" });
    }

    const token = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: JWT_EXPIRE });
    const refreshToken = jwt.sign({ id: user._id }, JWT_SECRET, { expiresIn: "7d" });

    res.json({
      success: true,
      message: "Đăng nhập thành công",
      token,
      refreshToken,
      data: {
        id: user._id,
        email: user.email,
        name: user.name,
        sdt: user.sdt
      }
    });

  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});

module.exports = router;
