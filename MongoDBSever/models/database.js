const mongoose = require("mongoose");
const Schema = mongoose.Schema;

/* ==========================
   USERS
========================== */
const UserSchema = new Schema({
  name: { type: String, required: true },
  sdt: { type: String },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
}, { timestamps: true });

const Users = mongoose.model("users", UserSchema);

/* ==========================
   CATEGORIES
========================== */
const CategorySchema = new Schema({
  name: { type: String, required: true },
  description: { type: String }
}, { timestamps: true });

const Categories = mongoose.model("categories", CategorySchema);

/* ==========================
   PRODUCTS
========================== */
const ProductSchema = new Schema({
  name: { type: String, required: true },
  description: { type: String },
  price: { type: Number, required: true },
  image: { type: String },
  category_id: { type: Schema.Types.ObjectId, ref: "categories", required: true }
}, { timestamps: true });

const Products = mongoose.model("products", ProductSchema);

/* ==========================
   CART ITEMS
========================== */
const CartItemSchema = new Schema({
  user_id: { type: Schema.Types.ObjectId, ref: "users", required: true },
  product_id: { type: Schema.Types.ObjectId, ref: "products", required: true },
  quantity: { type: Number, required: true },
  subtotal: { type: Number, required: true },
}, { timestamps: true });

const CartItems = mongoose.model("cart_items", CartItemSchema);

/* ==========================
   ORDERS
========================== */
const OrderSchema = new Schema({
  user_id: { type: Schema.Types.ObjectId, ref: "users", required: true },
  total_price: { type: Number, required: true },
  status: { type: String, default: "pending" },
  subtotal: { type: Number },
  receiver_name: { type: String, required: true },
  receiver_address: { type: String, required: true },
  receiver_phone: { type: String, required: true },
}, { timestamps: true });

const Orders = mongoose.model("orders", OrderSchema);

/* ==========================
   ORDER DETAILS
========================== */
const OrderDetailSchema = new Schema({
  order_id: { type: Schema.Types.ObjectId, ref: "orders", required: true },
  product_id: { type: Schema.Types.ObjectId, ref: "products", required: true },
  quantity: { type: Number, required: true },
  price: { type: Number, required: true },
  subtotal: { type: Number, required: true }
}, { timestamps: true });

const OrderDetails = mongoose.model("order_details", OrderDetailSchema);

/* ==========================
   REVIEWS
========================== */
const ReviewSchema = new Schema({
  order_id: { type: Schema.Types.ObjectId, ref: "orders", required: true },
  user_id: { type: Schema.Types.ObjectId, ref: "users", required: true },
  rating: { type: Number, min: 1, max: 5, required: true },
  comment: { type: String },
}, { timestamps: true });

const Reviews = mongoose.model("reviews", ReviewSchema);

/* ==========================
   EXPORT
========================== */
module.exports = {
  Users,
  Categories,
  Products,
  CartItems,
  Orders,
  OrderDetails,
  Reviews
};
