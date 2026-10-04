package pl.autodrip;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AutoDripScreen extends Screen {

    private static final int PANEL_W = 270;
    private static final int PANEL_H = 196;

    private ButtonWidget powerBtn;
    private ButtonWidget autoCloseBtn;
    private final ButtonWidget[] speedBtns = new ButtonWidget[3];

    public AutoDripScreen() {
        super(Text.literal("AutoDrip"));
    }

    private int panelX() { return (width - PANEL_W) / 2; }
    private int panelY() { return (height - PANEL_H) / 2; }

    private Text powerText() {
        return AutoDripClient.enabled
                ? Text.literal("● WŁĄCZONY").formatted(Formatting.GREEN, Formatting.BOLD)
                : Text.literal("● WYŁĄCZONY").formatted(Formatting.RED, Formatting.BOLD);
    }

    private Text speedText(AutoDripClient.Speed s) {
        return AutoDripClient.speed == s
                ? Text.literal("» " + s.label + " «").formatted(Formatting.GREEN, Formatting.BOLD)
                : Text.literal(s.label).formatted(Formatting.GRAY);
    }

    private Text autoCloseText() {
        return Text.literal("Auto-zamykanie trapdora: ")
                .append(AutoDripClient.autoClose
                        ? Text.literal("TAK").formatted(Formatting.GREEN)
                        : Text.literal("NIE").formatted(Formatting.RED));
    }

    private void refresh() {
        powerBtn.setMessage(powerText());
        autoCloseBtn.setMessage(autoCloseText());
        AutoDripClient.Speed[] all = AutoDripClient.Speed.values();
        for (int i = 0; i < all.length; i++) speedBtns[i].setMessage(speedText(all[i]));
    }

    @Override
    protected void init() {
        int x0 = panelX() + 10;
        int y0 = panelY();

        powerBtn = addDrawableChild(ButtonWidget.builder(powerText(), b -> {
            AutoDripClient.enabled = !AutoDripClient.enabled;
            refresh();
        }).dimensions(x0, y0 + 42, 250, 24).build());

        AutoDripClient.Speed[] all = AutoDripClient.Speed.values();
        for (int i = 0; i < all.length; i++) {
            final AutoDripClient.Speed s = all[i];
            speedBtns[i] = addDrawableChild(ButtonWidget.builder(speedText(s), b -> {
                AutoDripClient.speed = s;
                refresh();
            }).dimensions(x0 + i * 84, y0 + 90, 82, 22).build());
        }

        autoCloseBtn = addDrawableChild(ButtonWidget.builder(autoCloseText(), b -> {
            AutoDripClient.autoClose = !AutoDripClient.autoClose;
            refresh();
        }).dimensions(x0, y0 + 124, 250, 22).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> close())
                .dimensions(x0 + 75, y0 + 166, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int x = panelX(), y = panelY();

        // panel + obramowanie + pasek tytułowy
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, 0xE0101018);
        ctx.fill(x, y, x + PANEL_W, y + 30, 0xFF1B2A3A);
        ctx.fill(x, y + 30, x + PANEL_W, y + 31, 0xFF55FFFF);
        ctx.drawBorder(x, y, PANEL_W, PANEL_H, 0xFF55FFFF);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = panelX(), y = panelY();
        int cx = width / 2;

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("AutoDrip").formatted(Formatting.AQUA, Formatting.BOLD), cx, y + 7, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("automatyczna pułapka z dripstone").formatted(Formatting.GRAY), cx, y + 18, 0xFFFFFF);

        ctx.drawTextWithShadow(textRenderer,
                Text.literal("Prędkość").formatted(Formatting.GRAY), x + 12, y + 78, 0xFFFFFF);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Trafienia: " + AutoDripClient.triggers).formatted(Formatting.YELLOW),
                cx, y + 152, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
