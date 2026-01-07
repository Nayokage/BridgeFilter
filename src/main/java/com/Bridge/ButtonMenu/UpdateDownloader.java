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
                File[] modFiles = modsDir.listFiles((dir, name) -> 
                    name.startsWith("BridgeFilter") && name.endsWith(".jar") && !name.contains(".tmp"));
                if (modFiles != null) {
                    for (File modFile : modFiles) {
                        oldFiles.add(modFile.getName());
                    }
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
                
                // Имя файла для скачивания
                String fileName = "BridgeFilter-" + updateInfo.version + ".jar";
                File tempFile = new File(modsDir, fileName + ".tmp");
                File finalFile = new File(modsDir, fileName);
                
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
                            errorMsg += " (файл не найден). Проверьте:\n" +
                                       "1. Имя файла в релизе должно быть: BridgeFilter-" + updateInfo.version + ".jar\n" +
                                       "2. Тег релиза должен совпадать с версией в update.json\n" +
                                       "3. URL: " + updateInfo.downloadUrl;
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
                
                downloadStatus = "Устанавливаем обновление...";
                
                // Удаляем старую версию мода если есть
                File[] modFiles = modsDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.startsWith("bridgefilter") && lowerName.endsWith(".jar") && 
                           !name.equals(fileName) && !name.endsWith(".tmp");
                });
                
                if (modFiles != null && modFiles.length > 0) {
                    downloadStatus = "Удаляем старые версии...";
                    for (File oldMod : modFiles) {
                        try {
                            // Пытаемся удалить файл несколько раз (на случай если он заблокирован)
                            boolean deleted = false;
                            
                            // Сначала пробуем обычное удаление
                            for (int i = 0; i < 10 && !deleted; i++) {
                                deleted = oldMod.delete();
                                if (!deleted) {
                                    Thread.sleep(300); // Ждем 300мс перед следующей попыткой
                                }
                            }
                            
                            // Если не получилось, пробуем пометить для удаления при следующем запуске
                            if (!deleted) {
                                try {
                                    // Пытаемся переименовать файл, чтобы пометить для удаления
                                    File deleteMarker = new File(oldMod.getParent(), oldMod.getName() + ".delete");
                                    if (oldMod.renameTo(deleteMarker)) {
                                        // Если переименование успешно, пробуем удалить переименованный файл
                                        for (int i = 0; i < 5 && !deleted; i++) {
                                            deleted = deleteMarker.delete();
                                            if (!deleted) {
                                                Thread.sleep(200);
                                            }
                                        }
                                        if (deleted) {
                                            System.out.println("[Bridge Filter] Удален старый файл (через переименование): " + oldMod.getName());
                                        } else {
                                            // Файл останется с расширением .delete - его можно удалить вручную или при следующем запуске
                                            System.out.println("[Bridge Filter] Старый файл помечен для удаления: " + deleteMarker.getName());
                                        }
                                    }
                                } catch (Exception e2) {
                                    System.err.println("[Bridge Filter] Ошибка при переименовании файла: " + e2.getMessage());
                                }
                            }
                            
                            if (deleted) {
                                System.out.println("[Bridge Filter] Удален старый файл: " + oldMod.getName());
                            } else {
                                System.err.println("[Bridge Filter] Не удалось удалить старую версию: " + oldMod.getName() + 
                                                 " (файл заблокирован игрой, удалите вручную после закрытия игры)");
                            }
                        } catch (Exception e) {
                            System.err.println("[Bridge Filter] Ошибка при удалении старой версии " + oldMod.getName() + ": " + e.getMessage());
                        }
                    }
                }
                
                // Также удаляем файлы с расширением .delete (оставшиеся с предыдущих попыток)
                File[] deleteMarkers = modsDir.listFiles((dir, name) -> name.endsWith(".delete"));
                if (deleteMarkers != null) {
                    for (File marker : deleteMarkers) {
                        try {
                            marker.delete();
                        } catch (Exception e) {
                            // Игнорируем ошибки при удалении маркеров
                        }
                    }
                }
                
                // Переименовываем временный файл в финальный
                Path tempPath = tempFile.toPath();
                Path finalPath = finalFile.toPath();
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                
                downloadProgress = 1.0f;
                downloadStatus = "Обновление установлено!";
                
                // Уведомление в чате
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §aОбновление успешно установлено!")
                        );
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §7Перезапустите игру для применения изменений.")
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
