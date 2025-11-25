const mongoose = require('mongoose');
mongoose.set('strictQuery', true);

// kết nối local
const local = 'mongodb://127.0.0.1:27017/Duan1';
const connect = async () => {
    try {
        await mongoose.connect(local, {
            useNewUrlParser: true,
            useUnifiedTopology: true,
        });
        console.log('✅ connect success');
    } catch (error) {
        console.error(error);
        console.log('❌ connect fail');
    }
}

module.exports = { connect };