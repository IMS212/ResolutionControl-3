package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {
    protected static final Identifier backgroundTexture = ResolutionControlMod.identifier("textures/gui/settings.png");

    protected static Text text(String path, Object... args) {
        return new TranslatableText(ResolutionControlMod.MOD_ID + "." + path, args);
    }

    protected static final int containerWidth = 192;
    protected static final int containerHeight = 128;

    protected final ResolutionControlMod mod = ResolutionControlMod.getInstance();

    @Nullable
    protected final Screen parent;

    protected int centerX;
    protected int centerY;
    protected int startX;
    protected int startY;

    protected ButtonWidget mainSettingsButton;
    protected ButtonWidget screenshotSettingsButton;

    protected List<ButtonWidget> menuButtons;

    protected ButtonWidget doneButton;

    protected SettingsScreen(Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        centerX = width / 2;
        centerY = height / 2;
        startX = centerX - containerWidth / 2;
        startY = centerY - containerHeight / 2;

        // Init menu buttons
        menuButtons = new ArrayList<>();
        final int menuButtonWidth = 60;
        final int menuButtonHeight = 20;
        int o = 0;

        this.mainSettingsButton = new ButtonWidget(
                startX - menuButtonWidth - 20, startY + o,
                menuButtonWidth, menuButtonHeight,
                text("settings.main"),
                button -> client.openScreen(new MainSettingsScreen(this.parent)));
        menuButtons.add(mainSettingsButton);
        o += 25;

        this.screenshotSettingsButton = new ButtonWidget(
                startX - menuButtonWidth - 20, startY + o,
                menuButtonWidth, menuButtonHeight,
                text("settings.screenshot"),
                button -> client.openScreen(new ScreenshotSettingsButton(this.parent)));
        menuButtons.add(screenshotSettingsButton);
        o += 25;

        menuButtons.forEach(this::addButton);

        doneButton = new ButtonWidget(
                centerX - 15, startY + containerHeight - 30,
                60, 20,
                new TranslatableText("gui.done"),
                button -> client.openScreen(parent)
        );
        addButton(doneButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (client.world == null) {
            renderBackgroundTexture(0);
        }

        GlStateManager.enableAlphaTest();
        client.getTextureManager().bindTexture(backgroundTexture);
        GlStateManager.color4f(1, 1, 1, 1);

        int textureWidth = 256;
        int textureHeight = 192;
        drawTexture(
                matrices,
                centerX - textureWidth / 2, centerY - textureHeight / 2,
                0, 0,
                textureWidth, textureHeight
        );

        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredString(matrices, getTitle().getString(), centerX, startY + 10, 0x404040);
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    protected void drawCenteredString(MatrixStack matrices, String text, int x, int y, int color) {
        textRenderer.draw(matrices, text, x - textRenderer.getWidth(text) / 2, y, color);
    }

    protected void drawLeftAlignedString(MatrixStack matrices, String text, int x, int y, int color) {
        textRenderer.draw(matrices, text, x, y, color);
    }

    protected void drawRightAlignedString(MatrixStack matrices, String text, int x, int y, int color) {
        textRenderer.draw(matrices, text, x - textRenderer.getWidth(text), y, color);
    }
}
