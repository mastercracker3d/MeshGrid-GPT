package cl.mc3d.ai;

import cl.mc3d.as4p.ui.ComboBox;
import cl.mc3d.as4p.ui.CommandButton;
import cl.mc3d.as4p.ui.DataTable;
import cl.mc3d.as4p.ui.Div;
import cl.mc3d.as4p.ui.InputText;
import cl.mc3d.as4p.ui.OutputLabel;
import cl.mc3d.as4p.ui.SelectBooleanCheckbox;
import cl.mc3d.as4p.ui.Tree;
import cl.mc3d.as4p.ui.VDMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class Console extends JFrame {

    private PubSubFiles pubSubFiles = null;
    private ModelsInfo modelsInfo;
    private HashMap<String, JTextPane> hWebView = null;
    private HashMap<String, String> hPendingConversations = new HashMap();
    private InputText modelRepositoryTextField;
    private InputText modelTextField;
    private InputText modelPromptTemplateTextField;
    private InputText cacheTextField;
    private ComboBox cacheLocalIpComboBox;
    private SelectBooleanCheckbox cipherMessageCheckBox;
    private JPasswordField keyStorePassTextField;
    private CommandButton testKeystore;
    private SelectBooleanCheckbox chatUICheckBox;
    private JToggleButton igniteControl;
    private JTextArea logTextArea;
    private String locationStart;
    private Properties properties = new Properties();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private TimerService timerService;
    private DefaultTableModel dtmGridStatus = null;
    private String theme = "midnight";
    private VDMutableTreeNode dmtProjects = null;
    private Tree jtreeProjects = null;

    public Console(TimerService timerService) {
        setTitle("MeshGrid GPT - v1.1.0");
        this.timerService = timerService;
        ImageIcon icon = new ImageIcon(getClass().getResource("/resources/meshgrid_gpt-icon.png"));
        setIconImage(icon.getImage());
        locationStart = System.getProperty("user.dir");
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (!"Nimbus".equals(info.getName())) {
                    continue;
                }
                UIManager.setLookAndFeel(info.getClassName());

                SwingUtilities.updateComponentTreeUI(this);
                break;
            }
        } catch (Exception ex) {
            System.out.println("Error manejado" + ex.toString());
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        int chatSession = 0;
        CommandButton newChatButton = new CommandButton("New Chat");
        newChatButton.setTheme(theme, false);
        newChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createTabPane("Chat: " + tabbedPane.getTabCount(), tabbedPane);

            }
        });
        igniteControl = new JToggleButton("Ignite the cluster");
        igniteControl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (igniteControl.isSelected()) {
                    timerService.stop();
                    timerService.config();
                    timerService.start();
                    igniteControl.setBackground(Color.yellow);

                    System.out.println("Cluster on");
                } else {
                    System.out.println("Cluster off");
                    igniteControl.setBackground(Color.red);
                    igniteControl.setText("Ignite the cluster");
                    timerService.stop();
                }
            }
        });
        ImageIcon mc3dIcon = new ImageIcon(getClass().getResource("/resources/mc3d_logo.png"));
        CommandButton jbMc3dLogo = new CommandButton();
        jbMc3dLogo.setIcon(mc3dIcon);
        jbMc3dLogo.setTheme(theme, false);
        jbMc3dLogo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageIcon qrIcon = new ImageIcon(getClass().getResource("/resources/mc3d-qr.png"));

                Div jpQR = new Div("MeshGrid GPT by MC3D", null, new BorderLayout());
                jpQR.setTheme(theme, false);
                CommandButton jbMc3dQR = new CommandButton();
                jbMc3dQR.setIcon(qrIcon);

                jpQR.add("Center", jbMc3dQR);

                JOptionPane.showMessageDialog(Console.this, jpQR);
            }
        });
        Div leftPanel = new Div();
        leftPanel.setTheme(theme, false);

        leftPanel.setLayout(new GridLayout(3, 1));
        leftPanel.add(newChatButton);
        leftPanel.add(igniteControl);
        leftPanel.add(jbMc3dLogo);
        Div compressleftPanel = new Div();
        compressleftPanel.setTheme(theme, false);

        compressleftPanel.setLayout(new FlowLayout());

        compressleftPanel.add(leftPanel);
        getContentPane().add(compressleftPanel, BorderLayout.WEST);
        hWebView = new HashMap();
        createTabPane("Setup", tabbedPane);
        createTabPane("Status", tabbedPane);
        createTabPane("Principal", tabbedPane);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int index = tabbedPane.getSelectedIndex();
                    String oldTitle = tabbedPane.getTitleAt(index);

                    if (((!oldTitle.equals("Principal")) && (!oldTitle.equals("Status")) && (!oldTitle.equals("Setup")))) {
                        String newName = JOptionPane.showInputDialog(Console.this, "Enter new name for Tab:");
                        if (newName != null && !newName.isEmpty()) {
                            tabbedPane.setTitleAt(index, newName);
                        }
                    }
                }
            }
        });
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {

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

        }, 0, 50, TimeUnit.MILLISECONDS);
        loadProperties("mc3d-ai.properties");

        setVisible(true);
    }

    public JToggleButton getIgniteControl() {
        return igniteControl;
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
            cacheLocalIpComboBox.setSelectedItem(properties.getProperty("cacheLocalIp", ""));
            cipherMessageCheckBox.setSelected(Boolean.parseBoolean(properties.getProperty("cipherMessage", "false")));
            keyStorePassTextField.setText(properties.getProperty("keyStorePass", ""));
            try {
                Security.getPublicKeyFromKeystore(cacheTextField.getText() + ".pfx", keyStorePassTextField.getText(), cacheTextField.getText(), "PKCS12");

            } catch (Exception ex) {
                cipherMessageCheckBox.setSelected(false);
            }
            chatUICheckBox.setSelected(Boolean.parseBoolean(properties.getProperty("chatUI")));

        } catch (IOException e) {
            e.printStackTrace();
            // Manejar la excepción según tus necesidades
        }
    }

    private void saveFormInformation(String sConfig) {
        System.out.println("Properties: " + properties);
        try {

            properties.setProperty("modelRepository", modelRepositoryTextField.getText());
            properties.setProperty("model", modelTextField.getText());
            properties.setProperty("modelPromptTemplate", modelPromptTemplateTextField.getText());
            properties.setProperty("cache", cacheTextField.getText());
            properties.setProperty("cacheLocalIp", cacheLocalIpComboBox.getSelectedItem().toString());
            properties.setProperty("cipherMessage", String.valueOf(cipherMessageCheckBox.isSelected()));
            properties.setProperty("keyStorePass", keyStorePassTextField.getText());
            properties.setProperty("chatUI", String.valueOf(chatUICheckBox.isSelected()));
            FileOutputStream fos = new FileOutputStream(sConfig);
            properties.store(fos, "MC3D AI- configuration");
            fos.close();
            loadProperties(sConfig);
            igniteControl.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        modelRepositoryTextField.setText("");
        modelTextField.setText("");
        modelPromptTemplateTextField.setText("");
        cacheTextField.setText("");
        cacheLocalIpComboBox.setSelectedIndex(0);
        cipherMessageCheckBox.setSelected(false);
        keyStorePassTextField.setText("");
        chatUICheckBox.setSelected(true);
    }

    private void createTabPane(String title, JTabbedPane tabbedPane) {
        Div mainPanel = new Div(null, null, new BorderLayout());
        mainPanel.setTheme(theme, false);
        JTab tab = new JTab(title, mainPanel);
        if (title.equals("Status")) {
            mainPanel.setName(title);

            tab.setId("Status");
            Vector vGridStatusTitles = new Vector();
            vGridStatusTitles.add("Name");
            vGridStatusTitles.add("IP");
            vGridStatusTitles.add("LLM");
            vGridStatusTitles.add("Last Access");

            dtmGridStatus = new DefaultTableModel(vGridStatusTitles, 0);
            DataTable dtGridStatus = new DataTable(dtmGridStatus);
            dtGridStatus.setTheme(theme);
            dtGridStatus.setShowVerticalLines(true);
            dtGridStatus.setShowHorizontalLines(true);
            dtGridStatus.getTableHeader().setReorderingAllowed(false);
            dtGridStatus.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            dtGridStatus.setSelectionMode(2);
            JScrollPane jsGeneral = new JScrollPane(dtGridStatus);
            mainPanel.add(jsGeneral, BorderLayout.CENTER);
            logTextArea = new JTextArea();
            logTextArea.setEditable(false);
            logTextArea.setRows(20);
            JScrollPane jslogTextArea = new JScrollPane(logTextArea);

            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.setOpaque(false);
            inputPanel.add(jslogTextArea, BorderLayout.CENTER);
            CommandButton jbClear = new CommandButton("Clear Logs");
            jbClear.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    logTextArea.setText("");
                }
            });
            jbClear.setTheme(theme, false);
            inputPanel.add(jbClear, BorderLayout.EAST);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);

        } else {
            if (title.equals("Setup")) {
                tab.setId("Setup");
                Div progressBarPanel = new Div();
                progressBarPanel.setTheme(theme, true);
                progressBarPanel.setOpaque(false);
                progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS));
                JScrollPane jsProgressBarPanel = new JScrollPane(progressBarPanel);
                jsProgressBarPanel.setOpaque(false);
                jsProgressBarPanel.setPreferredSize(new Dimension(300, 20));

                Vector vLLMListTitles = new Vector();
                vLLMListTitles.add("Ranking");
                vLLMListTitles.add("Model");
                vLLMListTitles.add("Last Update");
                DefaultTableModel dtmLLMList = new DefaultTableModel(vLLMListTitles, 0);
                DataTable dtLLMList = new DataTable(dtmLLMList);
                dtLLMList.setTheme(theme);
                dtLLMList.setShowVerticalLines(true);
                dtLLMList.setShowHorizontalLines(true);
                dtLLMList.getTableHeader().setReorderingAllowed(false);
                dtLLMList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                dtLLMList.setSelectionMode(2);
                TableRowSorter<TableModel> sorter = new TableRowSorter<>(dtLLMList.getModel());
                dtLLMList.setRowSorter(sorter);
                InputText field = new InputText();
                mainPanel.add(field, BorderLayout.NORTH);

                field.getDocument().addDocumentListener(new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        String text = field.getText();
                        if (text.trim().length() == 0) {
                            sorter.setRowFilter(null);
                        } else {
                            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                        }
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        String text = field.getText();
                        if (text.trim().length() == 0) {

                            sorter.setRowFilter(null);
                        } else {
                            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                        }
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        // Esto generalmente no se usa con campos de texto
                    }
                });
                dtLLMList.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int index = dtLLMList.convertRowIndexToModel(dtLLMList.getSelectedRow()) + 1;
                            int max = sorter.getViewRowCount();
                            System.out.println("Ranking id:" + index);
                            for (int position = 0; position < max; position++) {
                                if (dtLLMList.getValueAt(position, 0).toString().equals("" + index)) {
                                    System.out.println("Encontrado en la posicion: " + position);
                                    String sModel = "" + dtmLLMList.getValueAt(dtLLMList.convertRowIndexToModel(position), 1);
                                    System.out.println("Encontrado en la sModel: " + sModel);
                                    List<LLM> lListModelType = modelsInfo.listModelType(sModel);
                                    JComboBox<String> comboBox = new JComboBox<>();
                                    for (LLM llm : lListModelType) {
                                        comboBox.addItem(llm.getName());
                                    }

                                    JPanel panel = new JPanel();
                                    panel.add(new JLabel("Select LLM version:"));
                                    panel.add(comboBox);

                                    int result = JOptionPane.showConfirmDialog(Console.this, panel, "LLM Download",
                                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                                    if (result == JOptionPane.OK_OPTION) {
                                        String sModelType = (String) comboBox.getSelectedItem();

                                        DownloadModels downloadModels = new DownloadModels(modelRepositoryTextField.getText(), sModel, sModelType);
                                        int iListModelTypeSize = lListModelType.size();
                                        String sSize = "";
                                        for (int i = 1; i < iListModelTypeSize; i++) {
                                            if (lListModelType.get(i).getName().equals(sModelType)) {
                                                sSize = lListModelType.get(i).getSize();
                                                break;
                                            }

                                        }
                                        JProgressItem progressItem = new JProgressItem(modelRepositoryTextField.getText(), sModel, sModelType, sSize);
                                        progressBarPanel.add(progressItem);
                                        JOptionPane.showMessageDialog(Console.this, "Option selected: " + sModelType);
                                        mainPanel.updateUI();
                                        downloadModels.start();
                                        break;
                                    }

                                }

                            }

                        }
                    }
                });
                JScrollPane jsGeneral = new JScrollPane(dtLLMList);
                mainPanel.add(jsGeneral, BorderLayout.CENTER);
                modelsInfo = new ModelsInfo(dtmLLMList);
                modelsInfo.start();

                JPanel jpGroupSetup = new JPanel(new GridLayout(9, 3));
                jpGroupSetup.setOpaque(false);
                OutputLabel modelRepositoryLabel = new OutputLabel("Model repository:");
                modelRepositoryLabel.setTheme(theme, false);
                jpGroupSetup.add(modelRepositoryLabel);
                modelRepositoryTextField = new InputText();
                jpGroupSetup.add(modelRepositoryTextField);
                CommandButton jbAssignRepository = new CommandButton("Assign Repository");
                jbAssignRepository.setTheme(theme, false);
                jpGroupSetup.add(jbAssignRepository);
                jbAssignRepository.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser fileChooser = new JFileChooser(new File(modelRepositoryTextField.getText()));
                        fileChooser.setDialogTitle("Select Directory");
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int result = fileChooser.showOpenDialog(Console.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedDirectory = fileChooser.getSelectedFile();
                            modelRepositoryTextField.setText(selectedDirectory.getAbsolutePath());
                        }
                    }
                });

                OutputLabel modelLabel = new OutputLabel("Model:");
                modelLabel.setTheme(theme, false);
                jpGroupSetup.add(modelLabel);
                modelTextField = new InputText();
                jpGroupSetup.add(modelTextField);
                CommandButton jbAssignModel = new CommandButton("Assign Model");
                jbAssignModel.setTheme(theme, false);
                jpGroupSetup.add(jbAssignModel);
                jbAssignModel.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser fileChooser = new JFileChooser(new File(modelTextField.getText()));
                        fileChooser.setFileFilter(new FileFilter() {
                            @Override
                            public boolean accept(File f) {
                                if (f.isDirectory()) {
                                    return true;
                                }
                                String extension = getExtension(f);
                                return extension != null && extension.equals("gguf");
                            }

                            @Override
                            public String getDescription() {
                                return "GGUF files (*.gguf)";
                            }

                            private String getExtension(File f) {
                                String fileName = f.getName();
                                int index = fileName.lastIndexOf('.');
                                if (index > 0 && index < fileName.length() - 1) {
                                    return fileName.substring(index + 1).toLowerCase();
                                }
                                return null;
                            }
                        });
                        fileChooser.setDialogTitle("Select the LLM");
                        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        int result = fileChooser.showOpenDialog(Console.this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedDirectory = fileChooser.getSelectedFile();
                            modelTextField.setText(selectedDirectory.getAbsolutePath());
                        }
                    }
                });
                OutputLabel modelPromptTemplateLabel = new OutputLabel("Model Prompt Template:");
                modelPromptTemplateLabel.setTheme(theme, false);
                jpGroupSetup.add(modelPromptTemplateLabel);
                modelPromptTemplateTextField = new InputText();
                jpGroupSetup.add(modelPromptTemplateTextField);
                jpGroupSetup.add(new JLabel(""));

                OutputLabel cacheLabel = new OutputLabel("Cache:");
                cacheLabel.setTheme(theme, false);
                jpGroupSetup.add(cacheLabel);
                cacheTextField = new InputText();
                jpGroupSetup.add(cacheTextField);
                jpGroupSetup.add(new JLabel(""));

                OutputLabel cacheLocalIpLabel = new OutputLabel("Cache Local IP:");
                cacheLocalIpLabel.setTheme(theme, false);

                jpGroupSetup.add(cacheLocalIpLabel);
                cacheLocalIpComboBox = new ComboBox();
                cacheLocalIpComboBox.setTheme(theme);
                for (String sIp : listIPAddresses()) {
                    cacheLocalIpComboBox.addItem(sIp);
                }
                jpGroupSetup.add(cacheLocalIpComboBox);
                jpGroupSetup.add(new JLabel(""));

                OutputLabel cipherMessageLabel = new OutputLabel("Cipher Message:");
                cipherMessageLabel.setTheme(theme, false);
                jpGroupSetup.add(cipherMessageLabel);
                cipherMessageCheckBox = new SelectBooleanCheckbox();
                cipherMessageCheckBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (cipherMessageCheckBox.isSelected()) {
                            testKeystore.setEnabled(true);
                        } else {
                            testKeystore.setEnabled(false);
                            testKeystore.setForeground(Color.BLACK);
                        }
                    }
                });

                jpGroupSetup.add(cipherMessageCheckBox);
                jpGroupSetup.add(new JLabel(""));

                OutputLabel keyStorePassLabel = new OutputLabel("KeyStore Password:");
                keyStorePassLabel.setTheme(theme, false);

                jpGroupSetup.add(keyStorePassLabel);
                keyStorePassTextField = new JPasswordField();
                jpGroupSetup.add(keyStorePassTextField);
                testKeystore = new CommandButton("Test KeyStore");
                testKeystore.setTheme(theme, false);
                testKeystore.setEnabled(false);
                testKeystore.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if ((cipherMessageCheckBox.isSelected()) && (!keyStorePassTextField.getText().isEmpty())) {
                            File f = new File(cacheTextField.getText() + ".pfx");
                            if (!f.exists()) {
                                JOptionPane.showMessageDialog(Console.this, "The KeyStore: " + cacheTextField.getText() + ".pfx not exists");
                                testKeystore.setEnabled(false);

                            } else {
                                try {
                                    Security.getPublicKeyFromKeystore(cacheTextField.getText() + ".pfx", new String(keyStorePassTextField.getPassword()), cacheTextField.getText(), "PKCS12");
                                    testKeystore.setForeground(Color.GREEN);
                                } catch (Exception ex) {
                                    testKeystore.setForeground(Color.RED);
                                    cipherMessageCheckBox.setSelected(false);
                                    testKeystore.setEnabled(false);
                                    JOptionPane.showMessageDialog(Console.this, "The password for KeyStore: " + cacheTextField.getText() + ".pfx is invalid");
                                }
                            }
                        }
                    }
                });

                jpGroupSetup.add(testKeystore);

                OutputLabel chatUILabel = new OutputLabel("Chat UI:");
                chatUILabel.setTheme(theme, false);

                jpGroupSetup.add(chatUILabel);
                chatUICheckBox = new SelectBooleanCheckbox();
                chatUICheckBox.setTheme(theme);
                jpGroupSetup.add(chatUICheckBox);
                jpGroupSetup.add(new JLabel(""));

                mainPanel.add(jsProgressBarPanel, BorderLayout.EAST);
                JPanel jpButtonsSetup = new JPanel(new FlowLayout(FlowLayout.CENTER));
                jpButtonsSetup.setOpaque(false);
                CommandButton jbSave = new CommandButton("Save");
                jbSave.setTheme(theme, false);

                jbSave.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        saveFormInformation("mc3d-ai.properties");
                    }
                });

                jpButtonsSetup.add(jbSave);
                CommandButton jbClear = new CommandButton("Clear");
                jbClear.setTheme(theme, false);

                jpButtonsSetup.add(jbClear);
                jbClear.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        clearForm();
                    }
                });
                CommandButton jbReload = new CommandButton("Reload");
                jbReload.setTheme(theme, false);

                jpButtonsSetup.add(jbReload);
                jbReload.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        loadProperties("mc3d-ai.properties");
                    }
                });
                jpGroupSetup.add(new JLabel(""));
                jpGroupSetup.add(jpButtonsSetup);
                jpGroupSetup.add(new JLabel(""));

                JPanel jpPackSetup = new JPanel(new FlowLayout(FlowLayout.CENTER));
                jpPackSetup.setOpaque(false);
                jpPackSetup.add(jpGroupSetup);
                mainPanel.add(jpPackSetup, BorderLayout.SOUTH);

            } else {
                tab.setId("WV" + System.currentTimeMillis());
                if (title.equals("Principal")) {
                    tab.setId("WV0");
                    tab.setClosable(false);
                } else {
                    tab.setClosable(true);
                }
                pubSubFiles = new PubSubFiles();
                JTextPane chatPane = new JTextPane();
                chatPane.setName(tab.getId());
                chatPane.setContentType("text/html");
                chatPane.setText("<html><body></body></html>");
                hWebView.put(tab.getId(), chatPane);
                chatPane.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(chatPane);
                JTextArea messageArea = new JTextArea();
                messageArea.setRows(20);

                JScrollPane jsMessageArea = new JScrollPane(messageArea);
                dmtProjects = new VDMutableTreeNode("Local Docs");
                dmtProjects.setName("Local Documents");

                jtreeProjects = new Tree(dmtProjects);
                jtreeProjects.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        final JTree jtree = (JTree) e.getSource();
                        final VDMutableTreeNode vdmLocalDocs = (VDMutableTreeNode) jtree.getLastSelectedPathComponent();
                        if ((e.getButton() == 1) && (e.getClickCount() == 2)) {
                            String dato = "localDoc=\"";
                            int iPath = vdmLocalDocs.getUserObjectPath().length;
                            for (int i = 1; i < iPath; i++) {
                                dato = dato + "/" + vdmLocalDocs.getUserObjectPath()[i];
                            }

                            if (dato.toLowerCase().endsWith(".pdf") || dato.toLowerCase().endsWith(".txt")) {
                                dato = dato + "\"";
                                messageArea.append(" " + dato);
                                jtreeProjects.removeSelectionPath(jtreeProjects.getAnchorSelectionPath());
                            }
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                    }
                });
                jtreeProjects.setDragEnabled(true);
                jtreeProjects.setDropMode(DropMode.ON_OR_INSERT);
                jtreeProjects.setTheme(theme);
                JScrollPane jsLocalDocs = new JScrollPane(jtreeProjects);
                final Timer t = new javax.swing.Timer(0, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        listLocalDocs();
                    }
                });
                t.start();
                t.setRepeats(true);
                t.setDelay(1000);

                CommandButton sendMessageButton = new CommandButton("Send");
                sendMessageButton.setTheme(theme, false);
                sendMessageButton.addActionListener(e -> {
                    String message = messageArea.getText();
                    if (!message.isEmpty()) {
                        sendMessage(sendMessageButton, "" + System.currentTimeMillis() + tab.getId(), messageArea);
                        messageArea.setText("");
                    }
                });
                CommandButton clearChatMessageButton = new CommandButton("Clear Chat");
                clearChatMessageButton.setTheme(theme, false);
                clearChatMessageButton.addActionListener(e -> {
                    String message = messageArea.getText();
                    chatPane.setText("<html><body></body>></html>");
                });
                CommandButton clearMessageButton = new CommandButton("Clear Message");
                clearMessageButton.setTheme(theme, false);
                clearMessageButton.addActionListener(e -> {
                    String message = messageArea.getText();
                    messageArea.setText("");
                });
                JPanel groupPanel = new JPanel();
                groupPanel.setLayout(new GridLayout(3, 1));
                groupPanel.add(sendMessageButton);
                groupPanel.add(clearChatMessageButton);
                groupPanel.add(clearMessageButton);

                JPanel inputPanel = new JPanel(new BorderLayout());
                JSplitPane jspMessageArea = new JSplitPane();
                jspMessageArea.setLeftComponent(jsLocalDocs);
                jspMessageArea.setRightComponent(jsMessageArea);
                inputPanel.add(jspMessageArea, BorderLayout.CENTER);
                jspMessageArea.setDividerLocation(300);

                inputPanel.add(groupPanel, BorderLayout.EAST);
                mainPanel.add(scrollPane, BorderLayout.CENTER);
                mainPanel.add(inputPanel, BorderLayout.SOUTH);
            }
        }
        tabbedPane.addTab(title, tab);
    }

    public DefaultTableModel getDtmGridStatus() {
        return dtmGridStatus;
    }

    private void sendMessage(JButton sendMessageButton, String id, JTextArea messageArea) {
        String message = messageArea.getText();
        if (!message.isEmpty()) {
            sendMessageButton.setEnabled(false);
            try {
                pubSubFiles.putQuestion("", "" + id, message);

                // jeMensajesGlobales.setCaretPosition(doc.getLength()); not work
                addJeMensajes("" + new Date(), "" + id, "Me", message);
                messageArea.setText("");
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Send Error: " + e.toString());
            }
            sendMessageButton.setEnabled(true);
        }
    }

    public JTextArea getLogTextArea() {
        return logTextArea;
    }

    public void addJeMensajes(String timestamp, String id, String username, final String message) {

        try {
            JTextPane jTextPane = null;
            String sWebViewId = "";
            if (id.contains("WV")) {
                System.out.println("Id: " + id);
                sWebViewId = id.substring(id.indexOf("WV"), id.length());
                boolean existTab = false;
                for (Component component : tabbedPane.getComponents()) {
                    JTab tab = (JTab) component;
                    if (tab.getId().equals(sWebViewId)) {
                        System.out.println("Encontrado el editor: " + sWebViewId);
                        existTab = true;
                        jTextPane = hWebView.get(sWebViewId);
                        break;
                    }
                }

                if (!existTab) {
                    jTextPane = hWebView.get("WV0");
                    System.out.println("Encontre el jTextPane");
                    hWebView.remove(sWebViewId);
                }
                String localMessage = message;
                if (localMessage.contains("\n")) {
                    localMessage = localMessage.replaceAll("\n", "<br/>");
                }
                String currentHtml = (String) jTextPane.getText();

                String align = "right";
                String headerColor = "orange";
                String refId = ", question id: " + id;
                String anchor="<a name=\"" + id + "\">" + username + " says:</a>";
                if (!username.toLowerCase().equals("me")) {
                   anchor=username + " says:";
                    headerColor = "yellow";
                    align = "left";
                    refId = ", Response to: <a href=\"#" + id + "\">" + id + "</a>";
                }
                String newHtml = currentHtml.substring(0, currentHtml.lastIndexOf("</body>")) + "<div align=\"" + align + "\"><table border=1 width=50%><th bgcolor=" + headerColor + ">"+anchor+"</th><tr><td>" + localMessage + "</td></tr><tr><td><small>[" + new Date() + "]" + refId + "</small></td></tr></table></div><br/></body></html>";
                if (username.toLowerCase().equals("me")) {
                    hPendingConversations.put(id, newHtml);
                } else {
                    if (!existTab) {
                        String oldHtml = hPendingConversations.get(id);
                        newHtml = oldHtml + newHtml;

                    }
                    hPendingConversations.remove(id);
                }

                //localDocText = "<table id=\"" + localDoc + "\"><th>" + localDoc + "</th><tr><td>" + localDocText + "</td></tr></table>";
                jTextPane.setText(newHtml);
                jTextPane.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(final HyperlinkEvent pE) {
                        if (HyperlinkEvent.EventType.ACTIVATED == pE.getEventType()) {
                            System.out.println("JEditorPane link click: url='" + pE.getURL() + "' description='" + pE.getDescription() + "'");
                            String reference = pE.getDescription();
                            if (reference != null && reference.startsWith("#")) { // link must start with # to be internal reference
                                reference = reference.substring(1);
                                ((JTextPane) pE.getSource()).scrollToReference(reference);
                            }
                        }
                    }
                });
                if (!username.toLowerCase().equals("me")) {
                    //To do setcaret position
                }
                // jTextPane.getEngine().executeScript("var range = document.createRange(); range.selectNodeContents(document.body); range.collapse(false); var sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(range);");
            }

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Add Messages error: " + e.toString());
            e.printStackTrace();
        }

    }

    private List<String> listIPAddresses() {
        List lMyCustomAddress = new ArrayList<>();
        lMyCustomAddress.add("");
        Enumeration<NetworkInterface> net = null;
        try {
            net = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        if (net == null) {
//            logTextArea.appendText("ListIPAddresses: No network interfaces found.\n");
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

    public void listLocalDocs() {
        UnitaryProcessPublishFile unitaryProcessPublishFile = new UnitaryProcessPublishFile();
        List<String> inputs = unitaryProcessPublishFile.getLocalDocsList("localdocs");
        Collections.sort(inputs);
        if (inputs.size() > 0) {
            for (String input : inputs) {
                input = input.replaceAll("/localdocs/", "");
                VDMutableTreeNode dmtBase = dmtProjects;
                StringTokenizer st = new StringTokenizer(input, "/");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    VDMutableTreeNode dmtNuevo = new VDMutableTreeNode(token);
                    dmtNuevo.setName(token);
                    if (!searchTreeLeaf(dmtBase, token)) {
                        dmtBase.add(dmtNuevo);
                    }
                    dmtBase = getTreeLeaf(dmtBase, token);
                }
                dmtBase = dmtProjects;
            }
            jtreeProjects.updateUI();
            jtreeProjects.validate();
            jtreeProjects.getParent().validate();
        }
    }

    public boolean searchTreeLeaf(VDMutableTreeNode vdmProject, String buscar) {
        boolean existe = false;
        for (int i = 0; i < vdmProject.getChildCount(); i++) {
            VDMutableTreeNode dmt = (VDMutableTreeNode) vdmProject.getChildAt(i);
            if (dmt.getName().toLowerCase().equals(buscar.toString().toLowerCase())) {
                existe = true;
                break;
            }
        }
        return existe;
    }

    public VDMutableTreeNode getTreeLeaf(VDMutableTreeNode vdmProject, String buscar) {
        VDMutableTreeNode existe = vdmProject;
        for (int i = 0; i < vdmProject.getChildCount(); i++) {
            VDMutableTreeNode dmt
                    = (VDMutableTreeNode) vdmProject.getChildAt(i);
            if (dmt.getName().toLowerCase().equals(buscar.toString().toLowerCase())) {
                existe = dmt;
                break;
            }
        }
        return existe;
    }
}
