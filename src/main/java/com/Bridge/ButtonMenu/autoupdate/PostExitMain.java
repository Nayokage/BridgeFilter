package com.Bridge.ButtonMenu.autoupdate;

import java.io.File;

public class PostExitMain {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PostExitMain <oldJar> <newJar>");
            System.exit(1);
        }
        
        try {
            File oldJar = new File(args[0]);
            File newJar = new File(args[1]);
            
            if (!newJar.exists()) {
                System.err.println("New JAR file not found: " + newJar.getAbsolutePath());
                System.exit(1);
            }
            
            // Wait a bit to ensure Minecraft is fully closed
            Thread.sleep(2000);
            
            // Delete old JAR
            if (oldJar.exists()) {
                int attempts = 0;
                while (!oldJar.delete() && attempts < 10) {
                    Thread.sleep(500);
                    attempts++;
                }
                
                if (oldJar.exists()) {
                    System.err.println("Failed to delete old JAR: " + oldJar.getAbsolutePath());
                    // Mark for deletion on next startup
                    oldJar.renameTo(new File(oldJar.getParent(), oldJar.getName() + ".delete"));
                }
            }
            
            // Rename new JAR to final name
            File finalJar = new File(newJar.getParent(), "BridgeFilter.jar");
            if (finalJar.exists()) {
                finalJar.delete();
            }
            
            int attempts = 0;
            while (!newJar.renameTo(finalJar) && attempts < 10) {
                Thread.sleep(500);
                attempts++;
            }
            
            if (!finalJar.exists()) {
                System.err.println("Failed to rename new JAR: " + newJar.getAbsolutePath());
                System.exit(1);
            }
            
            System.out.println("Bridge Filter update applied successfully!");
        } catch (Exception e) {
            System.err.println("Failed to apply update: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
