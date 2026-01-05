// UpdateChecker.java
package com.Bridge.ButtonMenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    
    private static final String UPDATE_JSON_URL = "https://raw.githubusercontent.com/Nayokage/BridgeFilter/main/update.json";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Nayokage/BridgeFilter/releases/latest";
    private static final String CURRENT_VERSION = "1.0.0";
    
    public static UpdateInfo latestUpdate = null;
    public static boolean updateAvailable = false;
    public static boolean checking = false;
    
    public static class UpdateInfo {
        public String version;
        public String downloadUrl;
        public String changelog;
        public String releaseUrl;
        
        public UpdateInfo(String version, String downloadUrl, String changelog, String releaseUrl) {
            this.version = version;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
            this.releaseUrl = releaseUrl;
        }
    }
    
    public static void checkForUpdates() {
        if (checking) return;
        checking = true;
        
        new Thread(() -> {
            try {
                // Сначала пробуем через update.json
                UpdateInfo info = checkViaUpdateJson();
                if (info == null) {
                    // Если не получилось, пробуем через GitHub API
                    info = checkViaGitHubAPI();
                }
                
                if (info != null && isNewerVersion(info.version, CURRENT_VERSION)) {
                    latestUpdate = info;
                    updateAvailable = true;
                    
                    // Показываем уведомление в чате
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText("§9[Bridge Filter] §aДоступно обновление! Версия: §e" + info.version)
                            );
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText("§9[Bridge Filter] §7Нажмите §eПравый Shift §7для открытия меню обновления")
                            );
                        }
                    });
                } else {
                    updateAvailable = false;
                }
            } catch (Exception e) {
                System.err.println("[Bridge Filter] Ошибка при проверке обновлений: " + e.getMessage());
                e.printStackTrace();
            } finally {
                checking = false;
            }
        }).start();
    }
    
    private static UpdateInfo checkViaUpdateJson() {
        try {
            URL url = new URL(UPDATE_JSON_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "BridgeFilter-Mod/1.0.0");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(response.toString()).getAsJsonObject();
            
            String latestVersion = json.getAsJsonObject("promos").get("1.8.9-latest").getAsString();
            String changelog = "";
            if (json.has("1.8.9") && json.getAsJsonObject("1.8.9").has(latestVersion)) {
                changelog = json.getAsJsonObject("1.8.9").get(latestVersion).getAsString();
            }
            
            String homepage = json.has("homepage") ? json.get("homepage").getAsString() : 
                             "https://github.com/Nayokage/BridgeFilter/releases";
            
            // Формируем URL для скачивания
            String downloadUrl = "https://github.com/Nayokage/BridgeFilter/releases/download/" + latestVersion + 
                                "/BridgeFilter-" + latestVersion + ".jar";
            
            return new UpdateInfo(latestVersion, downloadUrl, changelog, homepage);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static UpdateInfo checkViaGitHubAPI() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "BridgeFilter-Mod/1.0.0");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(response.toString()).getAsJsonObject();
            
            String version = json.get("tag_name").getAsString().replace("v", "");
            String changelog = json.has("body") ? json.get("body").getAsString() : "";
            String releaseUrl = json.has("html_url") ? json.get("html_url").getAsString() : 
                               "https://github.com/Nayokage/BridgeFilter/releases";
            
            // Ищем URL для скачивания .jar файла
            String downloadUrl = null;
            if (json.has("assets")) {
                for (var asset : json.getAsJsonArray("assets")) {
                    JsonObject assetObj = asset.getAsJsonObject();
                    String name = assetObj.get("name").getAsString();
                    if (name.endsWith(".jar") && name.contains("BridgeFilter")) {
                        downloadUrl = assetObj.get("browser_download_url").getAsString();
                        break;
                    }
                }
            }
            
            // Если не нашли в assets, формируем стандартный URL
            if (downloadUrl == null) {
                downloadUrl = "https://github.com/Nayokage/BridgeFilter/releases/download/" + 
                             json.get("tag_name").getAsString() + "/BridgeFilter-" + version + ".jar";
            }
            
            return new UpdateInfo(version, downloadUrl, changelog, releaseUrl);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int maxLength = Math.max(newParts.length, currentParts.length);
            for (int i = 0; i < maxLength; i++) {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
