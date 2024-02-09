package dk.easv.presentation.controller;

import dk.easv.APIService.APIConnection;
import dk.easv.entities.*;
import dk.easv.presentation.model.AppModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class AppController implements Initializable {
    //@FXML
    //private ListView<User> lvUsers;
    @FXML
    private ListView<Movie> lvTopForUser;
    @FXML
    private ListView<Movie> lvTopAvgNotSeen;
    @FXML
    private ListView<UserSimilarity> lvTopSimilarUsers;
    @FXML
    private ListView<TopMovie> lvTopFromSimilar;

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
        lvTopSimilarUsers.setItems(model.getObsSimilarUsers());
        lvTopFromSimilar.setItems(model.getObsTopMoviesSimilarUsers());

        startTimer("Load users");
        model.loadUsers();
        stopTimer();

        /*
        lvUsers.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldUser, selectedUser) -> {
                    startTimer("Loading all data for user: " + selectedUser);
                    model.loadData(selectedUser);
                });*/

        //Select the logged-in user in the listview, automagically trigger the listener above
        //lvUsers.getSelectionModel().select(model.getObsLoggedInUser());


    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setCustomCellFactory(lvTopForUser);
        setCustomCellFactory(lvTopAvgNotSeen);
        setCustomCellFactory(lvTopSimilarUsers);
        setCustomCellFactory(lvTopFromSimilar);
    }

    // Set custom cell factory for ListView to display movie images
    private <T> void setCustomCellFactory(ListView<T> listView) {
        listView.setCellFactory(param -> new ListCell<T>() {
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();

            {
                titleLabel.setAlignment(Pos.BOTTOM_LEFT); // Align the title in the center
                titleLabel.setWrapText(true); // Allow the title to wrap if it's too long
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
                        //System.out.println(imageUrl);
                        if (imageUrl != null) {
                            imageView.setFitHeight(250);
                            imageView.setFitWidth(200);
                            imageView.setPreserveRatio(true);
                            imageView.setImage(new Image(imageUrl, true)); // When background loading is enabled, the image will be loaded asynchronously (not freezing the UI)
                            titleLabel.setText(((Movie)item).getTitle()); // Set the title of the movie
                            setGraphic(new VBox(imageView, titleLabel));
                        } else {
                            setText("No Image Available");
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
                return "https://images.unsplash.com/photo-1706859450156-0214dca4260d?q=80&w=2080&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"; //TODO: put whatever image you want to display when no poster is found
            }

            return movie.getPosterPath();
        }
        // Add other conditions for different item types if needed
        return null;
    }
}