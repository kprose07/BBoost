package application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Random;

public class BBoost extends Application {
    private final Random random = new Random();

    @Override
    public void start(Stage primaryStage) {
        TabPane tabPane = new TabPane();

        // Creating tabs
        Tab dashboardTab = new Tab("Dashboard");
        Tab notesTab = new Tab("Notes (Affirmations)");
        Tab checkInTab = new Tab("Check-Ins");
        Tab journalTab = new Tab("Journals");
        Tab reportsTab = new Tab("Reports");
        Tab preferencesTab = new Tab("Preferences");

        // Setting place holders for now
        dashboardTab.setContent(new StackPane());
        notesTab.setContent(new StackPane());
        checkInTab.setContent(new StackPane());
        journalTab.setContent(new StackPane());
        reportsTab.setContent(new StackPane());
        preferencesTab.setContent(new StackPane());

        // Adding tabs to TabPane
        tabPane.getTabs().addAll(dashboardTab, notesTab, checkInTab, journalTab, reportsTab, preferencesTab);

        // Creating scene
        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setTitle("Bannan Boost");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start random pop-ups
        startRandomPopups();
    }

    private void startRandomPopups() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(random.nextInt(30) + 30), event -> showPopup()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void showPopup() {
        Stage popup = new Stage();
        StackPane root = new StackPane();
        Label label = new Label(getRandomMessage());
        root.getChildren().add(label);
        Scene scene = new Scene(root, 200, 150);
        popup.setScene(scene);
        popup.setTitle("Reminder");
        popup.show();
    }

    private String getRandomMessage() {
        String[] messages = {
                "You are beautiful!",
                "Daily Check-IN: How are you feeling?",
                "Self Reflection: What did you enjoy today?"
        };
        return messages[random.nextInt(messages.length)];
    }

    public static void main(String[] args) {
        launch(args);
    }
}
