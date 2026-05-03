/**
 * fix_shows.js
 * Deletes wrong shows for today + tomorrow (IST) so the backend
 * can regenerate them correctly with 7 movies x 10 theatres x 3 timings.
 *
 * Spring Data MongoDB stores LocalDate("2026-05-01") as:
 *   ISODate("2026-04-30T18:30:00.000Z")  ← IST midnight = UTC-05:30
 *
 * Run: node fix_shows.js
 */
const { MongoClient } = require('mongodb');

async function main() {
  const client = new MongoClient('mongodb://localhost:27017');
  await client.connect();
  const db = client.db('moviebooking');

  // IST dates expressed as UTC (IST = UTC + 5:30, so midnight IST = 18:30 prev day UTC)
  const MAY_02_IST = new Date('2026-05-01T18:30:00.000Z'); // 2026-05-02 in IST (TODAY)
  const MAY_03_IST = new Date('2026-05-02T18:30:00.000Z'); // 2026-05-03 in IST (TOMORROW)

  // ── Audit current state ──────────────────────────────────────
  const totalShows    = await db.collection('shows').countDocuments();
  const todayCount    = await db.collection('shows').countDocuments({ showDate: MAY_02_IST });
  const tomorrowCount = await db.collection('shows').countDocuments({ showDate: MAY_03_IST });

  console.log('\n📊 Current DB state:');
  console.log('   Total shows  :', totalShows);
  console.log('   Today   (05/02 IST):', todayCount, todayCount === 210 ? '✅ CORRECT' : '❌ WRONG — expected 210');
  console.log('   Tomorrow(05/03 IST):', tomorrowCount, tomorrowCount === 210 ? '✅ CORRECT' : '❌ WRONG — expected 210');

  let anyDeleted = false;

  // ── Fix today if wrong ───────────────────────────────────────
  if (todayCount !== 210) {
    console.log('\n🔄 Fixing today (05/02 IST)...');
    anyDeleted = await deleteShowsForDate(db, MAY_02_IST) || anyDeleted;
  }

  // ── Fix tomorrow if wrong ────────────────────────────────────
  if (tomorrowCount !== 210) {
    console.log('\n🔄 Fixing tomorrow (05/03 IST)...');
    anyDeleted = await deleteShowsForDate(db, MAY_03_IST) || anyDeleted;
  }

  if (!anyDeleted) {
    console.log('\n✅ Both dates already have 210 shows each. Nothing to fix!');
    console.log('   → Backend should display 3 timings per theatre per movie.');
  } else {
    console.log('\n✅ Cleanup done! NOW restart the backend:');
    console.log('   .\\mvnw.cmd spring-boot:run');
    console.log('   It will regenerate: 7 movies × 10 theatres × 3 timings = 210 shows/day');
  }

  await client.close();
}

async function deleteShowsForDate(db, dateUTC) {
  // Find all shows for this date that have NO booked seats
  const toDelete = await db.collection('shows')
    .find(
      { showDate: dateUTC, bookedSeats: { $size: 0 } },
      { projection: { _id: 1 } }
    )
    .toArray();

  if (toDelete.length === 0) {
    console.log('   Nothing to delete for this date (all seats may be booked).');
    return false;
  }

  const ids       = toDelete.map(s => s._id);
  const idStrings = ids.map(id => id.toString());

  // Delete seats first (referential integrity)
  const seatResult = await db.collection('seats').deleteMany({ showId: { $in: idStrings } });
  console.log('   Seats deleted:', seatResult.deletedCount);

  const showResult = await db.collection('shows').deleteMany({ _id: { $in: ids } });
  console.log('   Shows deleted:', showResult.deletedCount);

  return true;
}

main().catch(err => {
  console.error('❌ Error:', err.message);
  process.exit(1);
});
