package dk.easv.APIService;

import dk.easv.entities.Movie;
import dk.easv.presentation.model.AppModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LoadPosterUrls {
    private final APIConnection apiConnection = new APIConnection();
    private final String FILE_PATH = "data/movie_posters.txt";
    private final ConcurrentHashMap<Integer, Integer> movieIds = new ConcurrentHashMap<>(); // For storing API's movie IDs <app movie ID, API movie ID>
    private static final ConcurrentHashMap<Integer, String> moviePosters = new ConcurrentHashMap<>();
    private List<Movie> movies = new ArrayList<>();
    private AppModel model;

    public static void main(String[] args) {
        System.out.println("hey");
        var time = System.currentTimeMillis();
        System.out.println("Starting...");
        System.out.println("Buckle up, this will take a while.");
        LoadPosterUrls loadPosterUrls = new LoadPosterUrls();

        // Load all movies from file
        System.out.println("Loading movies from a file...");
        loadPosterUrls.loadAllMovies();
        updateAndSaveUrlsInFile();
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
        System.out.println("hey");
        int apiMovieId = movieIds.get(movieId);
        String url = apiConnection.getMovieImages(apiMovieId);
        if (url.matches("NO POSTER FOUND")){
            url = apiConnection.getTVImages(apiMovieId);
        }
        System.out.println(url);
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

    public void loadAllMovies() {
       // List<Movie> movies = new ArrayList<>();
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
       // return movies;
    }



    private void getMovieId(Movie movie) {
        try {
            int movieId;
            if(!movie.getTitle().toLowerCase().contains("season")){
                movieId = apiConnection.searchMovie(movie);
            }else{
                movieId = apiConnection.SearchTVShow(movie);
            }
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
    private static void updateAndSaveUrlsInFile() {
        try {
            // Read existing file content
            List<String> lines = Files.readAllLines(Path.of("data/movie_posters.txt"));
            FileOutputStream fileOutputStream = new FileOutputStream("data/movie_posters.txt");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

            // Update URLs and write to the file
            for (String line : lines) {
                String[] parts = line.split(",");
                int movieId = Integer.parseInt(parts[0]);
                if (moviePosters.containsKey(movieId)) {
                    line = movieId + "," + moviePosters.get(movieId);
                }
                writer.write(line + "\n");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
