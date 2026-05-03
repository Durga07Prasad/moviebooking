// ========================================================
// MongoDB Sample Data — Run this in MongoDB Shell or Compass
// CineBook - Movie Ticket Booking System (OOAD Project)
// ========================================================

// Switch to database
use moviebooking;

// ---------- 1. USERS ----------
db.users.insertMany([
  {
    name: "Durga Sravani",
    email: "durga@example.com",
    // BCrypt hash of "password123" — replace with actual hash from /api/auth/register
    password: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh72",
    phone: "9876543210",
    role: "USER",
    active: true,
    createdAt: new Date()
  },
  {
    name: "Admin User",
    email: "admin@cinebook.com",
    password: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh72",
    phone: "9000000001",
    role: "ADMIN",
    active: true,
    createdAt: new Date()
  }
]);

// ---------- 2. THEATRES ----------
db.theatres.insertMany([
  {
    name: "PVR Cinemas",
    location: "Forum Mall, Koramangala",
    city: "Bangalore",
    totalSeats: 150,
    rows: 10,
    columns: 15,
    active: true
  },
  {
    name: "INOX Multiplex",
    location: "GT World Mall, Mysore Road",
    city: "Bangalore",
    totalSeats: 150,
    rows: 10,
    columns: 15,
    active: true
  }
]);

// ---------- 3. MOVIES ----------
db.movies.insertMany([
  {
    title: "Kalki 2898 AD",
    description: "A mythological sci-fi epic set in the distant future.",
    genre: "Sci-Fi",
    language: "Telugu",
    durationMinutes: 181,
    director: "Nag Ashwin",
    cast: ["Prabhas", "Deepika Padukone", "Amitabh Bachchan"],
    posterUrl: "https://upload.wikimedia.org/wikipedia/en/9/96/Kalki_2898_AD_poster.jpg",
    rating: 7.3,
    releaseDate: "2024-06-27",
    certificate: "UA",
    active: true
  },
  {
    title: "Pushpa 2: The Rule",
    description: "Pushpa Raj establishes his supremacy in the smuggling world.",
    genre: "Action",
    language: "Telugu",
    durationMinutes: 190,
    director: "Sukumar",
    cast: ["Allu Arjun", "Rashmika Mandanna", "Fahadh Faasil"],
    posterUrl: "https://upload.wikimedia.org/wikipedia/en/d/d2/Pushpa_2_The_Rule_poster.jpg",
    rating: 7.8,
    releaseDate: "2024-12-05",
    certificate: "UA",
    active: true
  },
  {
    title: "Stree 2",
    description: "The spirit of Stree returns to haunt Chanderi village.",
    genre: "Horror",
    language: "Hindi",
    durationMinutes: 135,
    director: "Amar Kaushik",
    cast: ["Rajkummar Rao", "Shraddha Kapoor"],
    posterUrl: "",
    rating: 8.1,
    releaseDate: "2024-08-15",
    certificate: "UA",
    active: true
  },
  {
    title: "Deadpool & Wolverine",
    description: "Wade Wilson teams up with Wolverine in the MCU.",
    genre: "Action",
    language: "English",
    durationMinutes: 127,
    director: "Shawn Levy",
    cast: ["Ryan Reynolds", "Hugh Jackman"],
    posterUrl: "",
    rating: 8.0,
    releaseDate: "2024-07-26",
    certificate: "A",
    active: true
  }
]);

// ---------- USAGE NOTES ----------
print("✅ Sample data inserted!");
print("🔑 Use POST /api/auth/register to create users with proper BCrypt hashing");
print("🎬 Then use POST /api/admin/shows to create shows for these movies");
print("📋 Admin login: admin@cinebook.com / password123");
print("👤 User login:  durga@example.com / password123");
print("");
print("⚠️  IMPORTANT: The passwords above are example hashes.");
print("   Please register through the API to get properly hashed passwords!");
