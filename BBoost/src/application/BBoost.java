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
import javafx.scene.Cursor;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javafx.geometry.Insets;

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
    private VBox dashboardLayout = new VBox(20);
    private ComboBox<String> frequencyComboBox = new ComboBox<>();
    private ComboBox<String> timeComboBox = new ComboBox<>();
    private ComboBox<String> colorComboBox = new ComboBox<>();
    private Tab preferencesTab;

    private String moodSelected = null;
    private VBox journalTemplatesVBox = new VBox(10);
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Map<String, List<String>> journalTemplates = new HashMap<>();


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
            setupDashboardTab(dashboardTab);
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
            //VBox journalLayout = new VBox(10, clearJournalsButton,journalVBox, new Label("Journal Templates:"), journalTemplatesVBox);
            ScrollPane journalScrollPane = new ScrollPane(journalVBox);
            journalScrollPane.setFitToWidth(true);
            journalScrollPane.setPadding(new Insets(10));

            VBox journalLayout = new VBox(10, clearJournalsButton, journalScrollPane, new Label("Journal Templates:"), journalTemplatesVBox);
            journalLayout.setPadding(new Insets(10));

            journalTab.setContent(journalLayout);

            notesTab.setContent(affirmationLayout);
            checkInTab.setContent(checkInLayout);
            journalTab.setContent(journalLayout);
            dashboardTab.setContent(dashboardLayout);
            reportsTab.setContent(new StackPane());

            setupPreferencesTab();
            loadPreferences();

            tabPane.getTabs().addAll(dashboardTab, notesTab, checkInTab, journalTab, reportsTab, preferencesTab);

            Scene scene = new Scene(tabPane, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/index.css").toExternalForm());

            primaryStage.setTitle("Banana Boost");
            primaryStage.setScene(scene);
            primaryStage.show();

            updateCheckInTab();
            updateJournalTab();
            updateAffirmationTab();
            initializeJournalTemplates();
            loadJournalTemplates();


            
        
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
    private void setupDashboardTab(Tab dashboardTab) {
        dashboardLayout.setPadding(new Insets(20));
        dashboardLayout.setAlignment(Pos.TOP_CENTER);
        
        dashboardTab.setContent(dashboardLayout);


        Label titleLabel = new Label("Welcome to Banana Boost!");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label affirmationCountLabel = new Label();
        Label checkInCountLabel = new Label();
        Label journalCountLabel = new Label();
        Label lastMoodLabel = new Label();
        Label lastJournalLabel = new Label();

        Button refreshButton = new Button("Refresh Dashboard");
        refreshButton.setOnAction(e -> {
            affirmationCountLabel.setText("Total Affirmations: " + countLinesInFile(AFFIRMATIONS_FILE));
            checkInCountLabel.setText("Total Check-Ins: " + countLinesInFile(CHECKINS_FILE));
            journalCountLabel.setText("Total Journals: " + countJournalEntries(JOURNALS_FILE));

            lastMoodLabel.setText("Latest Mood: " + getLastLineFromFile(CHECKINS_FILE));
            //lastJournalLabel.setText("Latest Journal: " + getLastJournalTitle(JOURNALS_FILE));
        });

        refreshButton.fire();

        dashboardLayout.getChildren().addAll(
            titleLabel, 
            affirmationCountLabel, 
            checkInCountLabel, 
            journalCountLabel,
            lastMoodLabel,
            lastJournalLabel,
            refreshButton
        );

    }

    private int countLinesInFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            return (int) reader.lines().count();
        } catch (IOException e) {
            return 0;
        }
    }
    private int countJournalEntries(String filename) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("----- Journal Entry")) {
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private String getLastLineFromFile(String fileName) {
        String lastLine = "N/A";
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
        } catch (IOException e) {
            // handle error
        }
        return lastLine;
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

      
        // Schedule the cycle to repeat based on the frequency
        Timeline repeatCycle = new Timeline(
            new KeyFrame(Duration.millis(delayMillis), event -> showAffirmationPopup())
        );
        repeatCycle.setCycleCount(Timeline.INDEFINITE);
        repeatCycle.play();
    }
    

    private void showAffirmationPopup() {
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
           
        });

        Image closeImage = new Image(getClass().getResourceAsStream("/images/Close.png"));
        ImageView closeIcon = new ImageView(closeImage);
        closeIcon.setFitWidth(24); closeIcon.setFitHeight(24);
        closeIcon.setOnMouseClicked(e -> {
            popup.close();
      
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
    private void showCheckInPopup() {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);

        VBox content = createCheckInContent(() -> {
            saveCheckIn(moodSelected);
            updateCheckInTab();
            popup.close();

        });

        // Close button in the top right
        Button closeButton = new Button("✕");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 16px;");
        closeButton.setOnAction(e -> {
            popup.close();
           
        });

        HBox topBar = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(spacer, closeButton);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(5, 10, 0, 0));

        VBox wrapper = new VBox(topBar, content);
        wrapper.getStyleClass().add("checkin-popup");
 
        // Drag support
        final Delta drag = new Delta();
        wrapper.setOnMousePressed(e -> {
            drag.x = e.getSceneX();
            drag.y = e.getSceneY();
        });
        wrapper.setOnMouseDragged(e -> {
            popup.setX(e.getScreenX() - drag.x);
            popup.setY(e.getScreenY() - drag.y);
        });

        Scene scene = new Scene(wrapper, 360, 220);
        scene.getStylesheets().add(getClass().getResource("/styles/index.css").toExternalForm());
        popup.setScene(scene);
        popup.show();
    }
    private VBox createCheckInContent(Runnable onClose) {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.TOP_CENTER);
       vbox.getStyleClass().add("checkin-popupW");
        
        Label titleLabel = new Label("Daily Check-In");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #777;");

        HBox emojiRow = new HBox(12);
        emojiRow.setAlignment(Pos.CENTER);
        emojiRow.setPadding(new Insets(10));

        String[] moods = {"Excited", "Happy", "Neutral", "Sad", "Tired"};
        String[] imageFiles = {"/images/excited.png", "/images/happy.png", "/images/neutral.png", "/images/sad.png", "/images/tired.png"};
        String[] borderColors = {"#FFD700", "#32CD32", "#A9A9A9", "#1E90FF", "#FF4500"};

        for (int i = 0; i < moods.length; i++) {
            String mood = moods[i];
            String borderColor = borderColors[i];

            ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream(imageFiles[i])));
            imageView.setFitWidth(45);
            imageView.setFitHeight(45);

            StackPane moodBox = new StackPane(imageView);
            moodBox.setPadding(new Insets(6));
            moodBox.setCursor(Cursor.HAND);
            moodBox.setStyle("-fx-background-radius: 10px;" +
                (mood.equals(moodSelected)
                    ? "-fx-border-color: " + borderColor + "; -fx-border-width: 3; -fx-border-radius: 12;"
                    : "-fx-border-color: transparent;"));

            moodBox.setOnMouseClicked(e -> {
                moodSelected = mood;
                if (onClose != null) onClose.run();
            });

            emojiRow.getChildren().add(moodBox);
        }

        vbox.getChildren().addAll(titleLabel, dateLabel, emojiRow);
        return vbox;
    }

 
    private static class Delta {
        double x, y;
    }
    private void initializeJournalTemplates() {
        journalTemplates.put("Instant Cheer Up", Arrays.asList(
                "What are you grateful for?",
                "What did you enjoy today?",
                "What are you planning for the future?",
                "What do people like about you?",
                "Write down a compliment you’d give to yourself.",
                "What’s one thing that always brings you joy, no matter how small?",
                "Write about a time when you laughed so hard it hurt. What made it so funny?",
                "What compliments do you like to receive from others?"
        ));

        journalTemplates.put("Gratitude Entry", Arrays.asList(
                "List three things that you are grateful for:",
                "When was the last time you gave to someone that in turn made you feel grateful?"
        ));

        journalTemplates.put("Morning Reflection", Arrays.asList(
                "How do you feel?",
                "Why do you feel this way?",
                "What will you do today?",
                "What are you looking forward to?",
                "What is one easy task that you'd like to accomplish today?",
                "Take 10 minutes to meditate!",
                "Write about the dream you had last night!"
        ));

        journalTemplates.put("Letting Go of Worries", Arrays.asList(
                "What worries you?",
                "How would an outsider see it?",
                "What can be the positive outcome?",
                "What are some things you can let go of that aren't helping you?"
        ));
    }

    private void loadJournalTemplates() {
        journalTemplatesVBox.getChildren().clear();
        for (Map.Entry<String, List<String>> entry : journalTemplates.entrySet()) {
            String templateName = entry.getKey();
            List<String> questions = entry.getValue();

            Button templateButton = new Button(templateName);
            templateButton.setOnAction(e -> showJournalTemplatePopup(templateName, questions));
            journalTemplatesVBox.getChildren().add(templateButton);
        }
    }

    private void showJournalTemplatePopup(String title, List<String> questions) {
        Stage popup = new Stage();
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        vbox.getChildren().add(titleLabel);

        List<TextArea> textAreas = new ArrayList<>();

        for (String question : questions) {
            Label questionLabel = new Label(question);
            TextArea answerArea = new TextArea();
            answerArea.setWrapText(true);
            vbox.getChildren().addAll(questionLabel, answerArea);
            textAreas.add(answerArea);
        }

        Button saveButton = new Button("Save Journal");
        saveButton.setOnAction(e -> {
            StringBuilder entry = new StringBuilder(title + ":\n");
            for (int i = 0; i < questions.size(); i++) {
                entry.append(questions.get(i)).append("\n").append(textAreas.get(i).getText()).append("\n\n");
            }
            saveJournal(entry.toString().trim());
            updateJournalTab();
            popup.close();
        });

        vbox.getChildren().add(saveButton);
        Scene scene = new Scene(new ScrollPane(vbox), 400, 500);
        popup.setScene(scene);
        popup.setTitle(title);
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
        String timestamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a").format(new Date());
        String[] lines = entry.split("\n", 2);
        String title = lines.length > 0 ? lines[0] : "Untitled";
        String content = lines.length > 1 ? lines[1] : "";

        String formatted = "----- Journal Entry -----\nTitle: " + title +
                "\nTime: " + timestamp + "\n" + content.trim() + "\n---------------------------\n";

        saveToFile(JOURNALS_FILE, formatted);
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
    // Load check-in tab with same style
    private void updateCheckInTab() {
        Platform.runLater(() -> {
            checkInVBox.getChildren().clear();

            Button checkInButton = new Button("Check In");
            checkInButton.getStyleClass().add("button");
            checkInButton.setOnAction(e -> showCheckInPopup());

            checkInVBox.getChildren().add(checkInButton);

            try (BufferedReader reader = new BufferedReader(new FileReader(CHECKINS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Label entryLabel = new Label(line);
                    entryLabel.getStyleClass().add("saved-checkin-label");
                    checkInVBox.getChildren().add(entryLabel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateJournalTab() {
        Platform.runLater(() -> {
            journalVBox.getChildren().clear();

            try (BufferedReader reader = new BufferedReader(new FileReader(JOURNALS_FILE))) {
                String line;
                StringBuilder entryBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("----- Journal Entry")) {
                        if (entryBuilder.length() > 0) {
                            createJournalEntryButton(entryBuilder.toString().trim());
                            entryBuilder.setLength(0);
                        }
                    }
                    entryBuilder.append(line).append("\n");
                }
                if (entryBuilder.length() > 0) {
                    createJournalEntryButton(entryBuilder.toString().trim());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    private void createJournalEntryButton(String entryText) {
        String title = "Journal Entry";
        String time = "";

        for (String line : entryText.split("\n")) {
            if (line.startsWith("Title:")) {
                title = line.substring(6).trim();
            } else if (line.startsWith("Time:")) {
                time = line.substring(5).trim();
            }
        }

        Button entryButton = new Button(title + " (" + time + ")");
        entryButton.setMaxWidth(Double.MAX_VALUE);
        entryButton.setWrapText(true);
        entryButton.getStyleClass().add("jbtn");
        entryButton.setOnAction(e -> showJournalPopup(entryText));

        journalVBox.getChildren().add(entryButton);
    }
    private void showJournalPopup(String entryText) {
        Stage popup = new Stage();
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        Label label = new Label(entryText);
        label.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane(label);
        scrollPane.setFitToWidth(true);

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> popup.close());

        vbox.getChildren().addAll(scrollPane, closeButton);

        Scene scene = new Scene(vbox, 400, 500);
        popup.setTitle("Journal Entry");
        popup.setScene(scene);
        popup.show();
    }


    private void updateTab(String filename, VBox vbox) {
        Platform.runLater(() -> {
            vbox.getChildren().clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                StringBuilder entryBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("----- Journal Entry")) {
                        // When we hit a new entry, flush the previous one
                        if (entryBuilder.length() > 0) {
                            Label entryLabel = new Label(entryBuilder.toString().trim());
                            entryLabel.getStyleClass().add("saved-checkin-label");
                            entryLabel.setWrapText(true);
                            vbox.getChildren().add(entryLabel);
                            entryBuilder.setLength(0); // Reset for new entry
                        }
                    }
                    entryBuilder.append(line).append("\n");
                }

                // Add the last journal entry if any
                if (entryBuilder.length() > 0) {
                    Label entryLabel = new Label(entryBuilder.toString().trim());
                    entryLabel.getStyleClass().add("saved-checkin-label");
                    entryLabel.setWrapText(true);
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
