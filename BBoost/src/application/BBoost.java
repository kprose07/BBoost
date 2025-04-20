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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.StageStyle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BBoost extends Application {
    private final Random random = new Random();
    private static final String AFFIRMATIONS_FILE = "affirmations.txt";
    private static final String CHECKINS_FILE = "checkins.txt";
    private static final String JOURNALS_FILE = "journals.txt";
    private static final String PREFERENCES_FILE = "preferences.txt";
    private boolean popupsStarted = false;

    private VBox checkInVBox = new VBox(10);
    private VBox journalVBox = new VBox(10);
    private VBox affirmationVBox = new VBox(10);

    private ComboBox<String> frequencyComboBox = new ComboBox<>();
    private ComboBox<String> timeComboBox = new ComboBox<>();
    private ComboBox<String> colorComboBox = new ComboBox<>();
    private Tab preferencesTab;

    private final List<Runnable> popupSequence = new ArrayList<>();
    private int popupIndex = 0;

    private String currentFrequency = "Daily";
    @Override
    public void start(Stage primaryStage) {
        showSplash(primaryStage);
    }

    private void showSplash(Stage primaryStage) {
        Stage splashStage = new Stage(StageStyle.UNDECORATED);

        Image logo = new Image(getClass().getResourceAsStream("/images/icon.png"));
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(250);
        logoView.setPreserveRatio(true);

        Label title = new Label("Banana Boost");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #3b7a57;");

        VBox splashLayout = new VBox(20, logoView, title);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setPadding(new Insets(30));
        splashLayout.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 2;");

        Scene splashScene = new Scene(splashLayout, 400, 300);
        splashStage.setScene(splashScene);
        splashStage.show();

        Timeline splashDelay = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            splashStage.close();
            launchMainApp(primaryStage);
        }));
        splashDelay.play();
    }
    private void launchMainApp(Stage primaryStage) {
        // [Your original start() code from above goes here, except for the launch(args) line]
        // Move all the tabPane, scene setup, loading preferences, etc. into this method
        ensureFileExists("checkins.txt");
        ensureFileExists("journals.txt");
        ensureFileExists("affirmations.txt");    
    	Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            primaryStage.getIcons().add(icon);

            TabPane tabPane = new TabPane();
            Tab dashboardTab = new Tab("Dashboard");
            Tab notesTab = new Tab("Notes (Affirmations)");
            Tab checkInTab = new Tab("Check-Ins");
            Tab journalTab = new Tab("Journals");
            Tab reportsTab = new Tab("Reports");

            preferencesTab = new Tab("Preferences");

            Button clearAffirmationsButton = new Button("Clear Affirmations");
            Button clearCheckInsButton = new Button("Clear Check-Ins");
            Button clearJournalsButton = new Button("Clear Journals");

            clearAffirmationsButton.setOnAction(e -> clearFile(AFFIRMATIONS_FILE, affirmationVBox));
            clearCheckInsButton.setOnAction(e -> clearFile(CHECKINS_FILE, checkInVBox));
            clearJournalsButton.setOnAction(e -> clearFile(JOURNALS_FILE, journalVBox));

            VBox affirmationLayout = new VBox(10, clearAffirmationsButton, affirmationVBox);
            VBox checkInLayout = new VBox(10, clearCheckInsButton, checkInVBox);
            VBox journalLayout = new VBox(10, clearJournalsButton, journalVBox);

            notesTab.setContent(affirmationLayout);
            checkInTab.setContent(checkInLayout);
            journalTab.setContent(journalLayout);
            dashboardTab.setContent(new StackPane());
            reportsTab.setContent(new StackPane());

            setupPreferencesTab();
            loadPreferences();

            tabPane.getTabs().addAll(dashboardTab, notesTab, checkInTab, journalTab, reportsTab, preferencesTab);

            Scene scene = new Scene(tabPane, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/index.css").toExternalForm());

            primaryStage.setTitle("Bannan Boost");
            primaryStage.setScene(scene);
            primaryStage.show();

            updateCheckInTab();
            updateJournalTab();
            updateAffirmationTab();

            
        
        // Add this logic AFTER loading preferences:
        File prefFile = new File(PREFERENCES_FILE);
        boolean prefsExist = prefFile.exists() && prefFile.length() > 0;

        if (!prefsExist) {
            // Focus on Preferences tab only
            tabPane.getSelectionModel().select(preferencesTab);
        } else {
            // Start popups only if preferences already exist
            startFrequencyPopupCycle();
        }

        // Final scene setup and show
        primaryStage.setTitle("Banana Boost");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            event.consume();  // prevent app from closing
            primaryStage.hide();  // just hide the window
        });

    }

    private void ensureFileExists(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Could not create file: " + fileName);
                e.printStackTrace();
            }
        }
    }

    private void setupPreferencesTab() {
        frequencyComboBox.getItems().addAll("Daily", "Every Other Day", "Weekly");
        frequencyComboBox.setValue("Daily");

        timeComboBox.getItems().addAll("Morning", "Noon", "Evening");
        timeComboBox.setValue("Morning");

        colorComboBox.getItems().addAll("Red", "Green", "Blue", "Purple", "Orange");
        colorComboBox.setValue("Red");

        Button saveButton = new Button("Save Preferences");
        saveButton.setOnAction(e -> {
            savePreferences();

            // Avoid double start of popups
            if (!popupsStarted) {
                popupsStarted = true;
                startFrequencyPopupCycle();
            }

            // Optionally switch to another tab after saving
            preferencesTab.getTabPane().getSelectionModel().selectFirst(); // Switch to Dashboard or first tab
        });

        VBox preferencesVBox = new VBox(10);
        preferencesVBox.setPadding(new Insets(15));
        preferencesVBox.setAlignment(Pos.TOP_LEFT);
        preferencesVBox.getChildren().addAll(
                new Label("Set Frequency:"),
                frequencyComboBox,
                new Label("Set Time:"),
                timeComboBox,
                new Label("Set Color Theme:"),
                colorComboBox,
                saveButton
        );
        Button powerOffButton = new Button("Power Off App");
        powerOffButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        powerOffButton.setOnAction(e -> {
            Platform.exit();  // this will stop all background tasks and close the app
            System.exit(0);   // ensure JVM shuts down
        });

        preferencesVBox.getChildren().add(powerOffButton);

        preferencesTab.setContent(preferencesVBox);
    }

    private void savePreferences() {
        String frequency = frequencyComboBox.getValue();
        String time = timeComboBox.getValue();
        String color = colorComboBox.getValue();

        currentFrequency = frequency;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PREFERENCES_FILE))) {
            writer.write("Frequency=" + frequency);
            writer.newLine();
            writer.write("Time=" + time);
            writer.newLine();
            writer.write("Color=" + color);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPreferences() {
        File file = new File(PREFERENCES_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Frequency=")) {
                    String value = line.split("=")[1];
                    frequencyComboBox.setValue(value);
                    currentFrequency = value;
                } else if (line.startsWith("Time=")) {
                    timeComboBox.setValue(line.split("=")[1]);
                } else if (line.startsWith("Color=")) {
                    colorComboBox.setValue(line.split("=")[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startFrequencyPopupCycle() {
        double delayMillis;

        // Set delay based on the frequency selected
        switch (currentFrequency) {
            case "Weekly":
                delayMillis = 7 * 24 * 60 * 60 * 1000; // 7 days
                break;
            case "Every Other Day":
                delayMillis = 2 * 24 * 60 * 60 * 1000; // 2 days
                break;
            default: // Daily
                delayMillis = 24 * 60 * 60 * 1000; // 1 day
                break;
        }

        // Trigger the first set of popups immediately
        Platform.runLater(this::showPopupsInSequence);

        // Schedule the cycle to repeat based on the frequency
        Timeline repeatCycle = new Timeline(
            new KeyFrame(Duration.millis(delayMillis), event -> showPopupsInSequence())
        );
        repeatCycle.setCycleCount(Timeline.INDEFINITE);
        repeatCycle.play();
    }

    private void showPopupsInSequence() {
        // Show the first popup in the sequence
        showAffirmationPopup(() -> showCheckInPopup(() -> showJournalPopup(() -> {})));
    }

    private void showAffirmationPopup(Runnable onClose) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);

        String affirmation = getRandomAffirmation();
        Label label = new Label(affirmation);
        label.setWrapText(true);
        label.getStyleClass().add("affirmation-label");

        Image heartImage = new Image(getClass().getResourceAsStream("/images/Heart.png"));
        ImageView heartIcon = new ImageView(heartImage);
        heartIcon.setFitWidth(24); heartIcon.setFitHeight(24);
        heartIcon.setOnMouseClicked(e -> {
            saveAffirmation(affirmation);
            updateAffirmationTab();
            popup.close();
            onClose.run();
        });

        Image closeImage = new Image(getClass().getResourceAsStream("/images/Close.png"));
        ImageView closeIcon = new ImageView(closeImage);
        closeIcon.setFitWidth(24); closeIcon.setFitHeight(24);
        closeIcon.setOnMouseClicked(e -> {
            popup.close();
            onClose.run();
        });

        HBox icons = new HBox(10, heartIcon, new Region(), new Region(), closeIcon);
        HBox.setHgrow(icons.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(icons.getChildren().get(2), Priority.ALWAYS);

        VBox content = new VBox(10, icons, new Region(), label);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("pop-up");

        final Delta drag = new Delta();
        content.setOnMousePressed(e -> { drag.x = e.getSceneX(); drag.y = e.getSceneY(); });
        content.setOnMouseDragged(e -> { popup.setX(e.getScreenX() - drag.x); popup.setY(e.getScreenY() - drag.y); });

        Scene scene = new Scene(content, 180, 180);
        scene.getStylesheets().add(getClass().getResource("/styles/index.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
    }

    private void showCheckInPopup(Runnable onClose) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(15));
        vbox.setAlignment(Pos.TOP_CENTER);
        vbox.getStyleClass().add("checkin-popup");

        // Title
        Label titleLabel = new Label("Daily Check-In");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Date
        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");

        // Mood buttons as images
        HBox emojiRow = new HBox(10);
        emojiRow.setAlignment(Pos.CENTER);

        String[] moods = {"Excited", "Happy", "Neutral", "Sad", "Tired"};
        String[] imageFiles = {"/images/excited.png", "/images/happy.png", "/images/neutral.png", "/images/sad.png", "/images/tired.png"};
        String[] borderColors = {"#FFD700", "#32CD32", "#A9A9A9", "#1E90FF", "#FF4500"};

        for (int i = 0; i < moods.length; i++) {
            String mood = moods[i];
            String color = borderColors[i];

            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream(imageFiles[i])));
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);

            StackPane imageButton = new StackPane(imageView);
            imageButton.setPadding(new Insets(5));
            imageButton.setStyle("-fx-background-radius: 50%; -fx-cursor: hand;");
            int index = i;

            imageButton.setOnMouseClicked(e -> {
                saveCheckIn(mood);
                updateCheckInTab();
                popup.close();
                onClose.run();
            });

            emojiRow.getChildren().add(imageButton);
        }

        // Close Button
        Button closeButton = new Button("✕");
        closeButton.setStyle("-fx-background-color: transparent; -fx-font-size: 18px;");
        closeButton.setOnAction(e -> {
            popup.close();
            onClose.run();
        });

        StackPane closeWrapper = new StackPane(closeButton);
        closeWrapper.setAlignment(Pos.TOP_RIGHT);

        vbox.getChildren().addAll(closeWrapper, titleLabel, dateLabel, emojiRow);

        // Add draggable functionality
        final Delta drag = new Delta();
        vbox.setOnMousePressed(e -> {
            drag.x = e.getSceneX();
            drag.y = e.getSceneY();
        });
        vbox.setOnMouseDragged(e -> {
            popup.setX(e.getScreenX() - drag.x);
            popup.setY(e.getScreenY() - drag.y);
        });

        Scene scene = new Scene(vbox, 360, 200);
        scene.getStylesheets().add(getClass().getResource("/styles/index.css").toExternalForm());

        popup.setScene(scene);
        popup.show();
    }

    private static class Delta {
        double x, y;
    }

    private void showJournalPopup(Runnable onClose) {
        Stage popup = new Stage();
        VBox vbox = new VBox(10);
        Label label = new Label("How are you feeling today?");
        TextArea textArea = new TextArea();
        Button saveButton = new Button("Save Journal");

        saveButton.setOnAction(e -> {
            String journalText = textArea.getText().trim();
            if (!journalText.isEmpty()) {
                saveJournal(journalText);
                updateJournalTab();
            }

            popup.close(); // Close the popup after saving
            onClose.run(); // Continue to next popup or complete sequence
        });


        vbox.getChildren().addAll(label, textArea, saveButton);
        Scene scene = new Scene(vbox, 300, 250);
        popup.setScene(scene);
        popup.setTitle("Journal Entry");
        popup.show();
    }


    private String getRandomAffirmation() {
        String[] affirmations = {
            "You are beautiful!", "You are fabulous!", "Be who you are!",
            "You are doing better than you think. Celebrate every small win.",
            "Peace may be quiet, but it is powerful. You deserve calm.",
            "You are not alone. Connection is healing, even in small doses."
        };
        return affirmations[random.nextInt(affirmations.length)];
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
                    Label entryLabel = new Label(line);
                    entryLabel.getStyleClass().add("saved-checkin-label");
                    vbox.getChildren().add(entryLabel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
