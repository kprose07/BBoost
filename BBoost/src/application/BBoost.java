package application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BBoost extends Application {
    private final Random random = new Random();
    private static final String AFFIRMATIONS_FILE = "affirmations.txt";
    private static final String CHECKINS_FILE = "checkins.txt";
    private static final String JOURNALS_FILE = "journals.txt";
    
    private VBox checkInVBox = new VBox(10);
    private VBox journalVBox = new VBox(10);
    private VBox affirmationVBox = new VBox(10);  // New VBox for Affirmations
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
        primaryStage.getIcons().add(icon);

        // Create tab pane and tabs
        TabPane tabPane = new TabPane();
        Tab dashboardTab = new Tab("Dashboard");
        Tab notesTab = new Tab("Notes (Affirmations)");
        Tab checkInTab = new Tab("Check-Ins");
        Tab journalTab = new Tab("Journals");
        Tab reportsTab = new Tab("Reports");
        Tab preferencesTab = new Tab("Preferences");

        // Buttons to clear each section
        Button clearAffirmationsButton = new Button("Clear Affirmations");
        Button clearCheckInsButton = new Button("Clear Check-Ins");
        Button clearJournalsButton = new Button("Clear Journals");

        clearAffirmationsButton.setOnAction(e -> clearFile(AFFIRMATIONS_FILE, affirmationVBox));
        clearCheckInsButton.setOnAction(e -> clearFile(CHECKINS_FILE, checkInVBox));
        clearJournalsButton.setOnAction(e -> clearFile(JOURNALS_FILE, journalVBox));

        // Layouts for each tab
        VBox affirmationLayout = new VBox(10, clearAffirmationsButton, affirmationVBox);
        VBox checkInLayout = new VBox(10, clearCheckInsButton, checkInVBox);
        VBox journalLayout = new VBox(10, clearJournalsButton, journalVBox);

        notesTab.setContent(affirmationLayout);
        checkInTab.setContent(checkInLayout);
        journalTab.setContent(journalLayout);
        dashboardTab.setContent(new StackPane());
        reportsTab.setContent(new StackPane());
        preferencesTab.setContent(new StackPane());

        // Add tabs to TabPane
        tabPane.getTabs().addAll(dashboardTab, notesTab, checkInTab, journalTab, reportsTab, preferencesTab);

        // Create scene
        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setTitle("Bannan Boost");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start random pop-ups
        startRandomPopups();

        // Load saved entries
        updateCheckInTab();
        updateJournalTab();
        updateAffirmationTab();
    }

    private void startRandomPopups() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(random.nextInt(20) + 10), event -> showPopup())); 
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void showPopup() {
        int popupType = random.nextInt(3);
        switch (popupType) {
            case 0: showAffirmationPopup(); break;
            case 1: showCheckInPopup(); break;
            case 2: showJournalPopup(); break;
        }
    }

    private void showAffirmationPopup() {
        Stage popup = new Stage();
        StackPane root = new StackPane();
        Label label = new Label(getRandomAffirmation());
        root.getChildren().add(label);
        Scene scene = new Scene(root, 200, 150);
        popup.setScene(scene);
        popup.setTitle("Affirmation");
        popup.show();

        saveAffirmation(label.getText());
        updateAffirmationTab();
    }

    private void showCheckInPopup() {
        Stage popup = new Stage();
        VBox vbox = new VBox(10);
        Label label = new Label("How are you feeling today?");
        Button happyButton = new Button("Happy");
        Button neutralButton = new Button("Neutral");
        Button sadButton = new Button("Sad");
        Button angryButton = new Button("Angry");
        Button relaxedButton = new Button("Relaxed");

        vbox.getChildren().addAll(label, happyButton, neutralButton, sadButton, angryButton, relaxedButton);
        happyButton.setOnAction(e -> { saveCheckIn("Happy"); popup.close(); updateCheckInTab(); });
        neutralButton.setOnAction(e -> { saveCheckIn("Neutral"); popup.close(); updateCheckInTab(); });
        sadButton.setOnAction(e -> { saveCheckIn("Sad"); popup.close(); updateCheckInTab(); });
        angryButton.setOnAction(e -> { saveCheckIn("Angry"); popup.close(); updateCheckInTab(); });
        relaxedButton.setOnAction(e -> { saveCheckIn("Relaxed"); popup.close(); updateCheckInTab(); });

        Scene scene = new Scene(vbox, 300, 250);
        popup.setScene(scene);
        popup.setTitle("Daily Check-In");
        popup.show();
    }

    private void showJournalPopup() {
        Stage popup = new Stage();
        VBox vbox = new VBox(10);
        Label label = new Label("How are you feeling today?");
        TextArea textArea = new TextArea();
        Button saveButton = new Button("Save Journal");

        saveButton.setOnAction(e -> {
            saveJournal(textArea.getText());
            updateJournalTab();
            popup.close();
        });

        vbox.getChildren().addAll(label, textArea, saveButton);
        Scene scene = new Scene(vbox, 300, 250);
        popup.setScene(scene);
        popup.setTitle("Journal Entry");
        popup.show();
    }

    private String getRandomAffirmation() {
        return new String[]{"You are beautiful!", "You are fabulous", "Be who you are!"}[random.nextInt(3)];
    }

    private void saveAffirmation(String affirmation) {
        saveToFile(AFFIRMATIONS_FILE, "Affirmation: " + affirmation);
    }

    private void saveCheckIn(String mood) {
        saveToFile(CHECKINS_FILE, "Mood: " + mood);
    }

    private void saveJournal(String entry) {
        saveToFile(JOURNALS_FILE, "Journal Entry: " + entry);
    }

    private void saveToFile(String filename, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.write(content + " | Time: " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date()));
            writer.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void updateAffirmationTab() {
        updateTab(AFFIRMATIONS_FILE, affirmationVBox);
    }

    private void updateCheckInTab() {
        updateTab(CHECKINS_FILE, checkInVBox);
    }

    private void updateJournalTab() {
        updateTab(JOURNALS_FILE, journalVBox);
    }

    private void updateTab(String filename, VBox vbox) {
        Platform.runLater(() -> {
            vbox.getChildren().clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    vbox.getChildren().add(new Label(line));
                }
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    private void clearFile(String filename, VBox vbox) {
        try {
            new FileWriter(filename, false).close();
            vbox.getChildren().clear();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
