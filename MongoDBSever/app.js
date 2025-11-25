var createError = require('http-errors');
var express = require('express');
var path = require('path');
var cookieParser = require('cookie-parser');
var logger = require('morgan');

var indexRouter = require('./routes/index');
var api = require('./routes/api');
const database = require('./config/db');

var app = express();

// ==================== KẾT NỐI DATABASE ====================
database.connect();

// ==================== CẤU HÌNH VIEW ENGINE ====================
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'hbs');

// ==================== MIDDLEWARE ====================
app.use(logger('dev'));

// Middleware parse body
app.use(express.json()); // parse application/json
app.use(express.urlencoded({ extended: true })); // parse application/x-www-form-urlencoded

app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

// ==================== ROUTES ====================
app.use('/', indexRouter);
app.use('/api', api);

// ==================== XỬ LÝ 404 ====================
app.use(function (req, res, next) {
  res.status(404).json({ message: 'Không tìm thấy API hoặc trang yêu cầu' });
});

// ==================== XỬ LÝ ERROR ====================
app.use(function (err, req, res, next) {
  console.error("🔥 Lỗi server:", err.message);

  res.status(err.status || 500).json({
    message: err.message || "Lỗi server",
    error: req.app.get('env') === 'development' ? err : {}
  });
});

module.exports = app;