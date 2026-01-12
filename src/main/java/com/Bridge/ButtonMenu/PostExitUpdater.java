// PostExitUpdater.java
// Простой post-exit updater для применения обновлений после закрытия Minecraft
package com.Bridge.ButtonMenu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Post-exit updater для безопасного применения обновлений после закрытия Minecraft.
 * Запускается как отдельный Java-процесс и ждет завершения Minecraft перед применением обновлений.
 */
public class PostExitUpdater {
    
    /**
     * Главный метод для запуска updater'а из командной строки.
     * Формат аргументов: <identifier> <parentPid> <operation> <source> <destination>
     * 
     * Операции:
     * - delete: удалить файл <source>
     * - move: переместить <source> -> <destination>
     * - moveAndDelete: переместить <source> -> <destination>, затем удалить <source>
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Использование: PostExitUpdater <identifier> <parentPid> <operation> <source> [destination]");
            System.err.println("Операции: delete, move, moveAndDelete");
            System.exit(1);
            return;
        }
        
        String identifier = args[0];
        String parentPidStr = args[1];
        String operation = args[2];
        String source = args[3];
        String destination = args.length > 4 ? args[4] : null;
        
        System.out.println("[PostExitUpdater] Идентификатор: " + identifier);
        System.out.println("[PostExitUpdater] PID родителя: " + parentPidStr);
        System.out.println("[PostExitUpdater] Операция: " + operation);
        System.out.println("[PostExitUpdater] Источник: " + source);
        if (destination != null) {
            System.out.println("[PostExitUpdater] Назначение: " + destination);
        }
        
        try {
            long parentPid = Long.parseLong(parentPidStr);
            
            // Ждем завершения родительского процесса (Minecraft)
            System.out.println("[PostExitUpdater] Ожидание завершения процесса Minecraft (PID: " + parentPid + ")...");
            waitForProcessExit(parentPid);
            
            // Дополнительная задержка для гарантии освобождения файлов
            System.out.println("[PostExitUpdater] Дополнительное ожидание для освобождения файлов (5 секунд)...");
            Thread.sleep(5000); // Увеличиваем задержку до 5 секунд
            
            // Проверяем, что исходный файл все еще существует перед операцией
            if (operation.equalsIgnoreCase("move") || operation.equalsIgnoreCase("moveanddelete")) {
                File sourceFile = new File(source);
                if (!sourceFile.exists()) {
                    System.err.println("[PostExitUpdater] КРИТИЧЕСКАЯ ОШИБКА: Исходный файл не существует: " + source);
                    System.err.println("[PostExitUpdater] Обновление НЕ может быть применено!");
                    System.exit(1);
                    return;
                }
                System.out.println("[PostExitUpdater] Исходный файл существует, размер: " + sourceFile.length() + " байт");
            }
            
            // Выполняем операцию
            boolean success = false;
            switch (operation.toLowerCase()) {
                case "delete":
                    success = deleteFile(source);
                    break;
                case "move":
                    if (destination == null) {
                        System.err.println("[PostExitUpdater] ОШИБКА: операция move требует destination");
                        System.exit(1);
                        return;
                    }
                    System.out.println("[PostExitUpdater] ========== ВЫПОЛНЕНИЕ ОПЕРАЦИИ MOVE ==========");
                    System.out.println("[PostExitUpdater] Откуда (source): " + source);
                    System.out.println("[PostExitUpdater] Куда (destination): " + destination);
                    
                    File sourceFileBefore = new File(source);
                    File destFileBefore = new File(destination);
                    System.out.println("[PostExitUpdater] Исходный файл существует: " + sourceFileBefore.exists() + 
                                      " (размер: " + (sourceFileBefore.exists() ? sourceFileBefore.length() : 0) + " байт)");
                    System.out.println("[PostExitUpdater] Файл назначения существует: " + destFileBefore.exists() + 
                                      " (размер: " + (destFileBefore.exists() ? destFileBefore.length() : 0) + " байт)");
                    
                    success = moveFile(source, destination);
                    
                    // Проверяем результат после операции
                    File sourceFileAfter = new File(source);
                    File destFileAfter = new File(destination);
                    System.out.println("[PostExitUpdater] После операции:");
                    System.out.println("[PostExitUpdater]   Исходный файл существует: " + sourceFileAfter.exists());
                    System.out.println("[PostExitUpdater]   Файл назначения существует: " + destFileAfter.exists() + 
                                      " (размер: " + (destFileAfter.exists() ? destFileAfter.length() : 0) + " байт)");
                    
                    if (success && destFileAfter.exists() && destFileAfter.length() > 0) {
                        System.out.println("[PostExitUpdater] ✓✓✓ ОПЕРАЦИЯ MOVE УСПЕШНА! ✓✓✓");
                    } else {
                        System.err.println("[PostExitUpdater] ✗✗✗ ОПЕРАЦИЯ MOVE НЕ УДАЛАСЬ! ✗✗✗");
                        System.err.println("[PostExitUpdater] success=" + success + 
                                          ", dest.exists=" + destFileAfter.exists() + 
                                          ", dest.size=" + (destFileAfter.exists() ? destFileAfter.length() : 0));
                        success = false;
                    }
                    break;
                case "moveanddelete":
                    if (destination == null) {
                        System.err.println("[PostExitUpdater] ОШИБКА: операция moveAndDelete требует destination");
                        System.exit(1);
                        return;
                    }
                    success = moveFile(source, destination);
                    if (success) {
                        deleteFile(source); // Пытаемся удалить источник после перемещения
                    }
                    break;
                default:
                    System.err.println("[PostExitUpdater] ОШИБКА: неизвестная операция: " + operation);
                    System.exit(1);
                    return;
            }
            
            if (success) {
                System.out.println("[PostExitUpdater] Операция успешно выполнена!");
                System.exit(0);
            } else {
                System.err.println("[PostExitUpdater] ОШИБКА: не удалось выполнить операцию");
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("[PostExitUpdater] КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Ждет завершения процесса с указанным PID.
     * Использует проверку существования процесса через Java Management API и fallback на системные команды.
     */
    private static void waitForProcessExit(long pid) throws InterruptedException {
        // Сначала пытаемся проверить через JMX (если это тот же процесс)
        String currentPid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        if (currentPid.equals(String.valueOf(pid))) {
            // Это тот же процесс, просто ждем завершения
            System.out.println("[PostExitUpdater] Это тот же процесс, завершаемся немедленно");
            return;
        }
        
        // Для других процессов используем проверку через систему
        // В Windows проверяем через tasklist, в Linux через /proc
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        
        int checkInterval = 500; // Проверяем каждые 500мс
        int maxWaitTime = 300000; // Максимум 5 минут
        int waited = 0;
        
        while (waited < maxWaitTime) {
            if (!isProcessRunning(pid, isWindows)) {
                System.out.println("[PostExitUpdater] Процесс завершен!");
                return;
            }
            
            Thread.sleep(checkInterval);
            waited += checkInterval;
            
            if (waited % 5000 == 0) {
                System.out.println("[PostExitUpdater] Ожидание... (прошло " + (waited / 1000) + " сек)");
            }
        }
        
        System.err.println("[PostExitUpdater] Таймаут ожидания процесса. Применяем обновление принудительно...");
    }
    
    /**
     * Проверяет, работает ли процесс с указанным PID.
     */
    private static boolean isProcessRunning(long pid, boolean isWindows) {
        try {
            if (isWindows) {
                // Windows: используем tasklist
                ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                int exitCode = p.waitFor();
                
                if (exitCode == 0) {
                    // Читаем вывод, чтобы проверить, есть ли процесс
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(p.getInputStream(), "CP866"));
                    String line;
                    boolean found = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(String.valueOf(pid))) {
                            found = true;
                            break;
                        }
                    }
                    return found;
                }
                return false;
            } else {
                // Linux/Unix: проверяем через /proc
                File procFile = new File("/proc/" + pid);
                return procFile.exists();
            }
        } catch (Exception e) {
            // В случае ошибки считаем, что процесс не работает (безопаснее попробовать обновить)
            return false;
        }
    }
    
    /**
     * Удаляет файл с учетом блокировки в Windows (множественные попытки).
     */
    private static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("[PostExitUpdater] Файл не существует, считаем успешным: " + filePath);
            return true;
        }
        
        // Пытаемся удалить несколько раз с задержками
        for (int i = 0; i < 10; i++) {
            if (file.delete()) {
                System.out.println("[PostExitUpdater] Файл успешно удален: " + filePath);
                return true;
            }
            
            // Пробуем переименовать (обход блокировки в Windows)
            if (i == 5) {
                try {
                    File tempFile = new File(file.getParent(), file.getName() + ".tmp_" + System.currentTimeMillis());
                    if (file.renameTo(tempFile)) {
                        if (tempFile.delete()) {
                            System.out.println("[PostExitUpdater] Файл удален через переименование: " + filePath);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки переименования
                }
            }
            
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        System.err.println("[PostExitUpdater] Не удалось удалить файл: " + filePath);
        return false;
    }
    
    /**
     * Перемещает файл с учетом блокировки в Windows.
     */
    private static boolean moveFile(String sourcePath, String destPath) {
        File source = new File(sourcePath);
        File dest = new File(destPath);
        
        if (!source.exists()) {
            System.err.println("[PostExitUpdater] Исходный файл не существует: " + sourcePath);
            System.err.println("[PostExitUpdater] Проверьте, что файл был скачан корректно");
            return false;
        }
        
        System.out.println("[PostExitUpdater] Проверяем исходный файл: " + sourcePath);
        System.out.println("[PostExitUpdater] Размер исходного файла: " + source.length() + " байт");
        
        // Если файл назначения существует, пытаемся удалить его или переименовать
        if (dest.exists()) {
            System.out.println("[PostExitUpdater] Файл назначения существует, освобождаем место: " + destPath);
            
            // Сначала пробуем переименовать старый файл (безопаснее для Windows)
            File backup = new File(dest.getParent(), dest.getName() + ".old_" + System.currentTimeMillis());
            boolean renamed = false;
            for (int i = 0; i < 5 && !renamed; i++) {
                renamed = dest.renameTo(backup);
                if (!renamed) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            
            if (renamed) {
                System.out.println("[PostExitUpdater] Старый файл переименован в: " + backup.getName());
                // Пытаемся удалить backup
                backup.deleteOnExit();
                // Пытаемся удалить сразу
                if (!backup.delete()) {
                    System.out.println("[PostExitUpdater] Backup файл будет удален при следующем запуске: " + backup.getName());
                }
            } else {
                // Если переименование не удалось, пробуем удалить напрямую
                System.out.println("[PostExitUpdater] Переименование не удалось, пробуем удалить напрямую...");
                if (!deleteFile(destPath)) {
                    System.err.println("[PostExitUpdater] Не удалось освободить место для нового файла: " + destPath);
                    System.err.println("[PostExitUpdater] Обновление НЕ будет применено!");
                    return false;
                }
            }
        }
        
        // Пытаемся переместить файл несколько раз
        for (int i = 0; i < 15; i++) {
            try {
                // Проверяем, что исходный файл все еще существует
                if (!source.exists()) {
                    System.err.println("[PostExitUpdater] Исходный файл исчез во время перемещения!");
                    // Проверяем, может файл уже на месте?
                    if (dest.exists() && dest.length() > 0) {
                        System.out.println("[PostExitUpdater] Файл уже на месте, считаем успешным");
                        return true;
                    }
                    return false;
                }
                
                // Используем Files.move для атомарной операции
                Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Проверяем результат
                if (dest.exists() && dest.length() > 0) {
                    System.out.println("[PostExitUpdater] Файл успешно перемещен: " + sourcePath + " -> " + destPath);
                    System.out.println("[PostExitUpdater] Размер нового файла: " + dest.length() + " байт");
                    return true;
                } else {
                    System.err.println("[PostExitUpdater] Файл перемещен, но назначение пустое или не существует!");
                }
            } catch (IOException e) {
                System.out.println("[PostExitUpdater] Попытка " + (i + 1) + "/15: Files.move не удалось: " + e.getMessage());
                
                // Если не удалось через Files.move, пробуем через копирование + удаление (после 5 попыток)
                if (i >= 5) {
                    try {
                        System.out.println("[PostExitUpdater] Пробуем копирование как fallback...");
                        
                        // Проверяем, что исходный файл существует
                        if (!source.exists()) {
                            System.err.println("[PostExitUpdater] Исходный файл не существует для копирования!");
                            if (dest.exists() && dest.length() > 0) {
                                System.out.println("[PostExitUpdater] Файл назначения уже существует, считаем успешным");
                                return true;
                            }
                            return false;
                        }
                        
                        // Копируем содержимое
                        long sourceSize = source.length();
                        try (FileInputStream fis = new FileInputStream(source);
                             FileOutputStream fos = new FileOutputStream(dest)) {
                            
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalCopied = 0;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                totalCopied += bytesRead;
                            }
                            fos.flush();
                        }
                        
                        // Проверяем, что файл скопирован корректно
                        if (dest.exists() && dest.length() == sourceSize && dest.length() > 0) {
                            System.out.println("[PostExitUpdater] Файл успешно скопирован: " + sourcePath + " -> " + destPath);
                            
                            // Пытаемся удалить исходный файл
                            if (source.delete()) {
                                System.out.println("[PostExitUpdater] Исходный файл удален");
                            } else {
                                System.out.println("[PostExitUpdater] Исходный файл не удален (но файл скопирован, это ОК)");
                                source.deleteOnExit();
                            }
                            
                            return true;
                        } else {
                            System.err.println("[PostExitUpdater] Размеры не совпадают! Исходный: " + sourceSize + ", новый: " + dest.length());
                        }
                    } catch (IOException e2) {
                        System.err.println("[PostExitUpdater] Ошибка при копировании: " + e2.getMessage());
                    }
                }
            }
            
            try {
                Thread.sleep(500); // Увеличиваем задержку между попытками
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        System.err.println("[PostExitUpdater] КРИТИЧЕСКАЯ ОШИБКА: Не удалось переместить файл после 15 попыток!");
        System.err.println("[PostExitUpdater] Исходный файл: " + sourcePath + " (существует: " + source.exists() + ")");
        System.err.println("[PostExitUpdater] Файл назначения: " + destPath + " (существует: " + dest.exists() + ")");
        return false;
    }
}
