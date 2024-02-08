package dk.easv.APIService;

import dk.easv.entities.Movie;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LoadPosterUrls {
    private final APIConnection apiConnection = new APIConnection();
    private final String FILE_PATH = "data/movie_posters.txt";
    private final ConcurrentHashMap<Integer, Integer> movieIds = new ConcurrentHashMap<>(); // For storing API's movie IDs <app movie ID, API movie ID>
    private final ConcurrentHashMap<Integer, String> moviePosters = new ConcurrentHashMap<>();
    private List<Movie> movies = new ArrayList<>();

    public static void main(String[] args) {
        var time = System.currentTimeMillis();
        System.out.println("Starting...");
        System.out.println("Buckle up, this will take a while.");
        LoadPosterUrls loadPosterUrls = new LoadPosterUrls();

        // Load all movies from file
        System.out.println("Loading movies from a file...");
        loadPosterUrls.loadAllMovies();
        List<Integer> listBreakpoints = loadPosterUrls.getListBreakpoints(10);

        // Get movie IDs from the API
        System.out.println("Trying to get movie IDs...");
        List<Thread> threads = getThreadListToGetMovieIds(listBreakpoints, loadPosterUrls);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Movie IDs loaded after: " + (System.currentTimeMillis() - time) + "ms");

        // Get poster URLs from the API
        System.out.println("Trying to get poster URLs...");
        threads = getThreadListToGetPosters(listBreakpoints, loadPosterUrls);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        loadPosterUrls.saveUrlsInFile();
        System.out.println("Finished in: " + (System.currentTimeMillis() - time) + "ms");
    }

    private static List<Thread> getThreadListToGetPosters(List<Integer> listBreakpoints, LoadPosterUrls loadPosterUrls) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < listBreakpoints.size() - 1; i++) {
            int start = listBreakpoints.get(i);

            int end = listBreakpoints.get(i + 1);
            Thread thread = new Thread(() -> {
                for (int j = start; j < end; j++) {
                    try {
                        loadPosterUrls.getPosterUrl(j);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        // We don't need movie with ID 0 anyways because it doesn't exist.
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

    private static List<Thread> getThreadListToGetMovieIds(List<Integer> listBreakpoints, LoadPosterUrls loadPosterUrls) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < listBreakpoints.size() - 1; i++) {
            int start = listBreakpoints.get(i);
            int end = listBreakpoints.get(i + 1);
            Thread thread = new Thread(() -> {
                for (int j = start; j < end; j++) {
                    loadPosterUrls.getMovieId(loadPosterUrls.movies.get(j));
                }
            });
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

    // Send a request to the API to get the poster URL for a movie
    private void getPosterUrl(int movieId) throws IOException {
        int apiMovieId = movieIds.get(movieId);
        String url = apiConnection.getMovieImages(apiMovieId);
        moviePosters.put(movieId, url);
    }

    // Append to a file
    private void saveUrlsInFile() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(FILE_PATH);
            for (Integer movieId : moviePosters.keySet()) {
                String line = movieId + "," + moviePosters.get(movieId) + "\n";
                fileOutputStream.write(line.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadAllMovies() {
        try {
            List<String> movieLines = Files.readAllLines(Path.of("data/movie_titles.txt"));
            for (String movieLine : movieLines) {
                String[] split = movieLine.split(",");
                Movie movie = new Movie(Integer.parseInt(split[0]), split[2], Integer.parseInt(split[1]));
                movies.add(movie);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getMovieId(Movie movie) {
        try {
            int movieId = apiConnection.searchMovie(movie);
            movieIds.put(movie.getId(), movieId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getListBreakpoints(int numberOfThreads) {
        List<Integer> listBreakpoints = new ArrayList<>();
        int chunkSize = movies.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            listBreakpoints.add(i * chunkSize);
        }
        listBreakpoints.add(movies.size());
        return listBreakpoints;
    }
}
