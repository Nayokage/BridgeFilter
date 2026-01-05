// UpdateGUI.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class UpdateGUI extends GuiScreen {
    
    private UpdateChecker.UpdateInfo updateInfo;
    private boolean showConfirmDialog = false;
    private java.util.List<String> oldFilesToDelete;
    
    public UpdateGUI(UpdateChecker.UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
        this.oldFilesToDelete = UpdateDownloader.getOldModFiles();
    }
    
    @Override
    public void initGui() {
        buttonList.clear();
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        if (showConfirmDialog && !oldFilesToDelete.isEmpty()) {
            // Диалог подтверждения удаления старых файлов
            buttonList.add(new GuiButton(5, centerX - 105, centerY + 60, 100, 20, "§aПодтвердить"));
            buttonList.add(new GuiButton(3, centerX + 5, centerY + 60, 100, 20, "§cОтмена"));
        } else if (!UpdateDownloader.downloading) {
            // Обычное меню
            // Кнопка скачать
            buttonList.add(new GuiButton(1, centerX - 105, centerY + 40, 100, 20, "§aСкачать"));
            
            // Кнопка открыть в браузере
            buttonList.add(new GuiButton(2, centerX + 5, centerY + 40, 100, 20, "§bОткрыть GitHub"));
            
            // Кнопка закрыть
            buttonList.add(new GuiButton(3, centerX - 50, centerY + 70, 100, 20, "Закрыть"));
        } else {
            // Во время загрузки
            buttonList.add(new GuiButton(4, centerX - 50, centerY + 100, 100, 20, "§cОтменить"));
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            // Показываем диалог подтверждения если есть старые файлы
            if (!oldFilesToDelete.isEmpty() && !showConfirmDialog) {
                showConfirmDialog = true;
                initGui();
            } else {
                // Скачать обновление
                showConfirmDialog = false;
                UpdateDownloader.downloadUpdate(updateInfo, () -> {
                    mc.displayGuiScreen(null);
                });
                initGui(); // Обновляем GUI для показа прогресса
            }
        } else if (button.id == 2) {
            // Открыть в браузере
            UpdateDownloader.openReleasesPage(updateInfo);
        } else if (button.id == 3) {
            // Закрыть или Отмена
            if (showConfirmDialog) {
                showConfirmDialog = false;
                initGui();
            } else {
                mc.displayGuiScreen(null);
            }
        } else if (button.id == 5) {
            // Подтвердить удаление и скачать
            showConfirmDialog = false;
            UpdateDownloader.downloadUpdate(updateInfo, () -> {
                mc.displayGuiScreen(null);
            });
            initGui();
        } else if (button.id == 4) {
            // Отменить загрузку (не реализовано полностью, но можно закрыть GUI)
            mc.displayGuiScreen(null);
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Полупрозрачный фон
        drawRect(0, 0, width, height, 0xAA000000);
        
        // Основной фон
        int boxWidth = 400;
        int boxHeight = 200;
        int boxX = width / 2 - boxWidth / 2;
        int boxY = height / 2 - boxHeight / 2;
        
        drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF1E1E1E);
        drawRect(boxX, boxY, boxX + boxWidth, boxY + 25, 0xFF00AAFF);
        
        // Заголовок
        drawCenteredString(fontRendererObj, "§9§lДоступно обновление!", width / 2, boxY + 8, 0xFFFFFF);
        
        int textY = boxY + 40;
        
        // Текущая версия
        drawString(fontRendererObj, "§7Текущая версия: §f1.0.0", boxX + 20, textY, 0xFFFFFF);
        textY += 20;
        
        // Новая версия
        drawString(fontRendererObj, "§7Новая версия: §a" + updateInfo.version, boxX + 20, textY, 0xFFFFFF);
        textY += 30;
        
        // Диалог подтверждения удаления старых файлов
        if (showConfirmDialog && !oldFilesToDelete.isEmpty()) {
            textY += 10;
            drawString(fontRendererObj, "§cВнимание! Будут удалены старые версии:", boxX + 20, textY, 0xFF5555);
            textY += 15;
            for (int i = 0; i < Math.min(oldFilesToDelete.size(), 3); i++) {
                String fileName = oldFilesToDelete.get(i);
                if (fileName.length() > 45) {
                    fileName = fileName.substring(0, 42) + "...";
                }
                drawString(fontRendererObj, "§8• §c" + fileName, boxX + 30, textY, 0xFFAAAA);
                textY += 12;
            }
            if (oldFilesToDelete.size() > 3) {
                drawString(fontRendererObj, "§8... и ещё " + (oldFilesToDelete.size() - 3) + " файл(ов)", boxX + 30, textY, 0xFFAAAA);
                textY += 12;
            }
            textY += 5;
            drawString(fontRendererObj, "§7Продолжить установку?", boxX + 20, textY, 0xCCCCCC);
        }
        
        // Changelog (показываем только если нет диалога подтверждения)
        if (!showConfirmDialog && updateInfo.changelog != null && !updateInfo.changelog.isEmpty()) {
            drawString(fontRendererObj, "§7Изменения:", boxX + 20, textY, 0xFFFFFF);
            textY += 15;
            
            String[] changelogLines = updateInfo.changelog.split("\n");
            for (int i = 0; i < Math.min(changelogLines.length, 3); i++) {
                String line = changelogLines[i].trim();
                if (line.length() > 50) {
                    line = line.substring(0, 47) + "...";
                }
                drawString(fontRendererObj, "§8• §7" + line, boxX + 30, textY, 0xCCCCCC);
                textY += 12;
            }
        }
        
        // Прогресс загрузки
        if (UpdateDownloader.downloading) {
            textY += 10;
            drawString(fontRendererObj, "§7" + UpdateDownloader.downloadStatus, boxX + 20, textY, 0xFFFFFF);
            textY += 15;
            
            // Прогресс бар
            int barWidth = boxWidth - 40;
            int barX = boxX + 20;
            int barY = textY;
            int barHeight = 10;
            
            drawRect(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            drawRect(barX, barY, barX + (int)(barWidth * UpdateDownloader.downloadProgress), 
                    barY + barHeight, 0xFF00AAFF);
            
            String percent = String.format("%.1f%%", UpdateDownloader.downloadProgress * 100);
            drawCenteredString(fontRendererObj, percent, barX + barWidth / 2, barY + 1, 0xFFFFFF);
        }
        
        // Отрисовка кнопок
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            mc.displayGuiScreen(null);
        }
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        // Обновляем GUI если идет загрузка (каждые 5 тиков для производительности)
        if (UpdateDownloader.downloading && mc.theWorld != null && mc.theWorld.getTotalWorldTime() % 5 == 0) {
            // Не пересоздаем кнопки полностью, только обновляем отрисовку
        }
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // Если загрузка завершена, обновляем статус
        if (!UpdateDownloader.downloading && UpdateDownloader.downloadProgress >= 1.0f) {
            UpdateChecker.updateAvailable = false;
        }
    }
}
