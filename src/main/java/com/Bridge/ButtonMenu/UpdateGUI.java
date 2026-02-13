package com.Bridge.ButtonMenu;

import com.Bridge.ButtonMenu.autoupdate.UpdateChecker;
import com.Bridge.ButtonMenu.autoupdate.UpdateDownloader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdateGUI extends Screen {
    private final String latestVersion;
    private final String downloadUrl;
    
    public UpdateGUI(String latestVersion, String downloadUrl) {
        super(Component.literal("Bridge Filter Update"));
        this.latestVersion = latestVersion;
        this.downloadUrl = downloadUrl;
    }
    
    public static void open() {
        if (UpdateChecker.UpdateInfo.updateAvailable) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new UpdateGUI(
                UpdateChecker.UpdateInfo.latestVersion,
                UpdateChecker.UpdateInfo.downloadUrl
            ));
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Download button
        this.addRenderableWidget(Button.builder(
            Component.literal("Download Update"),
            button -> {
                if (downloadUrl != null) {
                    UpdateDownloader.downloadUpdate(downloadUrl);
                    this.minecraft.setScreen(null);
                }
            }
        ).bounds(centerX - 100, centerY + 20, 200, 20).build());
        
        // Open GitHub button
        this.addRenderableWidget(Button.builder(
            Component.literal("Open GitHub Releases"),
            button -> {
                try {
                    java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://github.com/Nayokage aka fiokem/BridgeFilter/releases")
                    );
                } catch (Exception e) {
                    System.err.println("Failed to open GitHub: " + e.getMessage());
                }
            }
        ).bounds(centerX - 100, centerY + 50, 200, 20).build());
        
        // Close button
        this.addRenderableWidget(Button.builder(
            Component.literal("Close"),
            button -> this.minecraft.setScreen(null)
        ).bounds(centerX - 100, centerY + 80, 200, 20).build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Title
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("Bridge Filter Update Available"),
            centerX,
            centerY - 60,
            0xFFFFFF
        );
        
        // Version info
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("Latest Version: " + latestVersion),
            centerX,
            centerY - 40,
            0xFFFFFF
        );
        
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("Current Version: 1.0.0"),
            centerX,
            centerY - 30,
            0xAAAAAA
        );
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
