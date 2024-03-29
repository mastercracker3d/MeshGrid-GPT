package cl.mc3d.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class FXChat extends Application {

    private PubSubFiles pubSubFiles = null;
    private TabPane tabPane = null;
    private HashMap<String, WebView> hWebView = null;
    private HashMap<String, String> hPendingConversations = null;
    private Properties properties = new Properties();
    private TextField modelRepositoryTextField;
    private TextField modelTextField;
    private TextField modelPromptTemplateTextField;
    private TextField cacheTextField;
    private ComboBox<String> cacheLocalIpComboBox;
    private CheckBox cipherMessageCheckBox;
    private PasswordField keyStorePassTextField;
    private Button testKeystore;
    private CheckBox chatUICheckBox;
    private Stage primaryStage;
    private ToggleButton igniteControl;
    private TextArea logTextArea;
    private final ObservableList<StatusDataRow> statusData = FXCollections.observableArrayList();
    private final ObservableList<ModelDataRow> modelsData = FXCollections.observableArrayList();
    private TableView<ModelDataRow> modelsTableView;
    private String locationStart;
    private ModelsInfo webOperations;
    private List<ProgressItem> progressItems = new ArrayList<>();
    private VBox vBoxLLMDownloads = new VBox(10);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MeshGrid GPT");
        Image icon = new Image("resources/meshgrid_gpt-icon.png");
        primaryStage.getIcons().add(icon);
        locationStart = System.getProperty("user.dir");
        webOperations = new ModelsInfo(modelsData);
        webOperations.start();
        System.out.println("El programa se inició desde: " + locationStart + ", hashCode:" + locationStart.hashCode());
        this.primaryStage = primaryStage;
        logTextArea = new TextArea();
        igniteControl = new ToggleButton("Ignite the cluster");
        igniteControl.setPrefWidth(165);

        TimerService timerService = new TimerService(locationStart, igniteControl, logTextArea, statusData);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        tabPane.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                if (((Node) event.getPickResult().getIntersectedNode()) instanceof Text) {
                    String text = ((Text) ((Node) event.getPickResult().getIntersectedNode())).getText();
                    for (Tab tab : tabPane.getTabs()) {
                        if (((!text.equals("Principal")) && (!text.equals("Status")) && (!text.equals("Setup"))) && (tab.getText().equals(text))) {
                            TextInputDialog dialog = new TextInputDialog(text);
                            dialog.setTitle("Title change");
                            dialog.setHeaderText(null);
                            dialog.setContentText("Change the title:");
                            Optional<String> result = dialog.showAndWait();
                            String nuevoTitulo = result.orElse(null);
                            if (nuevoTitulo != null && !nuevoTitulo.isEmpty()) {
                                boolean repited = false;
                                for (Tab tabExists : tabPane.getTabs()) {
                                    if (tabExists.getText().toLowerCase().equals(nuevoTitulo.toLowerCase())) {
                                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Title repeated");
                                        alert.setHeaderText(null);
                                        alert.setContentText("The new Title exists.");
                                        alert.showAndWait();
                                        repited = true;
                                        break;
                                    }
                                }
                                if (!repited) {
                                    tab.setText(nuevoTitulo);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });

        hWebView = new HashMap();
        createTabPane("Setup", tabPane);
        createTabPane("Status", tabPane);
        createTabPane("Principal", tabPane);
        loadProperties("mc3d-ai.properties");
        hPendingConversations = new HashMap();
        BorderPane principalPane = new BorderPane();
        FlowPane flowPane = new FlowPane(Orientation.VERTICAL);
        flowPane.setVgap(10);
        Button addButton = new Button("New Chat");
        addButton.setPrefWidth(165);
        addButton.setOnAction(e -> createTabPane("Chat: " + tabPane.getTabs().size(), tabPane));
        flowPane.getChildren().add(addButton);
        BackgroundFill backgroundFill = new BackgroundFill(Color.RED, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
        Background background = new Background(backgroundFill);
        igniteControl.setBackground(background);
        igniteControl.setOnAction(new EventHandler() {
            @Override
            public void handle(Event t) {
                if (igniteControl.isSelected()) {
                    timerService.stop();
                    timerService.config();
                    timerService.start();

                    BackgroundFill backgroundFill = new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                    Background background = new Background(backgroundFill);
                    igniteControl.setBackground(background);
                    System.out.println("Cluster on");
                } else {
                    System.out.println("Cluster off");
                    igniteControl.setText("Ignite the cluster");
                    timerService.stop();
                    BackgroundFill backgroundFill = new BackgroundFill(Color.RED, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                    Background background = new Background(backgroundFill);
                    igniteControl.setBackground(background);

                }
            }
        });
        flowPane.getChildren().add(igniteControl);
        Image mc3dIcon = new Image("resources/mc3d_logo.png");
        ImageView ivMc3d = new ImageView(mc3dIcon);
        Button mc3dLogo = new Button("", ivMc3d);
        mc3dLogo.setOnAction(e
                -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MC3D Support");
            alert.setHeaderText("Contact to Us");
            Image mc3dQR = new Image("resources/mc3d-qr.png");
            ImageView ivMc3dQR = new ImageView(mc3dQR);
            alert.setGraphic(ivMc3dQR);
            alert.setContentText("MC3D Support can be obtained using GPT4ALL Community forum and commercial support using http://www.mc3d.cl");
            alert.showAndWait();
        });

        flowPane.getChildren().add(mc3dLogo);
        principalPane.setLeft(flowPane);;

        principalPane.setCenter(tabPane);
        VBox root = new VBox(principalPane);
        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(10);
        Scene scene = new Scene(root, 800, 600);
        //scene.getStylesheets().add("resources/primer-light.css");

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(this::handleClose);

        tabPane.getSelectionModel().select(2);
        if (chatUICheckBox.isSelected()) {
            primaryStage.show();
        }
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {

                if (chatUICheckBox.isSelected()) {
                    try {
                        String sLocation = "";
                        List<String> lIdResponses = pubSubFiles.getResponseId(sLocation);
                        for (String id : lIdResponses) {
                            if (id.contains("WV")) {
                                LinkedHashMap<String, Object> message = pubSubFiles.getResponse(sLocation, id);
                                String modelName = id;
                                if (modelName.contains("-(")) {
                                    modelName = modelName.substring(modelName.indexOf("-") + 1, modelName.lastIndexOf("-("));
                                    modelName = modelName.substring(0, modelName.lastIndexOf("-"));
                                }
                                String externalId = id;
                                if (externalId.contains("-(")) {
                                    externalId = externalId.substring(externalId.indexOf("-(") + 2, externalId.length());
                                    if (externalId.contains(".")) {
                                        externalId = externalId.substring(0, externalId.indexOf("."));
                                    }
                                    if (externalId.contains("-")) {
                                        externalId = externalId.substring(0, externalId.indexOf("-"));
                                    }
                                }
                                String sResponse = "" + message.get("data");
                                if (sResponse.length() > 3) {
                                    addJeMensajes("" + new Date(), externalId, modelName, sResponse);
                                    pubSubFiles.deleteResponse(sLocation, id);
                                }
                            }

                        }
                    } catch (Exception e) {

                    }
                }

            });

        }, 0, 50, TimeUnit.MILLISECONDS);

    }
    // Método para filtrar la TableView

    private void filterTable(String filtro) {
        if (filtro == null || filtro.isEmpty()) {
            modelsTableView.setItems(modelsData);
        } else {
            ObservableList<ModelDataRow> modelsDataFilter = FXCollections.observableArrayList();
            for (ModelDataRow model : modelsData) {
                if (model.getModel().toLowerCase().contains(filtro.toLowerCase())) {
                    modelsDataFilter.add(model);
                }
            }
            modelsTableView.setItems(modelsDataFilter);
        }
    }

    private TabPane createTabPane(String title, TabPane tabPane) {

        Tab tab = new Tab(title);

        if (title.equals("Status")) {
            tab.setId("Status");
            tab.setClosable(false);

            BorderPane borderPane = new BorderPane();
            borderPane.setPadding(new Insets(10));
            TableView<StatusDataRow> statusTableView = new TableView<>();
            statusTableView.setEditable(true);
            statusTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            statusTableView.setItems(statusData); // Configurar la tabla para usar la lista observable
            TableColumn<StatusDataRow, String> nameColumn = new TableColumn<>("Name");
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<StatusDataRow, String> ipColumn = new TableColumn<>("IP");
            ipColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));

            TableColumn<StatusDataRow, String> llmColumn = new TableColumn<>("LLM");
            llmColumn.setCellValueFactory(new PropertyValueFactory<>("llm"));
            TableColumn<StatusDataRow, String> lastAccessColumn = new TableColumn<>("Last Access");
            lastAccessColumn.setCellValueFactory(new PropertyValueFactory<>("lastAccess"));
            statusTableView.getColumns().addAll(nameColumn, ipColumn, llmColumn, lastAccessColumn);
            borderPane.setTop(statusTableView);
            logTextArea.setEditable(false);
            Button clearButton = new Button("Clear logs");
            clearButton.setOnAction(e -> logTextArea.clear());
            borderPane.setCenter(logTextArea);
            borderPane.setBottom(clearButton);
            tab.setContent(borderPane);

        } else {
            if (title.equals("Setup")) {
                tab.setId("Setup");
                tab.setClosable(false);
                BorderPane borderPane = new BorderPane();
                borderPane.setPadding(new Insets(10));
                vBoxLLMDownloads.setPadding(new Insets(10));
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setContent(vBoxLLMDownloads);
                scrollPane.setFitToWidth(true); // Ajustar al ancho del ScrollPane

                // Crear un título para el ScrollPane
                Label titleLLMDownloadsLabel = new Label("LLM Downloads");
                titleLLMDownloadsLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

                // Crear un VBox para contener el título y el ScrollPane
                VBox llmDownloadsPane = new VBox();
                llmDownloadsPane.getChildren().addAll(titleLLMDownloadsLabel, scrollPane);
                borderPane.setRight(llmDownloadsPane);;
                modelsTableView = new TableView<>();
                modelsTableView.setEditable(true);
                modelsTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                modelsTableView.setItems(modelsData);
                TableColumn<ModelDataRow, String> rankingColumn = new TableColumn<>("Ranking");
                rankingColumn.setCellValueFactory(new PropertyValueFactory<>("ranking"));
                rankingColumn.setSortable(false);
                TableColumn<ModelDataRow, String> nameColumn = new TableColumn<>("Model");
                nameColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
                nameColumn.setSortable(false);
                TextField filtroTextField = new TextField();
                filtroTextField.setPromptText("filter by Model");

                // Aplicar el filtro cuando el texto cambie
                filtroTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filterTable(newValue);
                });
                TableColumn<ModelDataRow, String> lastUpdateColumn = new TableColumn<>("Last update");
                lastUpdateColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdate"));
                lastUpdateColumn.setSortable(false);
                modelsTableView.getColumns().addAll(rankingColumn, nameColumn, lastUpdateColumn);
                modelsTableView.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) { // Verificar si fue un doble clic
                        ModelDataRow modelDataRowSelected = modelsTableView.getSelectionModel().getSelectedItem();
                        if (modelDataRowSelected != null) {
                            System.out.println("Doble clic en " + modelDataRowSelected.getModel());
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Model download from: huggingface.co");
                            alert.setHeaderText("Themodel: " + modelDataRowSelected.getModel() + "\nHas download Ranking: " + modelDataRowSelected.getRanking());
                            GridPane gridModelType = new GridPane();
                            gridModelType.setPadding(new Insets(20, 20, 20, 20));
                            gridModelType.setVgap(10);
                            gridModelType.setHgap(10);
                            Label modelTypeLabel = new Label("Select the model type:");
                            gridModelType.add(modelTypeLabel, 0, 0);
                            List<LLM> lListModelType = webOperations.listModelType(modelDataRowSelected.getModel());

                            ComboBox modelTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(lListModelType));
                            gridModelType.add(modelTypeComboBox, 1, 0);
                            alert.getDialogPane().setContent(gridModelType);
                            ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
                            if (result == ButtonType.OK) {
                                DownloadModels downloadModels = new DownloadModels(modelRepositoryTextField.getText(), modelDataRowSelected.getModel(), modelTypeComboBox.getValue().toString());
                                int iListModelTypeSize = lListModelType.size();
                                String sSize = "";
                                System.out.println("Model to download: " + modelTypeComboBox.getValue().toString());
                                for (int i = 1; i < iListModelTypeSize; i++) {
                                    if (lListModelType.get(i).getName().equals(modelTypeComboBox.getValue().toString())) {
                                        sSize = lListModelType.get(i).getSize();
                                        break;
                                    }

                                }
                                ProgressItem ProgressItem = new ProgressItem(modelRepositoryTextField.getText(), modelDataRowSelected.getModel(), modelTypeComboBox.getValue().toString(), sSize);
                                progressItems.add(ProgressItem);
                                vBoxLLMDownloads.getChildren().add(ProgressItem);
                                downloadModels.start();
                            }

                        }
                    }
                });

                borderPane.setTop(filtroTextField);
                borderPane.setCenter(modelsTableView);
                GridPane grid = new GridPane();
                TitledPane titledPane = new TitledPane("MC3D AI Configuration", grid);

                grid.setPadding(new Insets(20, 20, 20, 20));
                grid.setVgap(10);
                grid.setHgap(10);

                // Labels
                Label modelRepositoryLabel = new Label("Model repository:");
                Label modelLabel = new Label("Model:");
                Label modelPromptTemplateLabel = new Label("Model Prompt Template:");
                Label cacheLabel = new Label("Cache:");
                Label cacheLocalIpLabel = new Label("Cache Local IP:");
                Label cipherMessageLabel = new Label("Cipher Message:");
                Label keyStorePassLabel = new Label("KeyStore Password:");
                Label chatUILabel = new Label("Chat UI:");
                modelRepositoryTextField = new TextField();

                modelTextField = new TextField();
                modelPromptTemplateTextField = new TextField();
                cacheTextField = new TextField();

                cacheLocalIpComboBox = new ComboBox<>(FXCollections.observableArrayList(listIPAddresses()));
                cipherMessageCheckBox = new CheckBox();
                keyStorePassTextField = new PasswordField();
                testKeystore = new Button("Test Keystore");
                testKeystore.setOnAction(e -> {
                    if ((cipherMessageCheckBox.isSelected()) && (!keyStorePassTextField.getText().isEmpty())) {
                        File f = new File(cacheTextField.getText() + ".pfx");
                        if (!f.exists()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("The KeyStore not exists");
                            alert.setHeaderText(null);
                            alert.setContentText("The KeyStore: " + cacheTextField.getText() + ".pfx not exists");
                            alert.showAndWait();
                        } else {
                            try {
                                Security.getPublicKeyFromKeystore(cacheTextField.getText() + ".pfx", keyStorePassTextField.getText(), cacheTextField.getText(), "PKCS12");
                                BackgroundFill backgroundFill = new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                                Background background = new Background(backgroundFill);
                                testKeystore.setBackground(background);
                            } catch (Exception ex) {
                                BackgroundFill backgroundFill = new BackgroundFill(Color.RED, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                                Background background = new Background(backgroundFill);
                                testKeystore.setBackground(background);
                                cipherMessageCheckBox.setSelected(false);
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("KeyStore access denied");
                                alert.setHeaderText(null);
                                alert.setContentText("The password for KeyStore: " + cacheTextField.getText() + ".pfx is invalid");
                                alert.showAndWait();
                            }
                        }
                    }
                });

                DirectoryChooser directoryChooser = new DirectoryChooser();

                Button selectFolderModelLibraryButton = new Button("Select Model repository Folder");
                selectFolderModelLibraryButton.setOnAction(e -> {
                    File selectedFolder = directoryChooser.showDialog(primaryStage);
                    if (selectedFolder != null) {
                        modelRepositoryTextField.setText(selectedFolder.getAbsolutePath());

                    }
                });
                chatUICheckBox = new CheckBox();

                FileChooser modelFileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("GGUF Files (*.gguf)", "*.gguf");
                modelFileChooser.getExtensionFilters().add(extFilter);
                Button selectModelButton = new Button("Select Model File");
                selectModelButton.setOnAction(e -> {

                    if (modelTextField.getText().length() > 0) {
                        String sModelDirectory = modelTextField.getText();
                        if (sModelDirectory.contains("/")) {
                            sModelDirectory = sModelDirectory.substring(0, sModelDirectory.lastIndexOf("/"));
                        } else {
                            if (sModelDirectory.contains("\\")) {
                                sModelDirectory = sModelDirectory.substring(0, sModelDirectory.lastIndexOf("\\"));
                            }
                        }
                        File fModel = new File(sModelDirectory);
                        modelFileChooser.setInitialDirectory(fModel);
                    }
                    File selectedFile = modelFileChooser.showOpenDialog(primaryStage);

                    if (selectedFile != null) {
                        modelTextField.setText(selectedFile.getAbsolutePath());
                    }
                });
                grid.add(modelRepositoryLabel, 0, 0);
                grid.add(modelRepositoryTextField, 1, 0);
                grid.add(selectFolderModelLibraryButton, 2, 0);

                grid.add(modelLabel, 0, 1);
                grid.add(modelTextField, 1, 1);
                grid.add(selectModelButton, 2, 1);

                grid.add(modelPromptTemplateLabel, 0, 2);
                grid.add(modelPromptTemplateTextField, 1, 2);

                grid.add(cacheLabel, 0, 3);
                grid.add(cacheTextField, 1, 3);

                grid.add(cacheLocalIpLabel, 0, 4);
                grid.add(cacheLocalIpComboBox, 1, 4);

                grid.add(cipherMessageLabel, 0, 5);
                grid.add(cipherMessageCheckBox, 1, 5);
                grid.add(keyStorePassLabel, 0, 6);
                grid.add(keyStorePassTextField, 1, 6);
                grid.add(testKeystore, 2, 6);

                grid.add(chatUILabel, 0, 7);
                grid.add(chatUICheckBox, 1, 7);

                Button saveButton = new Button("Save");
                Button clearButton = new Button("Clear form");

                // Event handlers
                saveButton.setOnAction(e -> saveFormInformation("mc3d-ai.properties"));
                clearButton.setOnAction(e -> clearForm(grid));

                // Add buttons to the grid
                grid.add(saveButton, 0, 8);
                grid.add(clearButton, 1, 8);
                borderPane.setBottom(titledPane);

                tab.setContent(borderPane);

            } else {
                tab.setId("WV" + System.currentTimeMillis());

                if (title.equals("Principal")) {
                    tab.setClosable(false);
                    tab.setId("WV0");
                }
                pubSubFiles = new PubSubFiles();

                WebView webView = new WebView();
                webView.setId(tab.getId());
                hWebView.put(webView.getId(), webView);
                WebEngine webEngine = webView.getEngine();
                TextArea messageArea = new TextArea();
                messageArea.setPromptText("Write your message here...");
                TableView<ItemAttachedDocument> attachedDocumentsTableView = new TableView<>();
                TableColumn<ItemAttachedDocument, CheckBox> selectedColumn = new TableColumn<>("Local docs");
                selectedColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
                attachedDocumentsTableView.getColumns().add(selectedColumn);
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    Platform.runLater(() -> {
                        listLocalDocs(attachedDocumentsTableView);
                    });

                }, 0, 1000, TimeUnit.MILLISECONDS);
                Button sendMessageButton = new Button("Send message");
                sendMessageButton.setOnAction(e
                        -> sendMessage(sendMessageButton, "" + System.currentTimeMillis() + webView.getId(), messageArea, attachedDocumentsTableView)
                );

                Button clearChatButton = new Button("Clear Chat");
                clearChatButton.setOnAction(e -> clearChat(webView));
                BorderPane sendMessageBorderPane = new BorderPane();
                sendMessageBorderPane.setLeft(attachedDocumentsTableView);
                sendMessageBorderPane.setCenter(messageArea);

                VBox buttonsMessageVBox = new VBox(sendMessageButton, clearChatButton);
                buttonsMessageVBox.setAlignment(Pos.TOP_CENTER);
                buttonsMessageVBox.setSpacing(10);
                sendMessageBorderPane.setRight(buttonsMessageVBox);
                VBox chatVBox = new VBox(webView, sendMessageBorderPane);
                chatVBox.setAlignment(Pos.CENTER);
                chatVBox.setSpacing(10);
                tab.setContent(chatVBox);

            }
        }

        tabPane.getTabs().add(tab);

        return tabPane;
    }

    private void sendMessage(Button sendMessageButton, String id, TextArea messageArea, TableView<ItemAttachedDocument> attachedDocumentsTableView) {
        String message = messageArea.getText();
        if (!message.isEmpty()) {
            sendMessageButton.setDisable(true);
            try {
                int iAttachedDocuments = attachedDocumentsTableView.getItems().size();
                for (int i = 0; i < iAttachedDocuments; i++) {
                    ItemAttachedDocument item = attachedDocumentsTableView.getItems().get(i);
                    if (item.getSelected().isSelected()) {
                        message += "<br/><br/><small>localDoc=\"" + item.getSelected().getText() + "\"</small>";
                    }
                }
                pubSubFiles.putQuestion("", "" + id, message);

                // jeMensajesGlobales.setCaretPosition(doc.getLength()); not work
                addJeMensajes("" + new Date(), "" + id, "Me", message);
                messageArea.setText("");

            } catch (Exception e) {
                logTextArea.appendText("Send Error: " + e.toString());
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Send Error: " + e.toString());
            }

            sendMessageButton.setDisable(false);
        }
    }

    private void clearChat(WebView webView) {
        webView.getEngine().loadContent("<html><body></body></html>");
    }

    public void addJeMensajes(String timestamp, String id, String username, final String message) {

        try {
            WebView webView = null;
            String sWebViewId = "";
            if (id.contains("WV")) {
                sWebViewId = id.substring(id.indexOf("WV"), id.length());
                boolean existTab = false;
                Iterator<Tab> iTabs = tabPane.getTabs().iterator();
                while (iTabs.hasNext()) {
                    Tab tab = iTabs.next();
                    if (tab.getId().equals(sWebViewId)) {
                        existTab = true;
                        webView = hWebView.get(sWebViewId);
                        break;
                    }
                }
                if (!existTab) {
                    webView = hWebView.get("WV0");
                    hWebView.remove(sWebViewId);
                }
                String localMessage = message;
                if (localMessage.contains("\n")) {
                    localMessage = localMessage.replaceAll("\n", "<br/>");
                }
                String currentHtml = (String) webView.getEngine().executeScript("document.documentElement.outerHTML");
                String align = "right";
                String refId = ", question id: " + id;
                if (!username.toLowerCase().equals("me")) {
                    align = "left";
                    refId = ", Response to: <a href=\"#\" onclick=\"document.getElementById('" + id + "').scrollIntoView();\">" + id + "</a>";
                }
                String newHtml = currentHtml + "<div align=\"" + align + "\"><fieldset id=\"" + id + "\"><legend><b>" + username + " says:</b></legend>" + localMessage + "<br/><p align=\"right\"><small>[" + new Date() + "]" + refId + "</small></p></fieldset></div><br/>";
                if (username.toLowerCase().equals("yo")) {
                    hPendingConversations.put(id, newHtml);
                } else {
                    if (!existTab) {
                        String oldHtml = hPendingConversations.get(id);
                        newHtml = oldHtml + newHtml;
                    }
                    hPendingConversations.remove(id);
                }
                webView.getEngine().loadContent(newHtml);
                if (!username.toLowerCase().equals("me")) {
                    //To do setcaret position
                }
                webView.getEngine().executeScript("var range = document.createRange(); range.selectNodeContents(document.body); range.collapse(false); var sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(range);");
            }

        } catch (Exception e) {
            logTextArea.appendText("Add Messages error:" + e.toString() + "\n");
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Cayo:" + e.toString());
        }

    }

    private void handleClose(WindowEvent event) {
        // Perform any actions needed before closing the window
        System.out.println("Closing the window...");
        System.exit(0);
    }

    private void saveFormInformation(String sConfig) {
        System.out.println("Properties: " + properties);
        try {

            properties.setProperty("modelRepository", modelRepositoryTextField.getText());
            properties.setProperty("model", modelTextField.getText());
            properties.setProperty("modelPromptTemplate", modelPromptTemplateTextField.getText());
            properties.setProperty("cache", cacheTextField.getText());
            properties.setProperty("cacheLocalIp", cacheLocalIpComboBox.getValue());
            properties.setProperty("cipherMessage", String.valueOf(cipherMessageCheckBox.isSelected()));
            properties.setProperty("keyStorePass", keyStorePassTextField.getText());
            properties.setProperty("chatUI", String.valueOf(chatUICheckBox.isSelected()));
            FileOutputStream fos = new FileOutputStream(sConfig);
            properties.store(fos, "MC3D AI- configuration");
            fos.close();
            loadProperties(sConfig);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    igniteControl.setDisable(false);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm(GridPane grid) {
        // Implementar lógica para limpiar el formulario
        for (Node node : grid.getChildren()) {
            if (node instanceof TextField) {
                ((TextField) node).clear();
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setSelected(false);
            } else if (node instanceof ComboBox) {
                ((ComboBox<?>) node).getSelectionModel().clearSelection();
            }
        }

    }

    private void loadProperties(String sConfig) {
        // Cargar las propiedades desde el archivo mc3d-ai.properties
        try {
            FileInputStream input = new FileInputStream(sConfig);
            properties.load(input);
            input.close();

            modelRepositoryTextField.setText(properties.getProperty("modelRepository", "repository"));

            modelTextField.setText(properties.getProperty("model", ""));
            modelPromptTemplateTextField.setText(properties.getProperty("modelPromptTemplate", ""));
            cacheTextField.setText(properties.getProperty("cache", ""));
            cacheLocalIpComboBox.setValue(properties.getProperty("cacheLocalIp", ""));
            cipherMessageCheckBox.setSelected(Boolean.parseBoolean(properties.getProperty("cipherMessage", "false")));
            keyStorePassTextField.setText(properties.getProperty("keyStorePass", ""));
            try {
                Security.getPublicKeyFromKeystore(cacheTextField.getText() + ".pfx", keyStorePassTextField.getText(), cacheTextField.getText(), "PKCS12");
                BackgroundFill backgroundFill = new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                Background background = new Background(backgroundFill);
                testKeystore.setBackground(background);

            } catch (Exception ex) {
                BackgroundFill backgroundFill = new BackgroundFill(Color.RED, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                Background background = new Background(backgroundFill);
                testKeystore.setBackground(background);
                cipherMessageCheckBox.setSelected(false);
            }
            chatUICheckBox.setSelected(Boolean.parseBoolean(properties.getProperty("chatUI")));

        } catch (IOException e) {
            e.printStackTrace();
            // Manejar la excepción según tus necesidades
        }
    }

    private List listIPAddresses() {
        List lMyCustomAddress = new ArrayList<>();
        lMyCustomAddress.add("");
        Enumeration<NetworkInterface> net = null;
        try {
            net = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        if (net == null) {
            logTextArea.appendText("ListIPAddresses: No network interfaces found.\n");
            throw new RuntimeException("No network interfaces found.");
        }

        while (net.hasMoreElements()) {
            NetworkInterface element = net.nextElement();
            try {
                if (element.isVirtual() || element.isLoopback()) {
                    // discard virtual and loopback interface (127.0.0.1)
                    continue;
                }
                Enumeration<InetAddress> addresses = element.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        if (ip.isSiteLocalAddress()) {
                            lMyCustomAddress.add(ip.getHostAddress());
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return lMyCustomAddress;

    }

    public static class StatusDataRow {

        private final SimpleStringProperty name;
        private final SimpleStringProperty ip;
        private final SimpleStringProperty llm;
        private final SimpleStringProperty lastAccess;

        public StatusDataRow(String name, String ip, String llm, String lastAccess) {
            this.name = new SimpleStringProperty(name);
            this.ip = new SimpleStringProperty(ip);
            this.llm = new SimpleStringProperty(llm);
            this.lastAccess = new SimpleStringProperty(lastAccess);
        }

        public String getName() {
            return name.get();
        }

        public String getIp() {
            return ip.get();
        }

        public String getLlm() {
            return llm.get();
        }

        public String getLastAccess() {
            return lastAccess.get();
        }
    }

    public static class ModelDataRow {

        private final SimpleStringProperty ranking;
        private final SimpleStringProperty model;
        private final SimpleStringProperty lastUpdate;

        public ModelDataRow(String ranking, String model, String lastUpdate) {
            this.ranking = new SimpleStringProperty(ranking);
            this.model = new SimpleStringProperty(model);
            this.lastUpdate = new SimpleStringProperty(lastUpdate);
        }

        public String getRanking() {
            return ranking.get();
        }

        public String getModel() {
            return model.get();
        }

        public String getLastUpdate() {
            return lastUpdate.get();
        }

    }

    public static class ItemAttachedDocument {

        private CheckBox selected;

        public ItemAttachedDocument(CheckBox selected) {
            this.selected = selected;
        }

        public CheckBox getSelected() {
            return selected;
        }

        public void setSelected(CheckBox selected) {
            this.selected = selected;
        }
    }

    public void listLocalDocs(TableView<ItemAttachedDocument> attachedDocumentsTableView) {
        UnitaryProcessPublishFile unitaryProcessPublishFile = new UnitaryProcessPublishFile();
        List<String> pdfList = unitaryProcessPublishFile.getPDFList("localdocs");
        for (String name : pdfList) {
            int iLocalDocs = attachedDocumentsTableView.getItems().size();
            boolean existLocalDoc = false;
            for (int i = 0; i < iLocalDocs; i++) {
                ItemAttachedDocument item = attachedDocumentsTableView.getItems().get(i);
                if (item.getSelected().getText().equals(name)) {
                    existLocalDoc = true;
                    break;
                }
            }
            if (!existLocalDoc) {
                CheckBox cbLocalDoc = new CheckBox(name);
                ItemAttachedDocument item1 = new ItemAttachedDocument(cbLocalDoc);
                attachedDocumentsTableView.getItems().add(item1);
            }

        }
    }

    public static void main(String[] args) {
        /*Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());*/
        launch(args);

    }
}
