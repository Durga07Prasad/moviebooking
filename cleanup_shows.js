// cleanup_shows.js — deletes wrongly-generated shows for today + tomorrow
// Run: node cleanup_shows.js
const { MongoClient } = require('mongodb');

async function main() {
  const client = new MongoClient('mongodb://localhost:27017');
  await client.connect();
  const db = client.db('moviebooking');

  // Delete shows for TODAY only (2026-05-01) — tomorrow's shows are already correct
  const today    = new Date('2026-05-01T00:00:00.000Z');
  const tomorrow = new Date('2026-05-02T00:00:00.000Z');

  // Find shows for today+tomorrow with empty bookedSeats
  const showsToDelete = await db.collection('shows').find({
    showDate: { $gte: today, $lt: tomorrow },
    $or: [{ bookedSeats: { $size: 0 } }, { bookedSeats: { $exists: false } }]
  }, { projection: { _id: 1 } }).toArray();

  const ids = showsToDelete.map(s => s._id);
  const idStrings = ids.map(id => id.toString());

  console.log(`Found ${ids.length} shows to delete for 2026-05-01 and 2026-05-02`);

  if (ids.length === 0) {
    console.log('Nothing to clean up.');
    await client.close();
    return;
  }

  // Delete seats for these shows first
  const seatResult = await db.collection('seats').deleteMany({
    showId: { $in: idStrings }
  });
  console.log(`Deleted ${seatResult.deletedCount} seats`);

  // Delete the shows
  const showResult = await db.collection('shows').deleteMany({
    _id: { $in: ids }
  });
  console.log(`Deleted ${showResult.deletedCount} shows`);

  const remaining = await db.collection('shows').countDocuments();
  const remainingSeats = await db.collection('seats').countDocuments();
  console.log(`Remaining: ${remaining} shows, ${remainingSeats} seats`);

  await client.close();
  console.log('Done! Now restart the backend to regenerate correct shows.');
}

main().catch(console.error);
