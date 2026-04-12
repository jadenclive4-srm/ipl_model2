const { MongoClient } = require("mongodb");

const uri = "mongodb+srv://Admin_main:admin123@cluster0.v2imikk.mongodb.net/ipl_app";
const client = new MongoClient(uri);

async function connectDB() {
    try {
        await client.connect();
        console.log("✅ Connected to MongoDB Atlas");

        const db = client.db("ipl_app");
        return db;

    } catch (err) {
        console.error("❌ Error:", err);
    }
}

module.exports = connectDB;