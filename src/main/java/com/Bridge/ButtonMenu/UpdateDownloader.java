// UpdateDownloader.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UpdateDownloader {
    
    public static boolean downloading = false;
    public static float downloadProgress = 0.0f;
    public static String downloadStatus = "";
    
    /**
     * Получает список старых .jar файлов мода, которые будут удалены
     */
    public static java.util.List<String> getOldModFiles() {
        java.util.List<String> oldFiles = new java.util.ArrayList<>();
        try {
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File modsDir = new File(mcDir, "mods");
            if (modsDir.exists()) {
                // Ищем только BridgeFilter.jar (фиксированное имя)
                File modFile = new File(modsDir, "BridgeFilter.jar");
                if (modFile.exists() && !modFile.getName().contains(".tmp")) {
                    oldFiles.add(modFile.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при поиске старых файлов: " + e.getMessage());
        }
        return oldFiles;
    }
    
    public static void downloadUpdate(UpdateChecker.UpdateInfo updateInfo, Runnable onComplete) {
        if (downloading) return;
        
        downloading = true;
        downloadProgress = 0.0f;
        downloadStatus = "Начинаем загрузку...";
        
        new Thread(() -> {
            try {
                // Получаем путь к папке mods
                File mcDir = Minecraft.getMinecraft().mcDataDir;
                File modsDir = new File(mcDir, "mods");
                if (!modsDir.exists()) {
                    modsDir.mkdirs();
                }
                
                // Создаем папку updates для новых версий
                File updatesDir = new File(modsDir, "updates");
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs();
                }
                
                // Фиксированное имя файла - всегда BridgeFilter.jar
                // Скачиваем в папку updates, чтобы не блокировать текущий мод
                String fileName = "BridgeFilter.jar";
                File tempFile = new File(updatesDir, fileName + ".tmp");
                File updateFile = new File(updatesDir, fileName); // Файл в updates (будет применен при следующем запуске)
                
                // Удаляем старый временный файл если есть
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                
                downloadStatus = "Подключаемся к серверу...";
                
                // Скачиваем файл (с поддержкой редиректов)
                URL url = new URL(updateInfo.downloadUrl);
                HttpURLConnection conn = null;
                int redirectCount = 0;
                int maxRedirects = 5;
                
                while (redirectCount < maxRedirects) {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(30000);
                    conn.setRequestProperty("User-Agent", "BridgeFilter-Mod/1.0.7");
                    conn.setInstanceFollowRedirects(false); // Обрабатываем редиректы вручную
                    
                    int responseCode = conn.getResponseCode();
                    
                    // Обрабатываем редиректы (301, 302, 307, 308)
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 307 || responseCode == 308) {
                        String location = conn.getHeaderField("Location");
                        if (location != null && !location.isEmpty()) {
                            url = new URL(location);
                            redirectCount++;
                            System.out.println("[Bridge Filter] Редирект " + redirectCount + " на: " + location);
                            continue;
                        }
                    }
                    
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        String errorMsg = "HTTP код ошибки: " + responseCode;
                        if (responseCode == 404) {
                            errorMsg += " (файл не найден). Проверьте URL: " + updateInfo.downloadUrl;
                        }
                        throw new IOException(errorMsg);
                    }
                    
                    // Успешно получили ответ
                    break;
                }
                
                if (redirectCount >= maxRedirects) {
                    throw new IOException("Слишком много редиректов (>" + maxRedirects + ")");
                }
                
                long fileSize = conn.getContentLengthLong();
                downloadStatus = "Загружаем файл...";
                
                try (InputStream inputStream = conn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(tempFile);
                     BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                     BufferedOutputStream bufferedOutput = new BufferedOutputStream(outputStream)) {
                    
                    byte[] buffer = new byte[8192];
                    long totalBytesRead = 0;
                    int bytesRead;
                    
                    while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                        bufferedOutput.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        if (fileSize > 0) {
                            downloadProgress = (float) totalBytesRead / fileSize;
                            downloadStatus = String.format("Загружено: %.1f%%", downloadProgress * 100);
                        }
                    }
                    
                    bufferedOutput.flush();
                }
                
                downloadStatus = "Сохраняем обновление...";
                
                // Переименовываем временный файл в финальный BridgeFilter.jar в папке updates
                // Файл будет применен при следующем запуске игры
                Path tempPath = tempFile.toPath();
                Path updatePath = updateFile.toPath();
                Files.move(tempPath, updatePath, StandardCopyOption.REPLACE_EXISTING);
                
                downloadProgress = 1.0f;
                downloadStatus = "Обновление установлено!";
                
                // Уведомление в чате
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §aОбновление загружено!")
                        );
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §7Новая версия будет применена при следующем запуске игры.")
                        );
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
                
            } catch (Exception e) {
                downloadStatus = "Ошибка: " + e.getMessage();
                System.err.println("[Bridge Filter] Ошибка при загрузке обновления: " + e.getMessage());
                e.printStackTrace();
                
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §cОшибка при загрузке обновления: §7" + e.getMessage())
                        );
                    }
                });
            } finally {
                downloading = false;
            }
        }).start();
    }
    
    public static void openReleasesPage(UpdateChecker.UpdateInfo updateInfo) {
        try {
            String url = updateInfo.releaseUrl != null ? updateInfo.releaseUrl : 
                        "https://github.com/Nayokage/BridgeFilter/releases";
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Не удалось открыть браузер: " + e.getMessage());
        }
    }
}
