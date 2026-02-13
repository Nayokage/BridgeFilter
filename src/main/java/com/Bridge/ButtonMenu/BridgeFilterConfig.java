package com.Bridge.ButtonMenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BridgeFilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("BridgeFilter.json");
    
    public boolean filterEnabled = true;
    public boolean redHighlight = false;
    public String selectedBot = "etobridge";
    public List<String> blockList = new ArrayList<>();
    public boolean nickHighlightEnabled = true;
    public String nickHighlightColor = "yellow";
    public boolean guildBridgeFormatEnabled = true;
    
    private static BridgeFilterConfig instance;
    
    public static BridgeFilterConfig getInstance() {
        if (instance == null) {
            instance = new BridgeFilterConfig();
        }
        return instance;
    }
    
    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
                BridgeFilterConfig config = GSON.fromJson(json, BridgeFilterConfig.class);
                if (config != null) {
                    instance = config;
                    if (instance.blockList == null) {
                        instance.blockList = new ArrayList<>();
                    }
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Create default config
        instance = new BridgeFilterConfig();
        save();
    }
    
    public static void save() {
        new Thread(() -> {
            try {
                saveSync();
            } catch (Exception e) {
                System.err.println("Failed to save config: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void saveSync() {
        try {
            if (instance == null) {
                return;
            }
            
            // Atomic save through temporary file
            Path tempFile = CONFIG_FILE.resolveSibling(CONFIG_FILE.getFileName().toString() + ".tmp");
            String json = GSON.toJson(instance);
            
            // Ensure parent directory exists
            Files.createDirectories(CONFIG_FILE.getParent());
            
            // Write to temp file
            Files.writeString(tempFile, json, StandardCharsets.UTF_8);
            
            // Replace original file
            Files.move(tempFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: try direct write
            try {
                String json = GSON.toJson(instance);
                Files.createDirectories(CONFIG_FILE.getParent());
                Files.writeString(CONFIG_FILE, json, StandardCharsets.UTF_8);
            } catch (Exception e2) {
                System.err.println("Failed to save config (fallback): " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }
    
    public synchronized List<String> getBlockList() {
        return new ArrayList<>(blockList);
    }
    
    public synchronized void setBlockList(List<String> list) {
        this.blockList = new ArrayList<>(list != null ? list : new ArrayList<>());
    }
    
    public synchronized void addToBlockList(String item) {
        if (item != null && !item.isEmpty() && !blockList.contains(item)) {
            blockList.add(item);
        }
    }
    
    public synchronized void removeFromBlockList(String item) {
        blockList.remove(item);
    }
}
