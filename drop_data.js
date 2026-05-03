/**
 * drop_data.js — Drops movies, shows, seats, theatres from moviebooking DB
 * Run: node drop_data.js
 * Then restart Spring Boot to re-seed with updated DataLoader settings.
 */
const { MongoClient } = require('mongodb');

async function dropCollections() {
  const client = new MongoClient('mongodb://localhost:27017');
  try {
    await client.connect();
    const db = client.db('moviebooking');

    const collections = ['movies', 'shows', 'seats', 'theatres'];
    for (const col of collections) {
      try {
        await db.collection(col).drop();
        console.log(`✅ Dropped: ${col}`);
      } catch (e) {
        console.log(`ℹ️  ${col} — already empty or not found`);
      }
    }

    const counts = await Promise.all(
      ['users','bookings','payments','tickets'].map(async c => ({
        name: c, count: await db.collection(c).countDocuments()
      }))
    );
    console.log('\n📊 Remaining collections (preserved):');
    counts.forEach(c => console.log(`   ${c.name}: ${c.count} documents`));
    console.log('\n✅ Done! Restart Spring Boot to re-seed with updated movies/shows.');
  } finally {
    await client.close();
  }
}

dropCollections().catch(console.error);
