package dk.easv.presentation.controller;

import dk.easv.entities.Movie;
import dk.easv.entities.TopMovie;
import dk.easv.entities.User;
import dk.easv.entities.UserSimilarity;
import dk.easv.presentation.model.AppModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
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
        //lvTopSimilarUsers.setItems(model.getObsSimilarUsers());
        lvTopFromSimilar.setItems(model.getObsTopMoviesSimilarUsers());

        startTimer("Load users");
        model.loadUsers();
        stopTimer();


        /*lvUsers.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, oldUser, selectedUser) -> {
                    startTimer("Loading all data for user: " + selectedUser);
                    model.loadData(selectedUser);
                });
        //Select the logged-in user in the listview, automagically trigger the listener above
        lvUsers.getSelectionModel().select(model.getObsLoggedInUser());*/


    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setCustomCellFactory(lvTopForUser);
        setCustomCellFactory(lvTopAvgNotSeen);
        // setCustomCellFactory(lvTopSimilarUsers);
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
                            imageView.setPreserveRatio(true);
                            imageView.setFitHeight(220);
                            imageView.setImage(new Image(imageUrl, true));
                            titleLabel.setText(movie.getTitle());


                            VBox vBox = new VBox(imageView, titleLabel);
                            vBox.setMaxWidth(370);
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
                // return "https://img.freepik.com/free-photo/movie-background-collage_23-2149876010.jpg?w=1380&t=st=1707493292~exp=1707493892~hmac=99da9616d90f0d2f44960de681c9dbf9b02090cb26818d371374e831b72f0cf9"; //TODO: put whatever image you want to display when no poster is found
            }

            return movie.getPosterPath();
        }
        else if (item instanceof TopMovie tm) {
            return getImageUrl(((TopMovie)item).getMovie());
        }
        // Add other conditions for different item types if needed
        return null;
    }
}