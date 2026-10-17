package net.cyvplugin.gui;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.bridge.CyvThemeBridge;
import net.cyvplugin.macro.FavoritesStore;
import net.cyvplugin.macro.KeybindStore;
import net.cyvplugin.macro.MacroMirror;
import net.cyvplugin.macro.MacroStore;
import net.cyvplugin.macro.SettingsStore;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class GuiMacroList extends GuiScreen {
    private static final int SIZE_X = 500;
    private static final int SIZE_Y = 300;
    private static final int ROW_CARD_HEIGHT = 18;
    private static final int ROW_GAP = 6;
    private static final int LIST_RIGHT_INSET = 50;
    private static final int SEARCH_HEIGHT = 16;
    private static final int HEART_SIZE = 9;
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_RIGHT_INSET = 5;
    private static final String PLAY_LABEL = "Run";
    private static final String MIRROR_LABEL = "Mirror";
    private static final String TOGGLE_TOOLTIP = "Display MACRO text";
    private static final int SWITCH_W = 30;
    private static final int SWITCH_H = 12;
    private static final int SWITCH_KNOB = 10;
    private static final int TRACK_OFF_COLOR = 0xFF4A4A4A;
    private static final int TRACK_ON_COLOR = 0xFF55FF55;
    private static final long SWITCH_ANIM_MS = 180L;
    private static final int BUTTON_BG = 0xB0202020;
    private static final int BUTTON_BG_HOVER = 0xB03A3A3A;
    private static final int ROW_NORMAL = 0x90383838;
    private static final int ROW_TEXT = 0xCCCCCC;
    private static final int SEARCH_TEXT = 0xFFFFFFFF;
    private static final int SEARCH_PLACEHOLDER = 0xFF777777;
    private static final int SCROLL_TRACK = 0x80242424;
    private static final int SCROLL_KNOB = 0x903F3F3F;
    private static final int SCROLL_KNOB_HOVER = 0x905A5A5A;
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    private GuiTextField searchField;
    private List<String> allMacros;
    private List<String> filteredMacros;
    private Set<String> favorites;
    private int maxScroll = 0;
    private float scroll = 0f;
    private boolean scrollClicked = false;
    private String capturingMacro = null;
    private float switchAnim;
    private float switchAnimFrom;
    private float switchAnimTo;
    private long switchAnimStart;

    @Override
    public void initGui() {
        allMacros = MacroStore.listNames();
        favorites = FavoritesStore.load();
        switchAnim = SettingsStore.isMacroHudVisible() ? 1f : 0f;
        switchAnimFrom = switchAnim;
        switchAnimTo = switchAnim;
        switchAnimStart = 0L;
        searchField = new GuiTextField(0, fontRendererObj, listX1() + 4, searchY() + 3, searchWidth() - 8, SEARCH_HEIGHT - 6);
        searchField.setMaxStringLength(64);
        searchField.setEnableBackgroundDrawing(false);
        refreshFiltered();
    }

    private void refreshFiltered() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        filteredMacros = new ArrayList<String>();
        if (allMacros != null) {
            for (String name : allMacros) {
                if (query.isEmpty() || name.toLowerCase().contains(query)) filteredMacros.add(name);
            }
        }
        Collections.sort(filteredMacros, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                boolean fa = favorites.contains(a);
                boolean fb = favorites.contains(b);
                if (fa != fb) return fa ? -1 : 1;
                return a.compareToIgnoreCase(b);
            }
        });
        updateMaxScroll();
    }

    private int rowStride() { return ROW_CARD_HEIGHT + ROW_GAP; }

    private int panelX1() { return width / 2 - SIZE_X / 2 - 15; }
    private int panelY1() { return height / 2 - SIZE_Y / 2 - 4; }
    private int panelX2() { return width / 2 + SIZE_X / 2 + 14; }
    private int panelY2() { return height / 2 + SIZE_Y / 2 + 4; }

    private int listX1() { return width / 2 - SIZE_X / 2 + 6; }
    private int listX2() { return panelX2() - LIST_RIGHT_INSET; }
    private int titleY() { return height / 2 - SIZE_Y / 2 + 10; }
    private int searchY() { return height / 2 - SIZE_Y / 2 + 24; }
    private int searchWidth() { return listX2() - listX1(); }
    private int listY1() { return searchY() + SEARCH_HEIGHT + 6; }
    private int listY2() { return height / 2 + SIZE_Y / 2 - 8; }
    private int listHeight() { return listY2() - listY1(); }

    private int scrollbarX1() { return width / 2 + SIZE_X / 2 - 9; }
    private int scrollbarX2() { return width / 2 + SIZE_X / 2 - 6; }
    private int heartX() { return listX1() + 5; }
    private int textX() { return heartX() + HEART_SIZE + 4; }
    private int buttonY(int rowY) { return rowY + (ROW_CARD_HEIGHT - BUTTON_HEIGHT) / 2; }

    private void updateMaxScroll() {
        int contentHeight = filteredMacros == null ? 0 : filteredMacros.size() * rowStride();
        maxScroll = Math.max(0, contentHeight - listHeight());
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    private int scrollbarHeight() {
        if (maxScroll <= 0) return listHeight();
        int contentHeight = filteredMacros.size() * rowStride();
        return Math.max(20, (int) (listHeight() * ((float) listHeight() / contentHeight)));
    }

    private int rowIndexAt(int mouseX, int mouseY) {
        if (mouseX < listX1() || mouseX > listX2()) return -1;
        for (int i = 0; i < filteredMacros.size(); i++) {
            int rowY = listY1() + i * rowStride() - (int) scroll;
            if (mouseY >= rowY && mouseY <= rowY + ROW_CARD_HEIGHT) return i;
        }
        return -1;
    }

    private boolean inHeart(int mouseX, int mouseY, int rowY) {
        return mouseX >= heartX() && mouseX <= heartX() + HEART_SIZE
                && mouseY >= rowY && mouseY <= rowY + ROW_CARD_HEIGHT;
    }

    private int playButtonWidth() { return fontRendererObj.getStringWidth(PLAY_LABEL) + 8; }

    private int keyButtonWidth(String name) {
        return Math.max(12, fontRendererObj.getStringWidth(keyLabel(name)) + 8);
    }

    private int keyButtonX1(String name) { return listX2() - BUTTON_RIGHT_INSET - keyButtonWidth(name); }

    private int mirrorButtonWidth() { return fontRendererObj.getStringWidth(MIRROR_LABEL) + 8; }

    private int mirrorButtonX1(String name) { return keyButtonX1(name) - BUTTON_GAP - mirrorButtonWidth(); }

    private int playButtonX1(String name) { return mirrorButtonX1(name) - BUTTON_GAP - playButtonWidth(); }

    private String keyLabel(String name) {
        if (name.equals(capturingMacro)) return "...";
        int code = KeybindStore.get(name);
        return code < 0 ? "+" : Keyboard.getKeyName(code);
    }

    private boolean inPlayButton(int mouseX, int mouseY, String name, int rowY) {
        int x1 = playButtonX1(name);
        int y = buttonY(rowY);
        return mouseX >= x1 && mouseX <= x1 + playButtonWidth()
                && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
    }

    private boolean inMirrorButton(int mouseX, int mouseY, String name, int rowY) {
        int x1 = mirrorButtonX1(name);
        int y = buttonY(rowY);
        return mouseX >= x1 && mouseX <= x1 + mirrorButtonWidth()
                && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
    }

    private boolean inKeyButton(int mouseX, int mouseY, String name, int rowY) {
        int x1 = keyButtonX1(name);
        int y = buttonY(rowY);
        return mouseX >= x1 && mouseX <= x1 + keyButtonWidth(name)
                && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
    }

    private void drawRowButton(int x1, int width, int y, String label, boolean hovered) {
        CyvThemeBridge.drawRoundedRect(x1, y, x1 + width, y + BUTTON_HEIGHT, 3,
                hovered ? BUTTON_BG_HOVER : BUTTON_BG);
        int labelX = x1 + (width - fontRendererObj.getStringWidth(label)) / 2;
        CyvThemeBridge.drawString(label, labelX, y + (BUTTON_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2,
                0xFFFFFFFF, true);
    }

    private int switchTrackX1() {
        return width - 8 - SWITCH_W;
    }

    private int switchTrackY() {
        return 8;
    }

    private int switchRootX1() {
        return switchTrackX1() - fontRendererObj.getStringWidth("MACRO") - 8;
    }

    private boolean inMacroHudButton(int mouseX, int mouseY) {
        return mouseX >= switchRootX1() && mouseX <= switchTrackX1() + SWITCH_W
                && mouseY >= switchTrackY() && mouseY <= switchTrackY() + SWITCH_H;
    }

    private void triggerSwitchAnimation() {
        switchAnimFrom = switchAnim;
        switchAnimTo = SettingsStore.isMacroHudVisible() ? 1f : 0f;
        switchAnimStart = System.currentTimeMillis();
    }

    private void updateSwitchAnimation() {
        if (switchAnimStart == 0L) {
            switchAnim = SettingsStore.isMacroHudVisible() ? 1f : 0f;
            return;
        }
        float t = (System.currentTimeMillis() - switchAnimStart) / (float) SWITCH_ANIM_MS;
        if (t >= 1f) {
            switchAnim = switchAnimTo;
            switchAnimStart = 0L;
            return;
        }
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        switchAnim = switchAnimFrom + (switchAnimTo - switchAnimFrom) * eased;
    }

    private static int lerpColor(int from, int to, float t) {
        if (t <= 0f) return from;
        if (t >= 1f) return to;
        int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
        int r = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        int g = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawMacroHudSwitch(int mouseX, int mouseY) {
        updateSwitchAnimation();
        boolean hovered = inMacroHudButton(mouseX, mouseY);

        int labelX = switchRootX1();
        int labelY = switchTrackY() + (SWITCH_H - fontRendererObj.FONT_HEIGHT) / 2;
        CyvThemeBridge.drawString("MACRO", labelX, labelY, 0xFFBBBBBB, true);

        int trackX1 = switchTrackX1();
        int trackY = switchTrackY();
        int trackColor = lerpColor(TRACK_OFF_COLOR, TRACK_ON_COLOR, switchAnim);
        if (hovered) trackColor = CyvThemeBridge.lighten(trackColor, 0.15f);
        CyvThemeBridge.drawRoundedRect(trackX1, trackY, trackX1 + SWITCH_W, trackY + SWITCH_H,
                SWITCH_H / 2f, trackColor);

        int knobX = Math.round(trackX1 + 1 + (SWITCH_W - SWITCH_KNOB - 2) * switchAnim);
        int knobY = trackY + (SWITCH_H - SWITCH_KNOB) / 2;
        CyvThemeBridge.drawRoundedRect(knobX, knobY, knobX + SWITCH_KNOB, knobY + SWITCH_KNOB,
                SWITCH_KNOB / 2f, 0xFFFFFFFF);
    }

    private void drawMacroHudTooltip(int mouseX, int mouseY) {
        if (!inMacroHudButton(mouseX, mouseY)) return;
        int tipWidth = fontRendererObj.getStringWidth(TOGGLE_TOOLTIP) + 8;
        int x1 = switchTrackX1() + SWITCH_W - tipWidth;
        if (x1 < 2) x1 = 2;
        int y1 = switchTrackY() + SWITCH_H + 3;
        int y2 = y1 + BUTTON_HEIGHT - 1;
        CyvThemeBridge.drawRoundedRect(x1, y1, x1 + tipWidth, y2, 3, BUTTON_BG);
        CyvThemeBridge.drawString(TOGGLE_TOOLTIP, x1 + 4,
                y1 + (y2 - y1 - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF, true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        updateMaxScroll();

        CyvThemeBridge.drawRoundedRect(panelX1(), panelY1(), panelX2(), panelY2(), 5, CyvThemeBridge.theme("background1"));
        CyvThemeBridge.drawCenteredString("Macros", width / 2, titleY());

        int scale = new ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(listX1() * scale, (height - listY2()) * scale,
                (listX2() - listX1()) * scale, (listY2() - listY1()) * scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        int hoveredIndex = rowIndexAt(mouseX, mouseY);
        if (filteredMacros != null) {
            for (int i = 0; i < filteredMacros.size(); i++) {
                String name = filteredMacros.get(i);
                int rowY = listY1() + i * rowStride() - (int) scroll;
                if (rowY + ROW_CARD_HEIGHT < listY1() || rowY > listY2()) continue;
                boolean favorite = favorites.contains(name);
                boolean hovered = i == hoveredIndex;
                boolean hasBind = KeybindStore.get(name) >= 0;
                CyvThemeBridge.drawRoundedRect(listX1(), rowY, listX2(), rowY + ROW_CARD_HEIGHT, 3, rowColor(favorite, hovered));

                int textRight;
                if (hovered) textRight = playButtonX1(name) - 4;
                else if (hasBind) textRight = keyButtonX1(name) - 4;
                else textRight = listX2() - 6;
                String display = name.equals(capturingMacro) ? "Press a key..." : name;
                String label = fontRendererObj.trimStringToWidth(display, Math.max(0, textRight - textX()));
                CyvThemeBridge.drawString(label, textX(),
                        rowY + (ROW_CARD_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2, 0xFFFFFFFF, true);
                drawHeart(heartX(), rowY + (ROW_CARD_HEIGHT - HEART_SIZE) / 2, favorite);

                if (hovered) {
                    drawRowButton(playButtonX1(name), playButtonWidth(), buttonY(rowY), PLAY_LABEL,
                            inPlayButton(mouseX, mouseY, name, rowY));
                    drawRowButton(mirrorButtonX1(name), mirrorButtonWidth(), buttonY(rowY), MIRROR_LABEL,
                            inMirrorButton(mouseX, mouseY, name, rowY));
                }
                if (hovered || hasBind) {
                    drawRowButton(keyButtonX1(name), keyButtonWidth(name), buttonY(rowY), keyLabel(name),
                            inKeyButton(mouseX, mouseY, name, rowY));
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        drawSearchBox();
        drawScrollbar(mouseX, mouseY);
        drawMacroHudSwitch(mouseX, mouseY);
        drawMacroHudTooltip(mouseX, mouseY);
    }

    private int rowColor(boolean favorite, boolean hovered) {
        int accent = CyvThemeBridge.theme("highlight");
        if (favorite) {
            return accent;
        }
        return hovered ? accent : ROW_NORMAL;
    }

    private void drawHeart(int x, int y, boolean favorite) {
        mc.getTextureManager().bindTexture(ICONS);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        drawTexturedModalRect(x, y, 52, favorite ? 0 : 9, HEART_SIZE, HEART_SIZE);
    }

    private void drawSearchBox() {
        CyvThemeBridge.drawRoundedRect(listX1(), searchY(), listX2(), searchY() + SEARCH_HEIGHT, 3, ROW_NORMAL);
        CyvThemeBridge.drawRoundedRect(listX1() + 1, searchY() + 1, listX2() - 1, searchY() + SEARCH_HEIGHT - 1, 2, SCROLL_TRACK);
        String text = searchField.getText();
        int textY = searchY() + (SEARCH_HEIGHT - fontRendererObj.FONT_HEIGHT) / 2;
        if (text.isEmpty() && !searchField.isFocused()) {
            CyvThemeBridge.drawString("Search macros...", listX1() + 5, textY, SEARCH_PLACEHOLDER, false);
        } else {
            CyvThemeBridge.drawString(text, listX1() + 5, textY, SEARCH_TEXT, false);
        }
        if (searchField.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = listX1() + 5 + fontRendererObj.getStringWidth(text);
            CyvThemeBridge.drawRoundedRect(cursorX, searchY() + 3, cursorX + 1, searchY() + SEARCH_HEIGHT - 3, 0, SEARCH_TEXT);
        }
    }

    private void drawScrollbar(int mouseX, int mouseY) {
        CyvThemeBridge.drawRoundedRect(scrollbarX1(), listY1(), scrollbarX2(), listY2(), 2, SCROLL_TRACK);
        if (maxScroll > 0) {
            int sbH = scrollbarHeight();
            int sbY = listY1() + (int) ((listHeight() - sbH) * (scroll / maxScroll));
            boolean hovered = mouseX >= scrollbarX1() && mouseX <= scrollbarX2() && mouseY >= sbY && mouseY <= sbY + sbH;
            CyvThemeBridge.drawRoundedRect(scrollbarX1(), sbY, scrollbarX2(), sbY + sbH, 2,
                    hovered ? SCROLL_KNOB_HOVER : SCROLL_KNOB);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll -= wheel * 0.03f;
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (capturingMacro != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_SPACE) {
                KeybindStore.remove(capturingMacro);
            } else {
                KeybindStore.set(capturingMacro, keyCode);
            }
            capturingMacro = null;
            return;
        }
        if (searchField.isFocused()) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                searchField.textboxKeyTyped(typedChar, keyCode);
                refreshFiltered();
                scroll = 0;
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0 && inMacroHudButton(mouseX, mouseY)) {
            SettingsStore.setMacroHudVisible(!SettingsStore.isMacroHudVisible());
            triggerSwitchAnimation();
            return;
        }

        if (mouseButton == 0 && inSearchBox(mouseX, mouseY)) {
            searchField.setFocused(true);
            capturingMacro = null;
            return;
        }
        if (mouseButton == 0) searchField.setFocused(false);

        int index = rowIndexAt(mouseX, mouseY);
        if (index >= 0) {
            String name = filteredMacros.get(index);
            int rowY = listY1() + index * rowStride() - (int) scroll;
            if (mouseButton == 0 && inHeart(mouseX, mouseY, rowY)) {
                toggleFavorite(name);
                return;
            }
            if (mouseButton == 0 && inPlayButton(mouseX, mouseY, name, rowY)) {
                runMacro(name);
                return;
            }
            if (mouseButton == 0 && inMirrorButton(mouseX, mouseY, name, rowY)) {
                mirrorMacro(name);
                return;
            }
            if (inKeyButton(mouseX, mouseY, name, rowY)) {
                if (mouseButton == 0) {
                    capturingMacro = name.equals(capturingMacro) ? null : name;
                    searchField.setFocused(false);
                } else if (mouseButton == 1) {
                    KeybindStore.remove(name);
                    if (name.equals(capturingMacro)) capturingMacro = null;
                }
                return;
            }
            capturingMacro = null;
            return;
        }

        capturingMacro = null;

        if (mouseButton == 0 && mouseX >= scrollbarX1() && mouseX <= scrollbarX2()
                && mouseY >= listY1() && mouseY <= listY2()) {
            scrollClicked = true;
        }
    }

    private void runMacro(String name) {
        try {
            CyvForgeBridge.runMacro(name);
            mc.displayGuiScreen(null);
        } catch (Exception e) {
            CyvPlugin.sendChatMessage("Could not run the macro: " + e.getMessage());
        }
    }

    private void mirrorMacro(String name) {
        int ticks = MacroMirror.mirror(name);
        if (ticks < 0) {
            CyvPlugin.sendChatMessage("Could not mirror \"" + name + "\".");
        } else {
            CyvPlugin.sendChatMessage("Mirrored \"" + name + "\".");
        }
    }

    private boolean inSearchBox(int mouseX, int mouseY) {
        return mouseX >= listX1() && mouseX <= listX2()
                && mouseY >= searchY() && mouseY <= searchY() + SEARCH_HEIGHT;
    }

    private void toggleFavorite(String name) {
        if (!favorites.remove(name)) favorites.add(name);
        FavoritesStore.save(favorites);
        refreshFiltered();
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        if (scrollClicked && maxScroll > 0) {
            int sbH = scrollbarHeight();
            scroll = (mouseY - listY1() - sbH / 2f) / (listHeight() - sbH) * maxScroll;
            if (scroll > maxScroll) scroll = maxScroll;
            if (scroll < 0) scroll = 0;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        scrollClicked = false;
    }
}