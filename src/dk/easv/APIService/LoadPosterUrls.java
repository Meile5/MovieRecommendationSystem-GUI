package dk.easv.APIService;

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
    private final List<Integer> movieIds = new ArrayList<>();
    private final ConcurrentHashMap<Integer, String> moviePosters = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        var time = System.currentTimeMillis();
        System.out.println("Start");
        LoadPosterUrls loadPosterUrls = new LoadPosterUrls();
        loadPosterUrls.loadMovieIds();
        List<Integer> listBreakpoints = loadPosterUrls.getListBreakpoints(10);
        List<Thread> threads = getThreadList(listBreakpoints, loadPosterUrls);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        loadPosterUrls.saveUrlsInFile();
        System.out.println("End");
        System.out.println("Time: " + (System.currentTimeMillis() - time));
    }

    private static List<Thread> getThreadList(List<Integer> listBreakpoints, LoadPosterUrls loadPosterUrls) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < listBreakpoints.size() - 1; i++) {
            int start = listBreakpoints.get(i);
            int end = listBreakpoints.get(i + 1);
            Thread thread = new Thread(() -> {
                for (int j = start; j < end; j++) {
                    try {
                        loadPosterUrls.getPosterUrl(loadPosterUrls.movieIds.get(j));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        return threads;
    }

    // Send a request to the API to get the poster URL for a movie
    private void getPosterUrl(int movieId) throws IOException {
        String url = apiConnection.getMovieImages(movieId);
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

    private void loadMovieIds() {
        try {
            List<String> movieLines = Files.readAllLines(Path.of("data/movie_titles.txt"));
            for (String movieLine : movieLines) {
                String[] split = movieLine.split(",");
                movieIds.add(Integer.parseInt(split[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getListBreakpoints(int numberOfThreads) {
        List<Integer> listBreakpoints = new ArrayList<>();
        int chunkSize = movieIds.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            listBreakpoints.add(i * chunkSize);
        }
        listBreakpoints.add(movieIds.size());
        return listBreakpoints;
    }
}
