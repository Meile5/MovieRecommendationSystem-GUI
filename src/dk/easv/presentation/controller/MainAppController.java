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
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;


public class MainAppController implements Initializable {
    public Label topRatedMoviesLbl;
    public Label MovieSuggestionsLabel;
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

        lvTopForUser.setItems(model.getObsTopMovieSeen());
        lvTopAvgNotSeen.setItems(model.getObsTopMovieNotSeen());
        lvTopFromSimilar.setItems(model.getObsTopMoviesSimilarUsers());

        startTimer("Load users");
        model.loadUsers();
        stopTimer();


        //searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        //    filterMovies(newValue);
        //});


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
                titleLabel.setMaxWidth(330);
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
                            imageView.setFitHeight(200);
                            imageView.setFitWidth(330);
                            imageView.setImage(new Image(imageUrl, true));
                            titleLabel.setText(movie.getTitle());


                            VBox vBox = new VBox();
                            vBox.getChildren().addAll(imageView, titleLabel);
                            vBox.setMaxWidth(340);
                            vBox.setMaxHeight(240);
                            vBox.setSpacing(-10);

                            // Add event handlers to change border color when mouse enters and exits
                            vBox.setOnMouseEntered(event -> {
                                vBox.setStyle("-fx-border-color: white; -fx-border-width: 2px;");
                            });

                            vBox.setOnMouseExited(event -> {
                                vBox.setStyle(null);
                            });


                            setGraphic(vBox);

                        }else{
                        VBox vBox = new VBox();
                        vBox.setSpacing(0);
                        vBox.setPadding(new Insets(0, 0, 0, 0));
                        setGraphic(vBox);}

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
                //return "https://image.tmdb.org/t/p/original/qf55kqMNxU4RbUcLqk5xZIZrIVy.jpg";
                //return "";
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
            // Hide other ListView objects when filtering
            lvTopAvgNotSeen.setVisible(false);
            lvTopFromSimilar.setVisible(false);
            topRatedMoviesLbl.setVisible(false);
            MovieSuggestionsLabel.setVisible(false);
            // Make other ListView objects visible again when filter is not applied
            if (searchTerm.isEmpty()) {
                lvTopAvgNotSeen.setVisible(true);
                lvTopFromSimilar.setVisible(true);
                topRatedMoviesLbl.setVisible(true);
                MovieSuggestionsLabel.setVisible(true);
            }
            
        });
    }



}