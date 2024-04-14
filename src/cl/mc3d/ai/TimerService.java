/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import static cl.mc3d.ai.DescompresorZip.descomprimirArchivoZip;
import cl.mc3d.gpt4all.LLModel;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private String locationStart = System.getProperty("user.dir");
    private Console console = null;
    private boolean activeUI = false;

    public TimerService() {
        config();

        start();
        Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Starting MeshGrid file integration ");

        if (p.getProperty("chatUI").equals("true")) {
            console = new Console(this);
            console.getIgniteControl().setEnabled(false);
            activeUI = true;
            console.getLogTextArea().append("Starting MeshGrid file integration\n");
        } else {
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Chat UI disabled ");

        }
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
            if (activeUI) {
                console.getLogTextArea().append("Hostname can not be resolved\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Hostname can not be resolved");
        }
        File fConfig = new File(sConfig);

        try {
            if (!fConfig.exists()) {
                needConfig = true;
                String sStep = "Please set Model name in mc3d-ai.properties";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Model Prompt Template in mc3d-ai.properties";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Cache name in mc3d-ai.properties (if cipherMessage is setting to true, the keystore will be name to <CacheName>.pfx";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set Cache Local IP for lockup other nodes in mc3d-ai.properties";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set CipherMessage to true if you need to turn the conversation privates in the network (AES) in mc3d-ai.properties";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set keyStorePass in mc3d-ai.properties if you are set the cipherMessage to true, you can share the keystore (or the Certificate) with other machines or cluster Locations";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sStep);

                sStep = "Please set chatUI to false in mc3d-ai.properties if you need to run the Cluster without UI for integration or scaling up the Grid";
                if (activeUI) {
                    console.getLogTextArea().append(sStep + "\n");
                }
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
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

                } else {
                    String sLogMessage = "Please set model path and validate if the file mc3d-ai.properties exists";
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("model", "");
                    needConfig = true;

                }
                if ((!modelPromptTemplate.isEmpty()) && (!modelPromptTemplate.equals("null"))) {
                    String sLogMessage = "modelPromptTemplate:" + modelPromptTemplate;
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

                } else {
                    String sLogMessage = "Please set model prompt template and validate if the file  mc3d-ai.properties exists (ie: ### Human:\n%1\n### Assistant:)";
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("modelPromptTemplate", "### Human:\n%1\n### Assistant:");
                    needConfig = true;

                }

                if ((!cacheName.isEmpty()) && (!cacheName.equals("null"))) {
                    String sLogMessage = "Cache name:" + cacheName;
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

                } else {
                    String sLogMessage = "Please set Cache name in mc3d-ai.properties";
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    p.setProperty("cache", "");
                    needConfig = true;
                }
                if ((!cacheLocalIp.isEmpty()) && (!cacheLocalIp.equals("null"))) {
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Cache Local Ip:" + cacheLocalIp);
                } else {
                    p.setProperty("cacheLocalIp", "");
                    String sLogMessage = "Please set Cache Local IP in mc3d-ai.properties";
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
                    Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
                    needConfig = true;
                }
                if ((!cipherMessage.isEmpty()) && (!cipherMessage.equals("null"))) {
                    Logger.getLogger(TimerService.class.getName()).log(Level.INFO, "Cipher Message:" + cipherMessage);
                } else {
                    String sLogMessage = "Please set cipherMessage in mc3d-ai.properties";
                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }
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
            String sLogMessage = ex.toString();
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, null, sLogMessage);
        }
    }

    @Override
    public void run() {
        String os = System.getProperty("os.name");
        String gpt4allVersion = "linux-2.7.1";
        if (os.toLowerCase().contains("win")) {
            gpt4allVersion = "windows-2.7.1";
        }

        Logger.getLogger(LLModel.class.getName()).log(Level.INFO, "Java bindings for gpt4all version: " + gpt4allVersion);
        libraryPath = getLibraryPath(gpt4allVersion);
        String sLogMessage = "needConfig: " + needConfig;
        if (activeUI) {
            console.getLogTextArea().append(sLogMessage + "\n");
        }
        Logger.getLogger(LLModel.class.getName()).log(Level.INFO, sLogMessage);
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
                    sLogMessage = "Cache List:" + iCacheNames.next();

                    if (activeUI) {
                        console.getLogTextArea().append(sLogMessage + "\n");
                    }

                    System.out.println(sLogMessage);
                }

            } catch (Exception e) {
                ignite.close();
                sLogMessage = "TimerService, Cluster stopped: " + e.toString();
                if (activeUI) {
                    console.getLogTextArea().append(sLogMessage + "\n");
                }
                Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);

            }
            if (activeUI) {
                console.getIgniteControl().setBackground(Color.yellow);
            }

            Path path = Paths.get(modelFilePath);

            LLModel model = new LLModel(gpt4allVersion, path, libraryPath, 32768);
            // model.llmodel_gpu_init_gpu_device_by_int(99);

            LLModel.GenerationConfig config = LLModel.config().withNPredict(2048).build();
            UnitaryProcessSuscribeFile unitaryProcessFileWriter = new UnitaryProcessSuscribeFile(locationStart, igniteCache, cacheName, model, config, modelPromptTemplate, cipherMessage, keyStorePass);
            unitaryProcessFileWriter.start();
            sLogMessage = "TimerService, Cluster running";
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
                console.getIgniteControl().setSelected(true);
                console.getIgniteControl().setBackground(Color.green);
                console.getIgniteControl().setEnabled(true);
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

            long diferenceCluster = System.currentTimeMillis();
            while (true) {
                if (activeUI) {
                    if (!console.getIgniteControl().isSelected()) {
                        break;
                    }
               
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
                            int iGridStatus = console.getDtmGridStatus().getRowCount();
                            boolean existsRow = false;
                            for (int i = 0; i < iGridStatus; i++) {
                                if (console.getDtmGridStatus().getValueAt(i, 0).toString().equals(id) && console.getDtmGridStatus().getValueAt(i, 1).toString().equals(ip)) {
                                    existsRow = true;
                                    break;
                                }
                            }

                            if (!existsRow) {
                                Vector vector = new Vector();
                                vector.add(id);
                                vector.add(ip);
                                vector.add(modelName);
                                vector.add(lastAccess);
                                console.getDtmGridStatus().addRow(vector);
                            } else {
                                long diferenceGrid = Long.parseLong(lastAccess) - (Long.parseLong("" + console.getDtmGridStatus().getValueAt(iLine, 3).toString()));
                                if (diferenceGrid >= 1000) {
                                    console.getDtmGridStatus().setValueAt(id, iLine, 0);
                                    console.getDtmGridStatus().setValueAt(ip, iLine, 1);
                                    console.getDtmGridStatus().setValueAt(modelName, iLine, 2);
                                    console.getDtmGridStatus().setValueAt(lastAccess, iLine, 3);
                                }
                            }
                        }
                        iLine++;
                    }
                    int iData = console.getDtmGridStatus().getRowCount();
                    for (int i = 0; i < iData; i++) {
                        long lLastAccess = Long.parseLong(console.getDtmGridStatus().getValueAt(i, 3).toString());
                        long now = System.currentTimeMillis();
                        if ((now - lLastAccess) > 5000) {
                            console.getDtmGridStatus().removeRow(i);
                        }
                    }
                    diferenceCluster = System.currentTimeMillis();
                     }
                }
                try {
                    Long start = System.currentTimeMillis();
                    UnitaryProcessPublishFile unitaryProcessFileReader = new UnitaryProcessPublishFile(locationStart, igniteCache, cacheName, hostname, modelFilePath, cipherMessage, keyStorePass);
                    unitaryProcessFileReader.start();
                    Long end = System.currentTimeMillis();
                    // p.setProperty("processTime", "" + (end - start));
                    // p.setProperty("lastAccess", "" + start);

                    Thread.sleep(50);
                } catch (Exception e) {

                }
                /*if (!igniteControl) {
                    break;
                }*/
            }
        } else {
            sLogMessage = "TimerService, Cluster not running";
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

        }
    }

    public String getLibraryPath(String gpt4allVersion) {
        String result = "";
        try {

            String classpath = System.getProperty("java.class.path");
            String sLogMessage = "TimerService Classpath: " + classpath;
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);
            sLogMessage = "TimerService locationStart: " + locationStart;
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

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
            String sLogMessage = ex.toString();
            if (activeUI) {
                console.getLogTextArea().append(sLogMessage + "\n");
            }
            Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);
        }
        String sLogMessage = "Library Path: " + result;
        if (activeUI) {
            console.getLogTextArea().append(sLogMessage + "\n");
        }
        Logger.getLogger(TimerService.class.getName()).log(Level.INFO, sLogMessage);

        return result;
    }

    public void stop() {
        try {
            ignite.close();
        } catch (Exception e) {

        }
        while (console.getDtmGridStatus().getRowCount() > 0) {
            console.getDtmGridStatus().removeRow(0);
        }
        thread = null;
        Thread.interrupted();
        String sLogMessage = "TimerService, Cluster not running";
        if (activeUI) {
            console.getLogTextArea().append(sLogMessage + "\n");
        }
        Logger.getLogger(TimerService.class.getName()).log(Level.SEVERE, sLogMessage);
    }

    public static void main(String[] args) {
        TimerService timerService = new TimerService();
    }

}
