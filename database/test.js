const connectDB = require("./db");

(async () => {
    const db = await connectDB();
    if (db) {
        console.log("🔥 DB Connected:", db.databaseName);
    }
})();