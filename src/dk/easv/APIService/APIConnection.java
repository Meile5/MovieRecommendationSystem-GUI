package APIService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class APIConnection {
    private static final String API_KEY = "YOUR_TMDB_API_KEY";
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String SEARCH_MOVIE_ENDPOINT = "/search/movie";
    private static final String MOVIE_IMAGES_ENDPOINT = "/movie/%d/images";

    /**
     * Searches for a movie by title using the TMDb API.
     * @param title The title of the movie to search for.
     * @return JSON string containing search results.
     * @throws IOException If an error occurs during the HTTP request.
     */
    public String searchMovie(String title) throws IOException {
        String encodedTitle = title.replace(" ", "%20");
        String endpoint = BASE_URL + SEARCH_MOVIE_ENDPOINT + "?api_key=" + API_KEY + "&query=" + encodedTitle;

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            throw new IOException("Failed to search for movie. HTTP error code: " + responseCode);
        }
    }

    /**
     * Extracts the movie ID from the search results JSON response.
     * @param searchResult JSON string containing search results.
     * @return The movie ID.
     */
    public int extractMovieIdFromResponse(String searchResult) {
        Gson gson = new Gson();
        JsonObject jsonObject = JsonParser.parseString(searchResult).getAsJsonObject();
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");
        if (resultsArray.size() > 0) {
            JsonObject firstResult = resultsArray.get(0).getAsJsonObject();
            return firstResult.get("id").getAsInt();
        } else {
            return -1; // Movie not found
        }
    }

    /**
     * Fetches image URLs for a movie by its ID using the TMDb API.
     * @param movieId The ID of the movie.
     * @return List of image URLs.
     * @throws IOException If an error occurs during the HTTP request.
     */
    public List<String> getMovieImages(int movieId) throws IOException {
        String endpoint = String.format(BASE_URL + MOVIE_IMAGES_ENDPOINT, movieId) + "?api_key=" + API_KEY;

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            List<String> imageUrls = new ArrayList<>();
            Gson gson = new Gson();
            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonArray postersArray = jsonObject.getAsJsonArray("posters");
            for (JsonElement posterElement : postersArray) {
                JsonObject posterObject = posterElement.getAsJsonObject();
                String imageUrl = "https://image.tmdb.org/t/p/original" + posterObject.get("file_path").getAsString();
                imageUrls.add(imageUrl);
            }
            return imageUrls;
        } else {
            throw new IOException("Failed to fetch movie images. HTTP error code: " + responseCode);
        }
    }
}
