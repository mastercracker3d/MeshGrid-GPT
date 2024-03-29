/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import static cl.mc3d.ai.DescompresorZip.descomprimirArchivoZip;
import cl.mc3d.gpt4all.LLModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 *
 * @author maste
 */
public class TimerService implements Runnable {

    private Thread thread = null;
    private boolean needConfig = false;
    private Properties p = new Properties();
    private String sConfig = "mc3d-ai.properties";
    private Ignite ignite = null;
    private IgniteCache igniteCache = null;
    private String modelFilePath;
    private String modelPromptTemplate;
    private String libraryPath;
    private String cacheName;
    private String cacheLocalIp;
    private String hostname;
    private String cipherMessage;
    private String keyStorePass;
    private ToggleButton igniteControl;
    private TextArea logTextArea;
    private ObservableList<FXChat.StatusDataRow> data;
    private String locationStart;

    public TimerService(String locationStart, ToggleButton igniteControl, TextArea logTextArea, ObservableList<FXChat.StatusDataRow> data) {
        this.locationStart = locationStart;
        this.igniteControl = igniteControl;
        this.logTextArea = logTextArea;
        this.data = data;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                igniteControl.setDisable(true);

            }
        });
        logTextArea.appendText("Starting MeshGrid \n");
        Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Starting MeshGrid file integration ");
        config();
        start();

    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
        }
        thread.start();

    }

    public void config() {
        needConfig = false;
        try {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
        } catch (UnknownHostException ex) {
            System.out.println("Hostname can not be resolved");
        }
        File fConfig = new File(sConfig);

        try {
            if (!fConfig.exists()) {
                needConfig = true;
                String sStep = "Please set Model name in mc3d-ai.properties";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Model Prompt Template in mc3d-ai.properties";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Cache name in mc3d-ai.properties (if cipherMessage is setting to true, the keystore will be name to <CacheName>.pfx";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Cache Local IP for lockup other nodes in mc3d-ai.properties";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set CipherMessage to true if you need to turn the conversation privates in the network (AES) in mc3d-ai.properties";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set keyStorePass in mc3d-ai.properties if you are set the cipherMessage to true, you can share the keystore (or the Certificate) with other machines or cluster Locations";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set chatUI to false in mc3d-ai.properties if you need to run the Cluster without UI for integration or scaling up the Grid";
                logTextArea.appendText(sStep + "\n");
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                FileOutputStream fos = new FileOutputStream(sConfig);
                p.setProperty("model", "");
                p.setProperty("modelPromptTemplate", "### Human:\n%1\n### Assistant:");
                p.setProperty("cache", "");
                p.setProperty("cacheLocalIp", "");

                p.setProperty("cipherMessage", "false");
                p.setProperty("keyStorePass", "");
                p.setProperty("chatUI", "true");
                p.store(fos, "MC3D AI- configuration");
                fos.close();

            } else {
                FileInputStream fis = new FileInputStream(sConfig);
                p.load(fis);
                fis.close();
                modelFilePath = "" + p.getProperty("model");
                modelPromptTemplate = "" + p.getProperty("modelPromptTemplate");
                cacheName = "" + p.getProperty("cache");
                cacheLocalIp = "" + p.getProperty("cacheLocalIp");
                cipherMessage = "" + p.getProperty("cipherMessage");
                keyStorePass = "" + p.getProperty("keyStorePass");

                if ((!modelFilePath.isEmpty()) && (!modelFilePath.equals("null")) && (new File(modelFilePath).exists())) {
                    String sLogMessage = "Model File Path:" + modelFilePath;
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

                } else {
                    String sLogMessage = "Please set model path and validate if the file mc3d-ai.properties exists";
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("model", "");
                    needConfig = true;

                }
                if ((!modelPromptTemplate.isEmpty()) && (!modelPromptTemplate.equals("null"))) {
                    String sLogMessage = "modelPromptTemplate:" + modelPromptTemplate;
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

                } else {
                    String sLogMessage = "Please set model prompt template and validate if the file  mc3d-ai.properties exists (ie: ### Human:\n%1\n### Assistant:)";
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("modelPromptTemplate", "### Human:\n%1\n### Assistant:");
                    needConfig = true;

                }

                if ((!cacheName.isEmpty()) && (!cacheName.equals("null"))) {
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Cache name:" + cacheName);

                } else {
                    String sLogMessage = "Please set Cache name in mc3d-ai.properties";
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("cache", "");
                    needConfig = true;
                }
                if ((!cacheLocalIp.isEmpty()) && (!cacheLocalIp.equals("null"))) {
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Cache Local Ip:" + cacheLocalIp);
                } else {
                    p.setProperty("cacheLocalIp", "");
                    String sLogMessage = "Please set Cache Local IP in mc3d-ai.properties";
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    needConfig = true;
                }
                if ((!cipherMessage.isEmpty()) && (!cipherMessage.equals("null"))) {
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Cipher Message:" + cipherMessage);
                } else {
                    String sLogMessage = "Please set cipherMessage in mc3d-ai.properties";
                    logTextArea.appendText(sLogMessage + "\n");
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("cipherMessage", "false");
                }
                if (needConfig) {
                    FileOutputStream fos = new FileOutputStream(sConfig);
                    p.store(fos, "MC3D AI- configuration");
                    fos.close();
                }
            }
        } catch (Exception ex) {
            logTextArea.appendText("TimerService, Error: " + ex.toString() + "\n");

            Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        String os = System.getProperty("os.name");
        String gpt4allVersion = "linux-2.7.1";
        if (os.toLowerCase().contains("win")) {
            gpt4allVersion = "windows-2.7.0";
        }

        Logger.getLogger(LLModel.class.getName()).log(Level.INFO, "Java bindings for gpt4all version: " + gpt4allVersion);
        System.out.println("Sistema Operativo: " + os);
        libraryPath = getLibraryPath(gpt4allVersion);
        System.out.println("needConfig: " + needConfig);
        if (!needConfig) {
            try {

                IgniteConfiguration igniteCfg = new IgniteConfiguration();
                igniteCfg.setLocalHost(cacheLocalIp);
                ignite = Ignition.start(igniteCfg);
                CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>();
                cacheCfg.setName(cacheName);
                cacheCfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
                cacheCfg.setCacheMode(CacheMode.REPLICATED);
                igniteCache = ignite.getOrCreateCache(cacheCfg);
                Iterator iCacheNames = ignite.cacheNames().iterator();
                while (iCacheNames.hasNext()) {
                    System.out.println("Cache List:" + iCacheNames.next());
                }

            } catch (Exception e) {
                e.printStackTrace();
                ignite.close();
                igniteControl.setSelected(false);
                igniteControl.setDisable(false);
                logTextArea.appendText("TimerService, Cluster stopped\n");

            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    // Código que se ejecutará en el hilo de la interfaz de usuario
                    igniteControl.setText("Cluster Runnning");
                    igniteControl.setSelected(true);
                    BackgroundFill backgroundFill = new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY);
                    Background background = new Background(backgroundFill);
                    igniteControl.setBackground(background);
                    igniteControl.setDisable(false);

                }
            });
            Path path = Paths.get(modelFilePath);

            // Ejemplo de cómo puedes realizar acciones específicas según el sistema operativo
            LLModel model = new LLModel(gpt4allVersion, path, libraryPath,32768);
            model.llmodel_gpu_init_gpu_device_by_int(2);
            LLModel.GenerationConfig config = LLModel.config().withNPredict(2048).build();
            UnitaryProcessSuscribeFile unitaryProcessFileWriter = new UnitaryProcessSuscribeFile(locationStart, logTextArea, igniteCache, cacheName, model, config, modelPromptTemplate, cipherMessage, keyStorePass);
            unitaryProcessFileWriter.start();
            logTextArea.appendText("TimerService, Cluster running\n");
            long diferenceCluster = System.currentTimeMillis();
            while (true) {

                if ((System.currentTimeMillis() - diferenceCluster >= 500)) {
                    Iterable< ClusterNode> nodosConectados = ignite.cluster().forCacheNodes(cacheName).nodes();
                    int iLine = 0;
                    for (ClusterNode nodo : nodosConectados) {
                        String id = "" + nodo.id();
                        if (id.equals("" + ignite.cluster().localNode().id())) {
                            Properties nodeInfo = new Properties();
                            nodeInfo.setProperty("modelName", modelFilePath);
                            igniteCache.put("services_" + id, nodeInfo);
                        }
                        String ip = "" + nodo.addresses();

                        String lastAccess = "" + System.currentTimeMillis();
                        if (igniteCache.get("services_" + id) != null) {
                            String modelName = "" + ((Properties) igniteCache.get("services_" + id)).getProperty("modelName");

                            boolean existeFila = data.stream().anyMatch(row -> row.getName().equals(id) && row.getIp().equals(ip));
                            if (!existeFila) {
                                data.add(new FXChat.StatusDataRow(id, ip, modelName, lastAccess));
                            } else {
                                long diferenceGrid = Long.parseLong(lastAccess) - (Long.parseLong("" + data.get(iLine).getLastAccess()));
                                if (diferenceGrid >= 1000) {
                                    data.set(iLine, new FXChat.StatusDataRow(id, ip, modelName, lastAccess));
                                }
                            }
                        }
                        iLine++;
                    }
                    int iData = data.size();
                    for (int i = 0; i < iData; i++) {
                        long lLastAccess = Long.parseLong(data.get(i).getLastAccess());
                        long now = System.currentTimeMillis();
                        if ((now - lLastAccess) > 5000) {
                            data.remove(i);
                        }
                    }
                    diferenceCluster = System.currentTimeMillis();
                }
                try {
                    Long start = System.currentTimeMillis();
                    UnitaryProcessPublishFile unitaryProcessFileReader = new UnitaryProcessPublishFile(locationStart, logTextArea, igniteCache, cacheName, hostname, modelFilePath, cipherMessage, keyStorePass);
                    unitaryProcessFileReader.start();
                    Long end = System.currentTimeMillis();
                    // p.setProperty("processTime", "" + (end - start));
                    // p.setProperty("lastAccess", "" + start);

                    Thread.sleep(50);
                } catch (Exception e) {

                }
                if (!igniteControl.isSelected()) {
                    break;
                }
            }
        } else {
            logTextArea.appendText("TimerService, Cluster not running\n");
        }
    }

    public String getLibraryPath(String gpt4allVersion) {
        String result = "";
        try {

            String classpath = System.getProperty("java.class.path");
            System.out.println("TimerService Classpath: " + classpath);
            System.out.println("TimerService locationStart: " + locationStart);

            if (!classpath.equals("MeshGrid_GPT.jar")) {
                if (!locationStart.contains("/dist")) {
                    locationStart = locationStart + "/dist";
                }
            }

            String sGPT4All = "gpt4all-" + gpt4allVersion;
            String destinationDirectory = locationStart + "/lib";
            if (destinationDirectory.contains("\\")) {
                destinationDirectory = destinationDirectory.replaceAll("\\\\", "/");
            }
            if (!new File(destinationDirectory + "/" + sGPT4All).exists()) {
                String archivoZip = destinationDirectory + "/" + sGPT4All + ".zip";
                descomprimirArchivoZip(archivoZip, destinationDirectory + "/" + sGPT4All);
            }
            result = destinationDirectory + "/" + sGPT4All;
        } catch (Exception ex) {
            Logger.getLogger(FXChat.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Library Path: " + result);

        return result;
    }

    public void stop() {
        try {
            ignite.close();
        } catch (Exception e) {

        }
        while (data.size() > 0) {
            data.remove(0);
        }
        thread = null;
        Thread.interrupted();
        logTextArea.appendText("TimerService, Cluster not running\n");

    }

}
