// UpdateDownloader.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class UpdateDownloader {
    
    public static boolean downloading = false;
    public static float downloadProgress = 0.0f;
    public static String downloadStatus = "";
    
    // Статические переменные для post-exit updater
    private static File pendingOldModFile = null;
    private static File pendingNewModFile = null;
    private static boolean shutdownHookRegistered = false;
    
    /**
     * Получает список старых .jar файлов мода, которые будут удалены
     */
    public static java.util.List<String> getOldModFiles() {
        java.util.List<String> oldFiles = new java.util.ArrayList<>();
        try {
            File mcDir = Minecraft.getMinecraft().mcDataDir;
            File modsDir = new File(mcDir, "mods");
            if (modsDir.exists()) {
                // Ищем все BridgeFilter-*.jar файлы (кроме BridgeFilter-new.jar и временных)
                File[] bridgeFilterFiles = modsDir.listFiles((dir, name) -> 
                    name.startsWith("BridgeFilter-") && name.endsWith(".jar") && 
                    !name.equals("BridgeFilter-new.jar") && !name.contains(".tmp") && !name.contains(".old"));
                
                if (bridgeFilterFiles != null) {
                    for (File file : bridgeFilterFiles) {
                        oldFiles.add(file.getName());
                    }
                }
                
                // Также проверяем BridgeFilter.jar (fallback для старых версий)
                File fallbackFile = new File(modsDir, "BridgeFilter.jar");
                if (fallbackFile.exists() && !oldFiles.contains("BridgeFilter.jar")) {
                    oldFiles.add(fallbackFile.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при поиске старых файлов: " + e.getMessage());
        }
        return oldFiles;
    }
    
    /**
     * Получает PID текущего процесса Java (Minecraft)
     */
    private static long getCurrentPid() {
        try {
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            String pid = processName.split("@")[0];
            return Long.parseLong(pid);
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Не удалось получить PID: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Получает путь к Java executable из текущего процесса
     */
    private static String getJavaExecutablePath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return "java"; // Fallback на java из PATH
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        
        String javaExe = isWindows ? "java.exe" : "java";
        String javaPath = javaHome + File.separator + "bin" + File.separator + javaExe;
        
        File javaFile = new File(javaPath);
        if (javaFile.exists() && javaFile.canExecute()) {
            return javaPath;
        }
        
        return "java"; // Fallback
    }
    
    /**
     * Получает путь к текущему jar-файлу мода
     */
    private static String getCurrentModJarPath() {
        try {
            // Получаем путь к классу UpdateDownloader
            String className = UpdateDownloader.class.getName().replace('.', '/') + ".class";
            java.net.URL classUrl = UpdateDownloader.class.getClassLoader().getResource(className);
            
            if (classUrl != null) {
                String classPath = classUrl.getPath();
                if (classPath.startsWith("file:")) {
                    classPath = classPath.substring(5);
                }
                if (classPath.contains("!")) {
                    // Это jar-файл
                    classPath = classPath.substring(0, classPath.indexOf("!"));
                    // Decode URL encoding
                    classPath = java.net.URLDecoder.decode(classPath, "UTF-8");
                    // Убираем ведущий слеш для Windows
                    if (System.getProperty("os.name").toLowerCase().contains("windows") && 
                        classPath.startsWith("/") && classPath.length() > 1 && 
                        classPath.charAt(2) == ':') {
                        classPath = classPath.substring(1);
                    }
                    return classPath;
                }
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Не удалось получить путь к jar-файлу: " + e.getMessage());
        }
        return null;
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
                
                // POST-EXIT UPDATE LOGIC: Скачиваем новую версию во временный файл
                // Имя файла: BridgeFilter-new.jar (будет применен через post-exit updater)
                String newFileName = "BridgeFilter-new.jar";
                File tempFile = new File(modsDir, newFileName + ".tmp");
                File newModFile = new File(modsDir, newFileName); // Новая версия мода
                
                // Находим реальный путь к текущему jar-файлу мода
                String currentModJarPath = getCurrentModJarPath();
                File currentModFile;
                if (currentModJarPath != null) {
                    currentModFile = new File(currentModJarPath);
                    System.out.println("[Bridge Filter] Текущий mod файл найден: " + currentModFile.getAbsolutePath());
                } else {
                    // Fallback: ищем любой BridgeFilter-*.jar в папке mods
                    System.out.println("[Bridge Filter] Не удалось получить путь к jar-файлу, ищем в папке mods...");
                    File[] bridgeFilterFiles = modsDir.listFiles((dir, name) -> 
                        name.startsWith("BridgeFilter-") && name.endsWith(".jar") && !name.equals(newFileName));
                    
                    if (bridgeFilterFiles != null && bridgeFilterFiles.length > 0) {
                        // Берем первый найденный файл (обычно самый новый)
                        currentModFile = bridgeFilterFiles[0];
                        System.out.println("[Bridge Filter] Найден mod файл: " + currentModFile.getName());
                    } else {
                        // Последний fallback: BridgeFilter.jar
                        currentModFile = new File(modsDir, "BridgeFilter.jar");
                        System.out.println("[Bridge Filter] Используем fallback: BridgeFilter.jar");
                    }
                }
                
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
                    conn.setRequestProperty("User-Agent", "BridgeFilter-Mod/1.0.8");
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
                
                // POST-EXIT UPDATE LOGIC: Переименовываем временный файл в BridgeFilter-new.jar
                Path tempPath = tempFile.toPath();
                Path newModPath = newModFile.toPath();
                Files.move(tempPath, newModPath, StandardCopyOption.REPLACE_EXISTING);
                
                downloadStatus = "Настраиваем автообновление...";
                
                // POST-EXIT UPDATE LOGIC: Сохраняем пути для shutdown hook
                pendingOldModFile = currentModFile;
                pendingNewModFile = newModFile;
                
                // Регистрируем shutdown hook (только один раз)
                if (!shutdownHookRegistered) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            startPostExitUpdaterProcess(pendingOldModFile, pendingNewModFile);
                        } catch (Exception e) {
                            System.err.println("[Bridge Filter] Ошибка в shutdown hook: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }, "BridgeFilter-PostExitUpdater"));
                    shutdownHookRegistered = true;
                    System.out.println("[Bridge Filter] Shutdown hook зарегистрирован");
                }
                
                downloadProgress = 1.0f;
                
                // Уведомление в чате
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        downloadStatus = "Обновление будет применено после закрытия игры";
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §aОбновление загружено!")
                        );
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§9[Bridge Filter] §7Новая версия будет применена автоматически после закрытия игры.")
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
    
    /**
     * POST-EXIT UPDATE LOGIC: Запускает post-exit updater как отдельный процесс.
     * Вызывается из shutdown hook.
     * 
     * @param oldModFile Старый файл мода (будет удален)
     * @param newModFile Новый файл мода (будет переименован в старый)
     */
    private static void startPostExitUpdaterProcess(File oldModFile, File newModFile) {
        if (oldModFile == null || newModFile == null) {
            System.err.println("[Bridge Filter] Файлы для обновления не заданы");
            return;
        }
        
        try {
            long currentPid = getCurrentPid();
            if (currentPid == -1) {
                System.err.println("[Bridge Filter] Не удалось получить PID");
                return;
            }
            
            String javaExe = getJavaExecutablePath();
            String currentModJar = getCurrentModJarPath();
            
            if (currentModJar == null) {
                System.err.println("[Bridge Filter] Не удалось получить путь к jar-файлу");
                return;
            }
            
            // Получаем абсолютные пути через System.getProperty("user.dir")
            String userDir = System.getProperty("user.dir");
            File oldModCanonical = oldModFile.getCanonicalFile();
            File newModCanonical = newModFile.getCanonicalFile();
            String oldModPath = oldModCanonical.getAbsolutePath();
            String newModPath = newModCanonical.getAbsolutePath();
            
            // Защита: запрещаем операции, если source и destination один и тот же файл
            if (oldModPath.equalsIgnoreCase(newModPath)) {
                System.err.println("[Bridge Filter] ОШИБКА: старый и новый файлы совпадают: " + oldModPath);
                return;
            }
            
            System.out.println("[Bridge Filter] Запуск post-exit updater из shutdown hook...");
            System.out.println("[Bridge Filter] Java: " + javaExe);
            System.out.println("[Bridge Filter] Mod JAR: " + currentModJar);
            System.out.println("[Bridge Filter] Старый файл (будет удален): " + oldModPath);
            System.out.println("[Bridge Filter] Новый файл (будет переименован): " + newModPath);
            System.out.println("[Bridge Filter] PID процесса: " + currentPid);
            
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(javaExe);
            command.add("-cp");
            command.add(currentModJar);
            command.add("com.Bridge.ButtonMenu.autoupdate.PostExitMain");
            command.add(String.valueOf(currentPid));
            command.add(oldModPath);  // Старый файл (будет удален)
            command.add(newModPath);  // Новый файл (будет переименован в старый)
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(userDir));
            
            // Направляем вывод в файл
            File logDir = new File(userDir, ".autoupdates");
            logDir.mkdirs();
            File logFile = new File(logDir, "postexit.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile));
            
            // Запускаем процесс (не ждем завершения)
            Process updaterProcess = pb.start();
            System.out.println("[Bridge Filter] Post-exit updater запущен (PID: " + getProcessId(updaterProcess) + ")");
            
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Ошибка при запуске post-exit updater: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает PID процесса (для Windows и Unix)
     */
    private static long getProcessId(Process process) {
        try {
            if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                java.lang.reflect.Field field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                return field.getLong(process);
            } else if (process.getClass().getName().equals("java.lang.ProcessImpl")) {
                java.lang.reflect.Field field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                return (Long) field.get(process);
            }
        } catch (Exception e) {
            // Игнорируем ошибки рефлексии
        }
        return -1;
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
