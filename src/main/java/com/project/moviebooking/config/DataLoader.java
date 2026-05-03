package com.project.moviebooking.config;

import com.project.moviebooking.model.*;
import com.project.moviebooking.repository.*;
import com.project.moviebooking.service.ShowRefreshService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * DataLoader — Auto-populates MongoDB on startup (idempotent)
 * ============================================================
 * STEP 1: Tries to fetch REAL movies from TMDB API
 * STEP 2: If TMDB key not configured → uses curated Indian movie list
 * STEP 3: Creates 10 Bengaluru theatres
 * STEP 4: Creates 3 shows per movie per theatre × 3 days
 * STEP 5: Auto-generates 150 seats per show (VIP/PREMIUM/REGULAR)
 * ============================================================
 */
@Component
@RequiredArgsConstructor
public class DataLoader {

    private final TheatreRepository theatreRepository;
    private final MovieRepository   movieRepository;
    private final ShowRepository    showRepository;
    private final SeatRepository    seatRepository;
    private final ShowRefreshService showRefreshService;

    @Value("${tmdb.api.key:YOUR_TMDB_API_KEY_HERE}")
    private String tmdbApiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String tmdbBaseUrl;

    @Value("${tmdb.image.base-url:https://image.tmdb.org/t/p/w500}")
    private String tmdbImageUrl;

    @PostConstruct
    public void loadData() {
        System.out.println("\n🎬 [DATALOADER] Starting CineBook data initialization...");

        loadTheatres();
        loadMovies();

        // ── Delegate all show/seat generation to ShowRefreshService ──
        // Idempotent: generates shows only for dates that don't have them yet.
        // Uses IST (Asia/Kolkata) so shows are always correct regardless of server TZ.
        showRefreshService.ensureShowsExist();

        System.out.println("✅ [DATALOADER] Database ready!");
        System.out.printf("   Theatres: %d  |  Movies: %d  |  Shows: %d  |  Seats: %d%n%n",
            theatreRepository.count(), movieRepository.count(),
            showRepository.count(), seatRepository.count());
    }

    // ─────────────────────────────────────────────────────────────────
    // THEATRES
    // ─────────────────────────────────────────────────────────────────
    private List<Theatre> loadTheatres() {
        if (theatreRepository.count() > 0) {
            System.out.println("ℹ️  [DATALOADER] Theatres already loaded.");
            return theatreRepository.findAll();
        }
        List<Theatre> theatres = List.of(
            buildTheatre("PVR Orion Mall",       "Rajajinagar, Bengaluru"),
            buildTheatre("INOX Garuda Mall",      "Magrath Road, Bengaluru"),
            buildTheatre("Cinepolis Forum Mall",   "Koramangala, Bengaluru"),
            buildTheatre("PVR VR Bengaluru",       "Whitefield, Bengaluru"),
            buildTheatre("INOX Mantri Square",     "Malleswaram, Bengaluru"),
            buildTheatre("Cinepolis Nexus",        "Churchstreet, Bengaluru"),
            buildTheatre("PVR Gold Gopalan",       "Old Madras Road, Bengaluru"),
            buildTheatre("Urvashi Theatre",        "Lalbagh Road, Bengaluru"),
            buildTheatre("Innovative Multiplex",   "Marathahalli, Bengaluru"),
            buildTheatre("Swagath Cinema",         "JP Nagar, Bengaluru")
        );
        List<Theatre> saved = theatreRepository.saveAll(theatres);
        System.out.println("✅ [DATALOADER] Inserted " + saved.size() + " theatres.");
        return saved;
    }

    private Theatre buildTheatre(String name, String address) {
        Theatre t = new Theatre();
        t.setName(name); t.setCity("Bengaluru"); t.setAddress(address);
        t.setRows(10); t.setColumns(15); t.setTotalSeats(150); t.setActive(true);
        return t;
    }

    // ─────────────────────────────────────────────────────────────────
    // MOVIES — Try TMDB first, fallback to curated list
    // ─────────────────────────────────────────────────────────────────
    private List<Movie> loadMovies() {
        if (movieRepository.count() > 0) {
            System.out.println("ℹ️  [DATALOADER] Movies already loaded.");
            return movieRepository.findAll();
        }

        List<Movie> movies = null;

        // Try TMDB if API key is configured
        if (!"YOUR_TMDB_API_KEY_HERE".equals(tmdbApiKey) && tmdbApiKey != null && !tmdbApiKey.isBlank()) {
            System.out.println("🌐 [DATALOADER] Fetching movies from TMDB API...");
            movies = fetchFromTMDB();
        }

        // Fallback: curated Indian movie list
        if (movies == null || movies.isEmpty()) {
            System.out.println("📋 [DATALOADER] Using curated movie list (add TMDB API key for real posters).");
            movies = fallbackMovies();
        }

        List<Movie> saved = movieRepository.saveAll(movies);
        System.out.println("✅ [DATALOADER] Inserted " + saved.size() + " movies.");
        return saved;
    }

    /**
     * Fetches 6 Indian movies from TMDB using CORRECT movie IDs.
     * Uses a HYBRID approach: start with curated list, enrich with TMDB data.
     * If TMDB fails for any movie, the curated data is used as fallback.
     *
     * Verified TMDB IDs (2024):
     *   KGF Ch2=649609 · Pushpa2=1241674 · Stree2=773902
     *   Salaar=889737 · Kalki2899=1139826 · Animal=1188747
     */
    @SuppressWarnings("unchecked")
    private List<Movie> fetchFromTMDB() {

        // Curated base list with real TMDB poster paths (CDN-direct) as safe fallback
        record IndianMovie(int id, String title, String genre, String language,
                           int duration, double rating, String cert, String posterPath, String desc) {}

        List<IndianMovie> targets = List.of(
            new IndianMovie(649609,  "KGF Chapter 2",       "Action",        "Kannada", 168, 8.4, "A",
                "/oaOECraZGSEqy0hkzxCrJ3ueSWT.jpg",
                "Rocky's deadly empire attracts the attention of Ramika Sen, the new PM of India, who launches a war against crime."),
            new IndianMovie(1241674, "Pushpa 2 The Rule",   "Action",        "Telugu",  179, 7.9, "A",
                "/jXzmJqXm6SAA0QaJFgUh92hhBnG.jpg",
                "Pushpa Raj expands his red sandalwood smuggling empire, facing deadly adversaries and a new threat from the police."),
            new IndianMovie(773902,  "Stree 2",             "Horror-Comedy", "Hindi",   137, 8.5, "UA",
                "/3o1PPMqtDDK6N2Ec1GhHpBB5RqK.jpg",
                "The legend of Stree returns to Chanderi with a new terrifying chapter in the supernatural horror comedy saga."),
            new IndianMovie(889737,  "Salaar",              "Action",        "Kannada", 170, 7.8, "A",
                "/dNwHwQ9O9UVaZbhR9lJ5hOjYFLq.jpg",
                "A ferocious man and peaceful man are two childhood friends who hold the key to each other's destiny."),
            new IndianMovie(1139826, "Kalki 2899 AD",       "Sci-Fi",        "Telugu",  181, 7.7, "UA",
                "/1XMM5SWAV5VrdqyCO34vHTWyRQJ.jpg",
                "Set in the far future — the final avatar of Vishnu rises in a dystopian world to protect the last hope of humanity."),
            new IndianMovie(1188747, "Animal",              "Action-Drama",  "Hindi",   201, 7.6, "A",
                "/rTbHMSFH8rJqhPFllhgJMtHoGWG.jpg",
                "A son's obsessive love for his estranged father cascades into violent territory, setting off a chain of events.")
        );

        WebClient client = WebClient.builder()
                .baseUrl(tmdbBaseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        List<Movie> movies = new ArrayList<>();

        for (IndianMovie target : targets) {
            Movie movie = new Movie();
            // Pre-fill from curated data (reliable fallback)
            movie.setTitle(target.title());
            movie.setGenre(target.genre());
            movie.setLanguage(target.language());
            movie.setDurationMinutes(target.duration());
            movie.setRating(target.rating());
            movie.setCertificate(target.cert());
            movie.setPosterUrl(tmdbImageUrl + target.posterPath());
            movie.setDescription(target.desc());
            movie.setActive(true);

            // Try to enrich with live TMDB data (overwrites if successful)
            try {
                Map<String, Object> data = client.get()
                    .uri("/movie/{id}?api_key={key}&language=en-US", target.id(), tmdbApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();

                if (data != null) {
                    // Enrich description and rating from TMDB
                    String overview = (String) data.get("overview");
                    if (overview != null && !overview.isBlank())
                        movie.setDescription(overview);

                    Number voteAvg = (Number) data.get("vote_average");
                    if (voteAvg != null) movie.setRating(voteAvg.doubleValue());

                    Number runtime = (Number) data.get("runtime");
                    if (runtime != null && runtime.intValue() > 0)
                        movie.setDurationMinutes(runtime.intValue());

                    String poster = (String) data.get("poster_path");
                    if (poster != null) movie.setPosterUrl(tmdbImageUrl + poster);

                    System.out.println("  ✅ TMDB enriched: " + movie.getTitle());
                }
            } catch (Exception e) {
                System.out.println("  📋 Using curated data for: " + movie.getTitle()
                    + " (TMDB: " + e.getMessage().split("\n")[0] + ")");
            }

            movies.add(movie);
        }

        return movies;
    }

    /** Curated fallback — used when no TMDB API key is set */
    private List<Movie> fallbackMovies() {
        return new ArrayList<>(List.of(
            buildMovie("KGF Chapter 3",        "Action",        "Kannada", 165, 9.0, "UA",
                "https://image.tmdb.org/t/p/w500/oaOECraZGSEqy0hkzxCrJ3ueSWT.jpg",
                "KGF Chapter 3 — the legendary saga continues. Rocky's empire faces its greatest threat yet."),
            buildMovie("Pushpa 2 The Rule",    "Action",        "Telugu",  190, 8.5, "A",
                "https://image.tmdb.org/t/p/w500/jXzmJqXm6SAA0QaJFgUh92hhBnG.jpg",
                "Pushpa Raj expands his red sandalwood smuggling empire while battling deadly adversaries."),
            buildMovie("Stree 3",              "Horror-Comedy", "Hindi",   140, 8.8, "UA",
                "https://image.tmdb.org/t/p/w500/3o1PPMqtDDK6N2Ec1GhHpBB5RqK.jpg",
                "The legend of Stree returns to Chanderi — this time with a new terrifying twist."),
            buildMovie("RRR 2",                "Action-Drama",  "Telugu",  185, 9.2, "UA",
                "https://image.tmdb.org/t/p/w500/nEufeZlyAOLqO2brrs0yeF1lgXO.jpg",
                "Ram and Bheem unite again in this epic tale of friendship, sacrifice, and revolution."),
            buildMovie("Kalki 2899 AD",        "Sci-Fi",        "Telugu",  181, 8.2, "UA",
                "https://image.tmdb.org/t/p/w500/1XMM5SWAV5VrdqyCO34vHTWyRQJ.jpg",
                "Set in the far future — the final avatar of Vishnu rises to save humanity."),
            buildMovie("Salaar 2",             "Action",        "Kannada", 170, 8.3, "UA",
                "https://image.tmdb.org/t/p/w500/dNwHwQ9O9UVaZbhR9lJ5hOjYFLq.jpg",
                "Salaar — a man of his word, returns to fulfill a blood oath in the most brutal world.")
        ));
    }

    private Movie buildMovie(String title, String genre, String language,
                             int duration, double rating, String certificate,
                             String posterUrl, String description) {
        Movie m = new Movie();
        m.setTitle(title); m.setGenre(genre); m.setLanguage(language);
        m.setDurationMinutes(duration); m.setRating(rating);
        m.setCertificate(certificate); m.setPosterUrl(posterUrl);
        m.setDescription(description); m.setActive(true);
        return m;
    }

}
