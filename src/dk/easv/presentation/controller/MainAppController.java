package dk.easv.presentation.controller;

import dk.easv.entities.Movie;
import dk.easv.entities.TopMovie;
import dk.easv.entities.User;
import dk.easv.entities.UserSimilarity;
import dk.easv.presentation.model.AppModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;


public class MainAppController implements Initializable {
    @FXML
    private ListView<User> lvUsers;
    @FXML
    private ListView<Movie> lvTopForUser;
    @FXML
    private ListView<Movie> lvTopAvgNotSeen;
    @FXML
    private ListView<UserSimilarity> lvTopSimilarUsers;
    @FXML
    private ListView<TopMovie> lvTopFromSimilar;
    @FXML
    private TextField searchField;
    @FXML
    private Label blockBusterMoviesLbl;
    private AppModel model;
    private long timerStartMillis = 0;
    private String timerMsg = "";


    private void startTimer(String message) {
        timerStartMillis = System.currentTimeMillis();
        timerMsg = message;
    }

    private void stopTimer() {
        System.out.println(timerMsg + " took : " + (System.currentTimeMillis() - timerStartMillis) + "ms");
    }

    public void setModel(AppModel model) {
        this.model = model;

        lvTopForUser.setItems(model.getObsTopMovieNotSeen());
        lvTopAvgNotSeen.setItems(model.getObsTopMovieNotSeen());
        lvTopFromSimilar.setItems(model.getObsTopMoviesSimilarUsers());

        startTimer("Load users");
        model.loadUsers();
        stopTimer();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterMovies(newValue);
        });


    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setCustomCellFactory(lvTopForUser);
        setCustomCellFactory(lvTopAvgNotSeen);
        setCustomCellFactory(lvTopFromSimilar);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterMovies(newValue); // Call filterMovies method whenever the text changes
        });
    }

    // Set custom cell factory for ListView to display movie images
    private <T> void setCustomCellFactory(ListView<T> listView) {

        listView.setCellFactory(param -> new ListCell<T>() {
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();

            {
                titleLabel.setAlignment(Pos.BOTTOM_LEFT); // Align the title in the center
                titleLabel.setWrapText(true); // Allow the title to wrap if it's too long
                titleLabel.setMaxWidth(370);
                setContentDisplay(ContentDisplay.BOTTOM); // Display the graphic (image) above the text (title)
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    try {
                        String imageUrl = getImageUrl(item);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Movie movie = item instanceof TopMovie ? ((TopMovie) item).getMovie() : (Movie) item;
                            imageView.setFitHeight(220);
                            imageView.setFitWidth(350);
                            imageView.setImage(new Image(imageUrl, true));
                            titleLabel.setText(movie.getTitle());


                            VBox vBox = new VBox();
                            vBox.getChildren().addAll(imageView, titleLabel);
                            vBox.setMaxWidth(380);
                            vBox.setMaxHeight(220);

                            setGraphic(vBox);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        // Handle IO exception
                    }
                }
            }

        });
    }

    // Get image URL based on the item type
    private <T> String getImageUrl(T item) throws IOException {
        if (item instanceof Movie movie) {
            if (Objects.equals(movie.getPosterPath(), "NO POSTER FOUND")) {
                return null;
            }

            return movie.getPosterPath();
        }
        else if (item instanceof TopMovie tm) {
            return getImageUrl(((TopMovie)item).getMovie());
        }
        // Add other conditions for different item types if needed
        return null;
    }

    private void filterMovies(String searchTerm) {
        // Get the list of all movies from the model
        List<Movie> allMovies = model.getObsTopMovieNotSeen();

        // Create a list to store filtered movies
        List<Movie> filteredMovies = new ArrayList<>();

        // Filter movies based on the search term
        for (Movie movie : allMovies) {
            if (movie.getTitle().toLowerCase().trim().contains(searchTerm.toLowerCase().trim())) {
                filteredMovies.add(movie);
            }
        }

        // Convert the filtered list to ObservableList
        ObservableList<Movie> observableFilteredMovies = FXCollections.observableArrayList(filteredMovies);

        Platform.runLater(() -> {
            lvTopForUser.setItems(observableFilteredMovies);
            blockBusterMoviesLbl.setText(searchTerm.isEmpty() ? "Blockbuster Movies" : "Search result: " + searchTerm);
        });
    }



}