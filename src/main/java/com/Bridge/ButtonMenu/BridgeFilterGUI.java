// BridgeFilterGUI.java
package com.Bridge.ButtonMenu;

import net.minecraft.client.gui.*;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class BridgeFilterGUI extends GuiScreen {

    private GuiTextField textField;
    private int selectedCategory = 0;

    @Override
    public void initGui() {
        buttonList.clear();

        addCategoryButton(0, "Фильтр сообщений");
        addCategoryButton(1, "Формат чата");
        addCategoryButton(2, "Guild Bridge");
        addCategoryButton(3, "Инфо");

        if (selectedCategory == 0) buildFilterPage();
        else if (selectedCategory == 1) buildFormatPage();
        else if (selectedCategory == 2) buildGuildBridgePage();
        else if (selectedCategory == 3) buildInfoPage();

        // Кнопка проверки обновлений (только если не на странице Инфо)
        if (selectedCategory != 3) {
            String updateText = UpdateChecker.updateAvailable ? "§aОбновление доступно!" : "Проверить обновления";
            buttonList.add(new GuiButton(500, width - 250, height - 35, 120, 20, updateText));
        }
        
        buttonList.add(new GuiButton(999, width - 130, height - 35, 110, 20, "Закрыть"));
    }

    private void addCategoryButton(int id, String name) {
        int y = 60 + id * 40;
        String prefix = (id == selectedCategory) ? "§f> " : "§7";
        buttonList.add(new GuiButton(100 + id, 20, y, 160, 30, prefix + name));
    }

    private void buildFilterPage() {
        int left = 250, pad = 20, botX = left + pad, botY = 70;

        buttonList.add(new GuiButton(10, botX, botY, 200, 24, "§lБот: §b" + capitalize(BridgeFilterConfig.selectedBot)));

        int inputY = botY + 40;
        if (textField == null) {
            textField = new GuiTextField(0, fontRendererObj, botX, inputY, 200, 20);
            textField.setMaxStringLength(50);
        } else {
            textField.xPosition = botX;
            textField.yPosition = inputY;
            textField.width = 200;
        }
        buttonList.add(new GuiButton(50, botX + 210, inputY, 40, 20, "§a+"));

        int toggleY = inputY + 40;
        buttonList.add(new GuiButton(60, botX, toggleY, 250, 28,
                BridgeFilterConfig.filterEnabled ? "§aФильтр: ВКЛ" : "§cФильтр: ВЫКЛ"));
        buttonList.add(new GuiButton(61, botX, toggleY + 36, 250, 28,
                BridgeFilterConfig.redHighlight ? "§aКрасная подсветка: ВКЛ" : "§cКрасная подсветка: ВЫКЛ"));

        int listX = botX + 280, listY = botY - 10;
        // Создаем копию блоклиста для безопасной итерации
        List<String> blockListCopy;
        synchronized (BridgeFilterConfig.blockList) {
            blockListCopy = new ArrayList<String>(BridgeFilterConfig.blockList);
        }
        int maxDisplay = Math.min(blockListCopy.size(), 14); // Максимум 14 элементов для отображения
        for (int i = 0; i < maxDisplay; i++) {
            String word = blockListCopy.get(i);
            if (word != null && !word.trim().isEmpty()) {
                int y = listY + i * 30;
                // Ограничиваем длину отображаемого текста для длинных ников
                String displayWord = word.length() > 20 ? word.substring(0, 17) + "..." : word;
                buttonList.add(new GuiButton(200 + i, listX, y, 180, 26, "§c" + displayWord));
                buttonList.add(new GuiButton(300 + i, listX + 188, y + 3, 22, 22, "§cX"));
            }
        }
    }

    private void buildFormatPage() {
        int c = width / 2 - 100;

        buttonList.add(new GuiButton(1000, c, 80, 200, 28,
                BridgeFilterConfig.nickHighlightEnabled ? "§aПодсветка ника: ВКЛ" : "§cПодсветка ника: ВЫКЛ"));

        // Названия цветов для конфига (сохраняются в файл)
        String[] colorNames = {"yellow", "red", "green", "cyan", "pink", "orange", "white", "black"};
        // Отображаемые названия с цветами
        String[] displayNames = {"§eЖёлтый", "§cКрасный", "§aЗелёный", "§bГолубой", "§dРозовый", "§6Оранжевый", "§fБелый", "§0Чёрный"};
        // Коды цветов для Minecraft
        String[] codes = {"§e", "§c", "§a", "§b", "§d", "§6", "§f", "§0"};

        // Получаем текущее название цвета (если есть) или конвертируем из кода
        String currentColorName = BridgeFilterConfig.nickHighlightColorName != null ? 
            BridgeFilterConfig.nickHighlightColorName.toLowerCase() : 
            BridgeFilterConfig.colorCodeToName(BridgeFilterConfig.nickHighlightColor);

        for (int i = 0; i < 8; i++) {
            int y = 130 + i * 36;
            // Проверяем совпадение по названию цвета (новый формат)
            boolean isSelected = currentColorName.equals(colorNames[i]) || 
                                (currentColorName == null && BridgeFilterConfig.nickHighlightColor != null && 
                                 BridgeFilterConfig.nickHighlightColor.equals(codes[i]));
            String prefix = isSelected ? "§f> " : "§7";
            buttonList.add(new GuiButton(1100 + i, c, y, 200, 32, prefix + displayNames[i]));
        }
    }

    private void buildGuildBridgePage() {
        int c = width / 2 - 100;

        buttonList.add(new GuiButton(2000, c, 80, 200, 28,
                BridgeFilterConfig.guildBridgeFormatEnabled ? "§aФормат Guild: ВКЛ" : "§cФормат Guild: ВЫКЛ"));
    }

    private void buildInfoPage() {
        int c = width / 2 - 100;
        int startY = 80;
        int buttonHeight = 28;
        int buttonSpacing = 35;
        
        // Кнопка "Обновить Автоматически" - открывает актуальный релиз
        buttonList.add(new GuiButton(501, c, startY, 200, buttonHeight, "§bОбновить Автоматически"));
        
        // GitHub Repository
        buttonList.add(new GuiButton(502, c, startY + buttonSpacing * 1, 200, buttonHeight, "§7[§fGitHub§7] §bРепозиторий"));
        
        // Невидимые кнопки для кликабельных ссылок в тексте (ID 509-513)
        // Позиции будут рассчитаны динамически в drawInfoPageContent() при каждой отрисовке
        // Создаем кнопки с нулевыми координатами, они будут обновлены при отрисовке
        buttonList.add(new GuiButton(509, 0, 0, 350, 14, "")); // GitHub ссылка в тексте
        buttonList.add(new GuiButton(510, 0, 0, 350, 14, "")); // Discord: Кафе Антейку
        buttonList.add(new GuiButton(511, 0, 0, 350, 14, "")); // Discord: AYERUS
        buttonList.add(new GuiButton(512, 0, 0, 350, 14, "")); // Discord: TheSquidsEmpire
        buttonList.add(new GuiButton(513, 0, 0, 350, 14, "")); // Discord: The Final Calamity
        
        // Автоматически проверяем обновления при открытии страницы Инфо
        if (!UpdateChecker.checking && !UpdateChecker.updateAvailable) {
            UpdateChecker.checkForUpdates();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id >= 100 && button.id < 200) {
            selectedCategory = button.id - 100;
            textField = null;
            initGui();
            return;
        }

        if (selectedCategory == 0) {
            if (button.id == 10) {
                String[] bots = {"etobridge", "koorikage", "mothikh", "tenokage", "uzbekF3ndi", "gem_zz"};
                int idx = java.util.Arrays.asList(bots).indexOf(BridgeFilterConfig.selectedBot);
                BridgeFilterConfig.selectedBot = bots[(idx + 1) % bots.length]; // Исправлено: используем bots.length вместо 4
                BridgeFilterConfig.save(); // Сохраняем конфиг при изменении бота
                initGui();
            }
            if (button.id == 50 && textField != null) {
                try {
                    String txt = textField.getText().trim();
                    if (txt != null && !txt.isEmpty()) {
                        boolean wasAdded = false;
                        // Синхронизируем доступ к blockList для избежания ConcurrentModificationException
                        synchronized (BridgeFilterConfig.blockList) {
                            if (!BridgeFilterConfig.blockList.contains(txt)) {
                                BridgeFilterConfig.blockList.add(txt);
                                wasAdded = true;
                                System.out.println("[Bridge Filter] Добавлено в блоклист: " + txt + ", новый размер: " + BridgeFilterConfig.blockList.size());
                            } else {
                                System.out.println("[Bridge Filter] Ник уже в блоклисте: " + txt);
                            }
                        }
                        // Сохраняем конфиг после добавления: сразу и через 5 секунд для надежности
                        if (wasAdded) {
                            // Немедленное сохранение (в отдельном потоке, чтобы не блокировать UI)
                            Thread immediateSaveThread = new Thread(() -> {
                                try {
                                    BridgeFilterConfig.saveSync();
                                    System.out.println("[Bridge Filter] Конфиг сохранен сразу после добавления в блоклист");
                                } catch (Exception e) {
                                    System.err.println("[Bridge Filter] Ошибка при немедленном сохранении: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }, "BridgeFilter-BlockListImmediateSave");
                            immediateSaveThread.setPriority(Thread.MAX_PRIORITY);
                            immediateSaveThread.start();
                            
                            // Дополнительное сохранение через 5 секунд для гарантии
                            Thread delayedSaveThread = new Thread(() -> {
                                try {
                                    Thread.sleep(5000);
                                    BridgeFilterConfig.saveSync();
                                    System.out.println("[Bridge Filter] Конфиг повторно сохранен после добавления в блоклист (через 5 сек)");
                                    if (mc != null && mc.thePlayer != null) {
                                        mc.thePlayer.addChatMessage(new ChatComponentText("§a[Bridge Filter] Блоклист сохранен в конфиг"));
                                    }
                                } catch (Exception e) {
                                    System.err.println("[Bridge Filter] Ошибка при отложенном сохранении: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }, "BridgeFilter-BlockListDelayedSave");
                            delayedSaveThread.setPriority(Thread.MAX_PRIORITY);
                            delayedSaveThread.start();
                        }
                        textField.setText("");
                        initGui();
                    }
                } catch (Exception e) {
                    System.err.println("[Bridge Filter] Ошибка при добавлении в блоклист: " + e.getMessage());
                    e.printStackTrace();
                    // Не крашим игру, просто показываем ошибку
                    if (mc != null && mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new ChatComponentText("§c[Bridge Filter] Ошибка при добавлении в блоклист"));
                    }
                }
            }
            if (button.id == 60) { 
                BridgeFilterConfig.filterEnabled = !BridgeFilterConfig.filterEnabled; 
                BridgeFilterConfig.save(); // Сохраняем конфиг при изменении фильтра
                initGui(); 
            }
            if (button.id == 61) { 
                BridgeFilterConfig.redHighlight = !BridgeFilterConfig.redHighlight; 
                BridgeFilterConfig.save(); // Сохраняем конфиг при изменении подсветки
                initGui(); 
            }
            if (button.id >= 300 && button.id < 400) {
                try {
                    int index = button.id - 300;
                    boolean wasRemoved = false;
                    String removedName = null;
                    synchronized (BridgeFilterConfig.blockList) {
                        // Дополнительная проверка размера внутри synchronized блока
                        if (index >= 0 && index < BridgeFilterConfig.blockList.size()) {
                            removedName = BridgeFilterConfig.blockList.remove(index);
                            wasRemoved = true;
                            System.out.println("[Bridge Filter] Удалено из блоклиста: " + (removedName != null ? removedName : "null") + ", новый размер: " + BridgeFilterConfig.blockList.size());
                        } else {
                            System.err.println("[Bridge Filter] Индекс вне диапазона: " + index + ", размер: " + BridgeFilterConfig.blockList.size());
                        }
                    }
                    // Сохраняем конфиг после удаления: сразу и через 5 секунд для надежности
                    if (wasRemoved) {
                        // Немедленное сохранение (в отдельном потоке, чтобы не блокировать UI)
                        Thread immediateSaveThread = new Thread(() -> {
                            try {
                                BridgeFilterConfig.saveSync();
                                System.out.println("[Bridge Filter] Конфиг сохранен сразу после удаления из блоклиста");
                            } catch (Exception e) {
                                System.err.println("[Bridge Filter] Ошибка при немедленном сохранении: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }, "BridgeFilter-BlockListDeleteImmediateSave");
                        immediateSaveThread.setPriority(Thread.MAX_PRIORITY);
                        immediateSaveThread.start();
                        
                        // Дополнительное сохранение через 5 секунд для гарантии
                        Thread delayedSaveThread = new Thread(() -> {
                            try {
                                Thread.sleep(5000);
                                BridgeFilterConfig.saveSync();
                                System.out.println("[Bridge Filter] Конфиг повторно сохранен после удаления из блоклиста (через 5 сек)");
                                if (mc != null && mc.thePlayer != null) {
                                    mc.thePlayer.addChatMessage(new ChatComponentText("§a[Bridge Filter] Блоклист сохранен в конфиг"));
                                }
                            } catch (Exception e) {
                                System.err.println("[Bridge Filter] Ошибка при отложенном сохранении: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }, "BridgeFilter-BlockListDeleteDelayedSave");
                        delayedSaveThread.setPriority(Thread.MAX_PRIORITY);
                        delayedSaveThread.start();
                    }
                    initGui();
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("[Bridge Filter] Индекс вне диапазона при удалении из блоклиста: " + e.getMessage());
                    // Не крашим игру, просто обновляем GUI
                    initGui();
                } catch (Exception e) {
                    System.err.println("[Bridge Filter] Ошибка при удалении из блоклиста: " + e.getMessage());
                    e.printStackTrace();
                    // Не крашим игру
                    if (mc != null && mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new ChatComponentText("§c[Bridge Filter] Ошибка при удалении из блоклиста"));
                    }
                }
            }
        }

        if (selectedCategory == 1) {
            if (button.id == 1000) {
                BridgeFilterConfig.nickHighlightEnabled = !BridgeFilterConfig.nickHighlightEnabled;
                BridgeFilterConfig.save(); // Сохраняем конфиг при изменении подсветки ника
                initGui();
            }
            if (button.id >= 1100 && button.id < 1200) {
                // Названия цветов для сохранения в конфиг
                String[] colorNames = {"yellow", "red", "green", "cyan", "pink", "orange", "white", "black"};
                // Коды цветов для Minecraft
                String[] codes = {"§e", "§c", "§a", "§b", "§d", "§6", "§f", "§0"};
                
                int colorIndex = button.id - 1100;
                if (colorIndex >= 0 && colorIndex < colorNames.length && colorIndex < codes.length) {
                    // Обновляем и название цвета (для конфига) и код (для Minecraft)
                    BridgeFilterConfig.nickHighlightColorName = colorNames[colorIndex];
                    BridgeFilterConfig.nickHighlightColor = codes[colorIndex];
                    System.out.println("[Bridge Filter] Цвет ника изменен: " + colorNames[colorIndex] + " (" + codes[colorIndex] + ")");
                    BridgeFilterConfig.save(); // Сохраняем конфиг при изменении цвета ника
                }
                initGui();
            }
        }

        if (selectedCategory == 2) {
            if (button.id == 2000) {
                BridgeFilterConfig.guildBridgeFormatEnabled = !BridgeFilterConfig.guildBridgeFormatEnabled;
                BridgeFilterConfig.save(); // Сохраняем конфиг при изменении форматирования Guild Bridge
                initGui();
            }
        }

        // Обработка кнопки обновлений
        if (button.id == 500) {
            if (UpdateChecker.updateAvailable && UpdateChecker.latestUpdate != null) {
                mc.displayGuiScreen(new UpdateGUI(UpdateChecker.latestUpdate));
            } else {
                UpdateChecker.checkForUpdates();
                // Показываем сообщение о проверке
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§9[Bridge Filter] §7Проверяем обновления..."));
                }
            }
        }

        // Обработка кнопок страницы Инфо
        if (selectedCategory == 3) {
            if (button.id == 501) {
                // Кнопка "Обновить Автоматически" - открывает актуальный релиз
                UpdateChecker.UpdateInfo latestInfo = new UpdateChecker.UpdateInfo(
                    "latest", 
                    null, 
                    "", 
                    "https://github.com/Nayokage/BridgeFilter/releases/latest"
                );
                UpdateDownloader.openReleasesPage(latestInfo);
            } else if (button.id == 502) {
                // Кнопка "GitHub Репозиторий"
                openUrlInBrowser("https://github.com/Nayokage/BridgeFilter");
            } else if (button.id == 509) {
                // Невидимая кнопка для GitHub ссылки в тексте
                openUrlInBrowser("https://github.com/Nayokage/BridgeFilter");
            } else if (button.id == 510) {
                // Невидимая кнопка для Discord ссылки "Кафе Антейку"
                openUrlInBrowser("https://discord.gg/SyMcwJfnEe");
            } else if (button.id == 511) {
                // Невидимая кнопка для Discord ссылки "AYERUS"
                openUrlInBrowser("https://discord.gg/AYERUS");
            } else if (button.id == 512) {
                // Невидимая кнопка для Discord ссылки "TheSquidsEmpire"
                openUrlInBrowser("https://discord.gg/vQdpzeAbZu");
            } else if (button.id == 513) {
                // Невидимая кнопка для Discord ссылки "The Final Calamity"
                openUrlInBrowser("https://discord.gg/JytmJWmg6t");
            }
        }

        if (button.id == 999) mc.displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xAA000000);
        drawRect(10, 10, width - 10, height - 10, 0xFF1E1E1E);
        drawCenteredString(fontRendererObj, "§9§lBridge Filter §8v" + UpdateChecker.CURRENT_VERSION, width / 2, 20, 0x00FFFF);

        drawRect(15, 45, 195, height - 15, 0xFF252525);
        drawRect(210, 45, width - 15, height - 15, 0xFF252525);
        
        // Индикатор обновления в заголовке
        if (UpdateChecker.updateAvailable) {
            drawString(fontRendererObj, "§a●", width / 2 + 80, 22, 0x00FF00);
        }

        for (Object o : buttonList) {
            GuiButton b = (GuiButton) o;
            // Пропускаем невидимые кнопки для кликабельных ссылок (ID 509-513)
            if (b.id >= 509 && b.id <= 513) {
                continue; // Не отрисовываем эти кнопки, они только для обработки кликов
            }
            
            boolean hovered = mouseX >= b.xPosition && mouseX <= b.xPosition + b.width &&
                    mouseY >= b.yPosition && mouseY <= b.yPosition + b.height;

            drawRect(b.xPosition, b.yPosition, b.xPosition + b.width, b.yPosition + b.height,
                    hovered ? 0xFF3A3A3A : 0xFF2D2D2D);

            int border = hovered ? 0xFF00AAFF : 0xFF444444;
            drawRect(b.xPosition, b.yPosition, b.xPosition + b.width, b.yPosition + 1, border);
            drawRect(b.xPosition, b.yPosition + b.height - 1, b.xPosition + b.width, b.yPosition + b.height, border);
            drawRect(b.xPosition, b.yPosition, b.xPosition + 1, b.yPosition + b.height, border);
            drawRect(b.xPosition + b.width - 1, b.yPosition, b.xPosition + b.width, b.yPosition + b.height, border);

            String text = b.displayString;
            String clean = text.replaceAll("§[0-9a-fk-or]", "");
            int color = 0xFFFFFF;
            if (text.contains("ВКЛ")) color = 0x55FF55;
            if (text.contains("ВЫКЛ")) color = 0xFF5555;
            if (text.startsWith("§c")) color = 0xFF5555;
            if (text.startsWith("§a")) color = 0x55FF55;
            if (text.startsWith("§b")) color = 0x55FFFF;
            if (text.startsWith("§f>")) color = 0xAAAAAA;

            fontRendererObj.drawStringWithShadow(text,
                    b.xPosition + b.width / 2 - fontRendererObj.getStringWidth(clean) / 2,
                    b.yPosition + (b.height - 8) / 2, color);
        }

        if (selectedCategory == 0 && textField != null) {
            drawRect(textField.xPosition - 1, textField.yPosition - 1,
                    textField.xPosition + textField.width + 1, textField.yPosition + textField.height + 1, 0xFF00AAFF);
            drawRect(textField.xPosition, textField.yPosition,
                    textField.xPosition + textField.width, textField.yPosition + textField.height, 0xFF252525);
            textField.drawTextBox();
        }

        // Информация о Guild Bridge форматировании
        if (selectedCategory == 2) {
            int infoX = 250;
            int infoY = 130;
            drawString(fontRendererObj, "§7Форматирует сообщения от:", infoX, infoY, 0xCCCCCC);
            infoY += 15;
            drawString(fontRendererObj, "§8• gem_zz", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• uzbekF3ndi", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• etobridge", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• koorikage", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• mothikh", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• tenokage", infoX + 10, infoY, 0xAAAAAA);
            infoY += 20;
            drawString(fontRendererObj, "§7Форматы:", infoX, infoY, 0xCCCCCC);
            infoY += 15;
            drawString(fontRendererObj, "§8• .Nick: текст → [Minecraft]", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• [TG] Nick: текст → [Telegram]", infoX + 10, infoY, 0xAAAAAA);
            infoY += 12;
            drawString(fontRendererObj, "§8• Nick: текст → [Discord]", infoX + 10, infoY, 0xAAAAAA);
        }

        // Страница Инфо с кастомным текстом
        if (selectedCategory == 3) {
            drawInfoPageContent(mouseX, mouseY);
        }

        drawString(fontRendererObj, "§7Меню и там кнлпка — открыть меню", width - 190, height - 25, 0x888888);
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (selectedCategory == 0 && textField != null) textField.textboxKeyTyped(c, k);
        if (k == 1 || k == Keyboard.KEY_RSHIFT) mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException {
        super.mouseClicked(x, y, b);
        if (selectedCategory == 0 && textField != null) textField.mouseClicked(x, y, b);
    }

    private void drawInfoPageContent(int mouseX, int mouseY) {
        int infoX = 250;
        int infoY = 200; // Сместили ниже, чтобы освободить место для кнопок
        
        // ========================================
        // ЗДЕСЬ ДОБАВЛЯЙТЕ/ИЗМЕНЯЙТЕ ТЕКСТ
        // ========================================
        // Чтобы изменить текст, отредактируйте массив infoLines ниже
        int currentY = infoY;
        
        // Строка 1: Заголовок
        drawString(fontRendererObj, "§9§lBridge Filter §7v" + UpdateChecker.CURRENT_VERSION, infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        // Пустая строка
        currentY += 8;
        
        // Строка 2: Описание
        drawString(fontRendererObj, "§eДанный мод сделан Разрабочиком Nayokage и Toukaa_Kirishima (aka redeno)", infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        // Строка 3: GitHub ссылка с иконкой (кликабельная)
        // Проверяем, находится ли курсор над ссылкой для визуальной индикации
        boolean hoveredGitHub = mouseX >= infoX && mouseX <= infoX + 350 && 
                                mouseY >= currentY && mouseY <= currentY + 14;
        String githubText = hoveredGitHub ? 
            "§9Официальный Репоризиторий мода: §7[§fGitHub§7]: §b§nhttps://github.com/Nayokage/BridgeFilter" : // Подчеркнутая ссылка при наведении
            "§9Официальный Репоризиторий мода: §7[§fGitHub§7]: §bhttps://github.com/Nayokage/BridgeFilter";
        drawString(fontRendererObj, "§9● " + githubText, infoX, currentY, hoveredGitHub ? 0x55FFFF : 0xFFFFFF);
        // Обновляем позицию невидимой кнопки для GitHub ссылки (ID 509)
        updateInvisibleButtonPosition(509, infoX, currentY, 350, 14);
        currentY += 14;
        
        // Пустая строка
        currentY += 8;
        
        // Строки Discord ссылок с иконками (кликабельные)
        String[] discordLinks = {
            "§7[§9Кафе Антейку(JoJoBA)§7] §bhttps://discord.gg/SyMcwJfnEe",
            "§7[§cAYERUS§7] §bhttps://discord.gg/AYERUS",
            "§7[§9TheSquidsEmpire§7] §bhttps://discord.gg/vQdpzeAbZu",
            "§7[§4The Final Calamity§7] §bhttps://discord.gg/JytmJWmg6t"
        };
        String[] discordLinksHovered = {
            "§7[§9Кафе Антейку(JoJoBA)§7] §b§nhttps://discord.gg/SyMcwJfnEe", // Подчеркнутая ссылка при наведении
            "§7[§cAYERUS§7] §b§nhttps://discord.gg/AYERUS",
            "§7[§9TheSquidsEmpire§7] §b§nhttps://discord.gg/vQdpzeAbZu",
            "§7[§4The Final Calamity§7] §b§nhttps://discord.gg/JytmJWmg6t"
        };
        int[] discordButtonIds = {510, 511, 512, 513};
        
        for (int i = 0; i < discordLinks.length; i++) {
            // Проверяем, находится ли курсор над ссылкой
            boolean hovered = mouseX >= infoX && mouseX <= infoX + 350 && 
                             mouseY >= currentY && mouseY <= currentY + 14;
            String linkText = hovered ? discordLinksHovered[i] : discordLinks[i];
            int textColor = hovered ? 0x55FFFF : 0xFFFFFF; // Голубой цвет при наведении
            
            drawString(fontRendererObj, "§9● " + linkText, infoX, currentY, textColor);
            // Обновляем позицию невидимой кнопки для Discord ссылки
            updateInvisibleButtonPosition(discordButtonIds[i], infoX, currentY, 350, 14);
            currentY += 14;
        }
        
        // Строка: Инструкция
        drawString(fontRendererObj, "§7Используйте кнопки выше для перехода:", infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        drawString(fontRendererObj, "§8  • §bОбновить Автоматически §7- актуальный релиз", infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        drawString(fontRendererObj, "§8  • §7[§fGitHub§7] §bРепозиторий §7- исходный код", infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        // Пустая строка
        currentY += 8;
        
        // Строка: Версия
        drawString(fontRendererObj, "§7Текущая версия: §f" + UpdateChecker.CURRENT_VERSION, infoX, currentY, 0xFFFFFF);
        currentY += 14;
        
        // Строка: Обновление
        String updateText = UpdateChecker.updateAvailable && UpdateChecker.latestUpdate != null ? 
            "§aДоступно обновление: §f" + UpdateChecker.latestUpdate.version : 
            "§7Обновлений не найдено";
        drawString(fontRendererObj, updateText, infoX, currentY, 0xFFFFFF);
    }
    
    /**
     * Обновляет позицию невидимой кнопки для кликабельных ссылок
     */
    private void updateInvisibleButtonPosition(int buttonId, int x, int y, int width, int height) {
        for (GuiButton button : buttonList) {
            if (button.id == buttonId) {
                button.xPosition = x;
                button.yPosition = y;
                button.width = width;
                button.height = height;
                break;
            }
        }
    }
    
    /**
     * Вспомогательный метод для открытия URL в браузере
     * Используется для кнопок перехода на странице Инфо
     */
    private void openUrlInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("[Bridge Filter] Открыт URL в браузере: " + url);
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§9[Bridge Filter] §7Открываю ссылку в браузере..."));
                }
            } else {
                // Fallback: используем UpdateDownloader
                UpdateChecker.UpdateInfo info = new UpdateChecker.UpdateInfo("", null, "", url);
                UpdateDownloader.openReleasesPage(info);
            }
        } catch (Exception e) {
            System.err.println("[Bridge Filter] Не удалось открыть URL: " + e.getMessage());
            e.printStackTrace();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§c[Bridge Filter] Ошибка при открытии ссылки: " + e.getMessage()));
            }
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}