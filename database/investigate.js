const connectDB = require('./db');

async function investigatePredictions() {
  try {
    const db = await connectDB();
    const collection = db.collection('user_predictions');

    // Get all predictions
    const predictions = await collection.find({}).toArray();
    console.log(`Total predictions: ${predictions.length}`);

    // For each prediction, check if userId is valid
    // But to check against H2 and MongoDB users, I need to query those collections too
    const userCollection = db.collection('users'); // assuming H2 users are synced or something, but probably not
    // Actually, the H2 users are in a different DB, not MongoDB.

    // Since H2 is SQL, and MongoDB is NoSQL, I can't easily query from here.

    // So, perhaps just list the predictions with userIds and usernames
    console.log('Predictions:');
    predictions.forEach(pred => {
      console.log(`UserId: ${pred.userId}, Username: ${pred.username}, MatchId: ${pred.matchId}`);
    });

  } catch (error) {
    console.error('Error:', error);
  } finally {
    process.exit();
  }
}

investigatePredictions();