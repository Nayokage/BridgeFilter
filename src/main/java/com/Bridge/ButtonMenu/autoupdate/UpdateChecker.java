package com.Bridge.ButtonMenu.autoupdate;

import com.Bridge.ButtonMenu.UpdateGUI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Nayokage aka fiokem/BridgeFilter/releases/latest";
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/Nayokage aka fiokem/BridgeFilter/main/update.json";
    private static final String CURRENT_VERSION = "1.0.0"; // Should match mod version
    
    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                String latestVersion = getLatestVersion();
                if (latestVersion != null && isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    String downloadUrl = getDownloadUrl(latestVersion);
                    if (downloadUrl != null) {
                        Minecraft.getInstance().execute(() -> {
                            Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§a[Bridge Filter] §fДоступно обновление: " + latestVersion),
                                false
                            );
                            Minecraft.getInstance().player.displayClientMessage(
                                Component.literal("§a[Bridge Filter] §fНажмите F3+T для открытия меню обновления"),
                                false
                            );
                            
                            // Store update info for GUI
                            UpdateInfo.updateAvailable = true;
                            UpdateInfo.latestVersion = latestVersion;
                            UpdateInfo.downloadUrl = downloadUrl;
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to check for updates: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private static String getLatestVersion() {
        try {
            // Try update.json first
            String version = getVersionFromUpdateJson();
            if (version != null) {
                return version;
            }
            
            // Fallback to GitHub API
            return getVersionFromGitHub();
        } catch (Exception e) {
            System.err.println("Failed to get latest version: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static String getVersionFromUpdateJson() {
        try {
            URL url = new URL(UPDATE_JSON_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
                
                // Get version for 1.21
                if (json.has("1.21-latest")) {
                    return json.get("1.21-latest").getAsString();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get version from update.json: " + e.getMessage());
        }
        
        return null;
    }
    
    private static String getVersionFromGitHub() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
                
                if (json.has("tag_name")) {
                    String tag = json.get("tag_name").getAsString();
                    // Remove 'v' prefix if present
                    return tag.startsWith("v") ? tag.substring(1) : tag;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get version from GitHub: " + e.getMessage());
        }
        
        return null;
    }
    
    private static String getDownloadUrl(String version) {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(response.toString(), JsonObject.class);
                
                if (json.has("assets")) {
                    var assets = json.getAsJsonArray("assets");
                    for (var asset : assets) {
                        JsonObject assetObj = asset.getAsJsonObject();
                        String name = assetObj.get("name").getAsString();
                        if (name.contains("fabric") && name.contains("1.21") && name.endsWith(".jar")) {
                            return assetObj.get("browser_download_url").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get download URL: " + e.getMessage());
        }
        
        return null;
    }
    
    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] latestParts = latest.split("\\.");
            String[] currentParts = current.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < maxLength; i++) {
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (Exception e) {
            return false;
        }
    }
    
    public static class UpdateInfo {
        public static boolean updateAvailable = false;
        public static String latestVersion = null;
        public static String downloadUrl = null;
    }
}
