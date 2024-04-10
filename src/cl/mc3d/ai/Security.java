/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cl.mc3d.ai;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author maste
 */
public class Security {

    public static void encryptFile(String key, String inputFile, String outputFile) throws Exception {
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        Path inputPath = Paths.get(inputFile);
        byte[] fileBytes = Files.readAllBytes(inputPath);
        byte[] encryptedBytes = cipher.doFinal(fileBytes);

        Path outputPath = Paths.get(outputFile);
        Files.write(outputPath, Base64.getEncoder().encode(encryptedBytes), StandardOpenOption.CREATE);
        System.out.println("Archivo cifrado exitosamente.");
    }

    public static String encryptText(String key, String text) throws Exception {
        String outputText = "";
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(text.getBytes());
        outputText = new String(Base64.getEncoder().encode(encryptedBytes));
        System.out.println("Archivo cifrado exitosamente.");
        return outputText;
    }

    public static String decryptText(String key, String text) throws Exception {
        String outputText = "";
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(text.getBytes()));
        outputText = new String(decryptedBytes);
        System.out.println("Archivo descifrado exitosamente.");
        return outputText;
    }

    public static void decryptFile(String key, String inputFile, String outputFile) throws Exception {
        Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        Path inputPath = Paths.get(inputFile);
        byte[] encryptedBytes = Base64.getDecoder().decode(Files.readAllBytes(inputPath));
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        Path outputPath = Paths.get(outputFile);
        Files.write(outputPath, decryptedBytes, StandardOpenOption.CREATE);
        System.out.println("Archivo descifrado exitosamente.");
    }

    public static String getPublicKeyFromKeystore(String keystoreFile, String keystorePassword, String alias, String keyStoreType)
            throws Exception {
        KeyStore keystore = KeyStore.getInstance(keyStoreType.toUpperCase());

        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            keystore.load(fis, keystorePassword.toCharArray());
        }

        Key key = keystore.getKey(alias, keystorePassword.toCharArray());
        PublicKey pKey = keystore.getCertificate(alias).getPublicKey();

        return Base64.getEncoder().encodeToString(pKey.getEncoded());

    }

 /*   public static void main(String[] args) {

        try {
            String cache = "mc3d";
            String storeType = "PKCS12";
            String extensionStore = "pfx";

            String keystoreFile = cache + "." + extensionStore; // Cambia esto por la ruta de tu archivo PKCS12
            String keystorePassword = "123456"; // Cambia esto por la contraseña de tu keystore
            String alias = cache; // Cambia esto por el alias que quieres consultar

            String publicKey = getPublicKeyFromKeystore(keystoreFile, keystorePassword, alias, storeType);

            System.out.println("Clave pública del alias '" + alias + "': publicKey lenght: " + publicKey.length() + ", publicKey: " + publicKey);
            String clave = publicKey;
            if (clave.lastIndexOf("==") != -1) {
                clave = clave.substring(0, clave.lastIndexOf("=="));
                clave = clave.substring(clave.length() - 32, clave.length());
            }
            System.out.println("Clave AES '" + alias + "': Clave AES: " + clave);

            String inputFile = "archivo.txt";
            String outputFile = "archivo_cifrado-" + System.currentTimeMillis() + ".txt";

            // Cifrar el archivo
            encryptFile(clave, inputFile, outputFile);

            // Decifrar el archivo (solo para verificar)
            String decryptedFile = "archivo_descifrado.txt";
            decryptFile(clave, outputFile, decryptedFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
