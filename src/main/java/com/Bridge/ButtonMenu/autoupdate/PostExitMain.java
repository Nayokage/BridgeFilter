package com.Bridge.ButtonMenu.autoupdate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Post-exit updater для применения обновления мода после закрытия Minecraft.
 * 
 * Формат аргументов:
 *   <parentPid> <oldModPath> <newModPath>
 * 
 * Логика:
 * 1. Ждет завершения процесса с PID = parentPid
 * 2. Thread.sleep(3000-5000) для гарантированного освобождения файлов
 * 3. Удаляет старый .jar файл
 * 4. Переименовывает новый .jar файл в старый
 */
public class PostExitMain {

    public static void main(String[] args) throws Exception {
        // Используем абсолютные пути через System.getProperty("user.dir")
        String userDir = System.getProperty("user.dir");
        File logDir = new File(userDir, ".autoupdates");
        logDir.mkdirs();
        File logFile = new File(logDir, "postexit.log");
        
        PrintStream ps = new PrintStream(new FileOutputStream(logFile, true), true, "UTF-8");
        System.setOut(ps);
        System.setErr(ps);
        
        System.out.println("========================================");
        System.out.println("Post-exit updater started");
        System.out.println("User dir: " + userDir);
        System.out.println("Args count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("  args[" + i + "] = " + args[i]);
        }
        System.out.println("========================================");
        
        if (args.length < 3) {
            System.out.println("ERROR: Usage: <parentPid> <oldModPath> <newModPath>");
            System.out.println("  parentPid  - PID процесса Minecraft (будет ожидаться)");
            System.out.println("  oldModPath - путь к старому .jar файлу (будет удален)");
            System.out.println("  newModPath - путь к новому .jar файлу (будет переименован в oldModPath)");
            return;
        }
        
        long parentPid = -1L;
        try {
            parentPid = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: First argument must be parent PID, got: " + args[0]);
            return;
        }
        
        String oldModPath = args[1];
        String newModPath = args[2];
        
        // Преобразуем в абсолютные пути
        File oldModFile = new File(oldModPath).getAbsoluteFile();
        File newModFile = new File(newModPath).getAbsoluteFile();
        
        System.out.println("Parent PID: " + parentPid);
        System.out.println("Old mod file: " + oldModFile.getAbsolutePath());
        System.out.println("New mod file: " + newModFile.getAbsolutePath());
        
        // Шаг 1: Ждем завершения процесса Minecraft
        System.out.println("Step 1: Waiting for parent process (PID=" + parentPid + ") to exit...");
        waitForProcessExit(parentPid);
        System.out.println("Parent process exited.");
        
        // Шаг 2: Thread.sleep(3000-5000) для гарантированного освобождения файлов
        int sleepTime = 4000; // 4 секунды (в диапазоне 3000-5000)
        System.out.println("Step 2: Sleeping " + sleepTime + " ms to ensure files are unlocked...");
        Thread.sleep(sleepTime);
        System.out.println("Sleep completed.");
        
        // Шаг 3: Удаляем старый .jar файл
        System.out.println("Step 3: Deleting old mod file...");
        if (oldModFile.exists()) {
            // Пытаемся удалить файл с повторными попытками
            boolean deleted = false;
            for (int attempt = 1; attempt <= 10; attempt++) {
                if (oldModFile.delete()) {
                    deleted = true;
                    System.out.println("Old mod file deleted successfully: " + oldModFile.getAbsolutePath());
                    break;
                } else {
                    System.out.println("Attempt " + attempt + ": Failed to delete old mod file, retrying in 1 second...");
                    Thread.sleep(1000);
                }
            }
            if (!deleted) {
                System.out.println("WARNING: Failed to delete old mod file after 10 attempts: " + oldModFile.getAbsolutePath());
                System.out.println("Continuing with rename operation...");
            }
        } else {
            System.out.println("Old mod file does not exist (already deleted?): " + oldModFile.getAbsolutePath());
        }
        
        // Шаг 4: Переименовываем новый .jar файл в старый
        System.out.println("Step 4: Renaming new mod file to old mod file name...");
        if (!newModFile.exists()) {
            System.out.println("ERROR: New mod file does not exist: " + newModFile.getAbsolutePath());
            return;
        }
        
        // Пытаемся переименовать с повторными попытками
        boolean renamed = false;
        for (int attempt = 1; attempt <= 10; attempt++) {
            if (newModFile.renameTo(oldModFile)) {
                renamed = true;
                System.out.println("New mod file renamed successfully: " + newModFile.getAbsolutePath() + " -> " + oldModFile.getAbsolutePath());
                break;
            } else {
                System.out.println("Attempt " + attempt + ": Failed to rename new mod file, retrying in 1 second...");
                Thread.sleep(1000);
            }
        }
        
        if (!renamed) {
            System.out.println("ERROR: Failed to rename new mod file after 10 attempts");
            System.out.println("  From: " + newModFile.getAbsolutePath());
            System.out.println("  To:   " + oldModFile.getAbsolutePath());
            return;
        }
        
        System.out.println("========================================");
        System.out.println("Update completed successfully!");
        System.out.println("  Old file: DELETED");
        System.out.println("  New file: RENAMED to " + oldModFile.getAbsolutePath());
        System.out.println("========================================");
    }
    
    /**
     * Ждёт завершения процесса с указанным PID.
     * На Windows использует tasklist, на Unix-подобных системах проверяет /proc.
     */
    private static void waitForProcessExit(long pid) throws InterruptedException {
        if (pid <= 0) {
            System.out.println("WARNING: Invalid PID, skipping wait");
            return;
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");
        
        System.out.println("OS: " + os + " (Windows: " + isWindows + ")");
        
        int checkCount = 0;
        while (isProcessRunning(pid, isWindows)) {
            checkCount++;
            if (checkCount % 10 == 0) {
                System.out.println("Still waiting for process (PID=" + pid + ") to exit... (checked " + checkCount + " times)");
            }
            Thread.sleep(1000);
        }
        
        System.out.println("Process (PID=" + pid + ") has exited (checked " + checkCount + " times)");
    }
    
    private static boolean isProcessRunning(long pid, boolean isWindows) {
        try {
            if (isWindows) {
                ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                java.io.BufferedReader reader =
                    new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), "CP866"));
                String line;
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(String.valueOf(pid))) {
                        found = true;
                        break;
                    }
                }
                p.waitFor();
                return found;
            } else {
                return new File("/proc/" + pid).exists();
            }
        } catch (Exception e) {
            // В случае ошибки считаем, что процесса нет, чтобы не блокировать обновление
            System.out.println("WARNING: Error checking process status: " + e.getMessage());
            return false;
        }
    }
}
