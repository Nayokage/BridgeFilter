package com.Bridge.ButtonMenu.autoupdate;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdateDownloader {
    
    public static void downloadUpdate(String downloadUrl) {
        new Thread(() -> {
            try {
                File modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
                File tempFile = new File(modsDir, "BridgeFilter-new.jar.tmp");
                File finalFile = new File(modsDir, "BridgeFilter-new.jar");
                
                // Download file
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = conn.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Rename temp file to final file
                    if (tempFile.exists()) {
                        Files.move(tempFile.toPath(), finalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        
                        // Schedule post-exit updater
                        schedulePostExitUpdater();
                    }
                } else {
                    System.err.println("Failed to download update: HTTP " + conn.getResponseCode());
                }
            } catch (Exception e) {
                System.err.println("Failed to download update: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private static void schedulePostExitUpdater() {
        try {
            // Get current mod JAR path
            var modContainer = FabricLoader.getInstance().getModContainer("bridgefilter");
            if (modContainer.isEmpty()) {
                System.err.println("Failed to find bridgefilter mod container");
                return;
            }
            
            var rootPath = modContainer.get().getRootPath();
            File modJar = null;
            
            // Try to get JAR file from root path
            if (rootPath.getFileSystem().provider().getScheme().equals("jar")) {
                // Extract JAR path from jar:file:// URL
                String path = rootPath.toString();
                if (path.startsWith("jar:file:")) {
                    path = path.substring(9); // Remove "jar:file:"
                    int exclamation = path.indexOf('!');
                    if (exclamation != -1) {
                        path = path.substring(0, exclamation);
                    }
                    modJar = new File(path);
                }
            } else {
                modJar = rootPath.toFile();
            }
            
            if (modJar != null && modJar.isFile()) {
                // Create post-exit updater script
                File modsDir = FabricLoader.getInstance().getGameDir().resolve("mods").toFile();
                File newJar = new File(modsDir, "BridgeFilter-new.jar");
                File oldJar = modJar;
                
                // Store paths for post-exit updater
                final String oldJarPath = oldJar.getAbsolutePath();
                final String newJarPath = newJar.getAbsolutePath();
                
                // Register shutdown hook to run post-exit updater
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        PostExitMain.main(new String[]{
                            oldJarPath,
                            newJarPath
                        });
                    } catch (Exception e) {
                        System.err.println("Failed to run post-exit updater: " + e.getMessage());
                        e.printStackTrace();
                    }
                }));
            } else {
                System.err.println("Failed to determine mod JAR path");
            }
        } catch (Exception e) {
            System.err.println("Failed to schedule post-exit updater: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
