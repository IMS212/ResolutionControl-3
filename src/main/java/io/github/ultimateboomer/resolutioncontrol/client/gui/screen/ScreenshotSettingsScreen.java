package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

public class ScreenshotSettingsScreen extends SettingsScreen {
    private static final double[] scaleValues = {0.1, 0.25, 0.5, 1.0,
            2.0, 3.0, 4.0, 6.0, 8.0, 16.0};

    private static final Text increaseText = new LiteralText("x2");
    private static final Text decreaseText = new LiteralText("/2");

    private TextFieldWidget widthTextField;
    private TextFieldWidget heightTextField;

    private ButtonWidget increaseButton;
    private ButtonWidget decreaseButton;

    private ButtonWidget toggleOverrideSizeButton;

    private final int buttonSize = 20;
    private final int textFieldSize = 60;

    public ScreenshotSettingsScreen(@Nullable Screen parent) {
        super(text("settings.screenshot"), parent);
    }

    public ScreenshotSettingsScreen() {
        this(MinecraftClient.getInstance().currentScreen);
    }

    @Override
    protected void init() {
        super.init();

        toggleOverrideSizeButton = new ButtonWidget(
                centerX + 20, centerY - 40,
                50, 20,
                this.getOverrideSizeText(),
                button -> {
                    mod.setOverrideScreenshotScale(!mod.getOverrideScreenshotScale());
                    button.setMessage(this.getOverrideSizeText());
                }
        );
        addButton(toggleOverrideSizeButton);

        widthTextField = new TextFieldWidget(client.textRenderer,
                centerX - 35 - textFieldSize / 2, centerY ,
                textFieldSize, buttonSize,
                LiteralText.EMPTY);
        widthTextField.setText(String.valueOf(mod.getScreenshotWidth()));
        addButton(widthTextField);

        heightTextField = new TextFieldWidget(client.textRenderer,
                centerX - 25 + textFieldSize / 2, centerY ,
                textFieldSize, buttonSize,
                LiteralText.EMPTY);
        heightTextField.setText(String.valueOf(mod.getScreenshotHeight()));
        addButton(heightTextField);

        increaseButton = new ButtonWidget(
                centerX - 10 - 40, centerY + 24,
                20, 20,
                increaseText,
                button -> multiply(2.0));
        addButton(increaseButton);

        decreaseButton = new ButtonWidget(
                centerX + 10 - 40, centerY + 24,
                20, 20,
                decreaseText,
                button -> multiply(0.5));
        addButton(decreaseButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawLeftAlignedString(matrices,
                "\u00a78" + text("settings.screenshot.overrideSize").getString(),
                centerX - 65, centerY - 35,
                0x000000);

        drawLeftAlignedString(matrices,
                "\u00a78" + text("settings.screenshot.size").getString(),
                centerX - 65, centerY - 12,
                0x000000);
    }

    @Override
    public void tick() {
        widthTextField.tick();
        heightTextField.tick();
        super.tick();
    }

    @Override
    protected void applySettingsAndCleanup() {
        if (NumberUtils.isParsable(widthTextField.getText())
                && NumberUtils.isParsable(heightTextField.getText())) {
            mod.setScreenshotWidth((int) Double.parseDouble(widthTextField.getText()));
            mod.setScreenshotHeight((int) Double.parseDouble(heightTextField.getText()));
        }
        super.applySettingsAndCleanup();
    }

    private void multiply(double mul) {
        if (NumberUtils.isParsable(widthTextField.getText())
                && NumberUtils.isParsable(heightTextField.getText())) {
            widthTextField.setText(String.valueOf((int) (Double.parseDouble(widthTextField.getText()) * mul)));
            heightTextField.setText(String.valueOf((int) (Double.parseDouble(heightTextField.getText()) * mul)));
        }
    }

    private Text getOverrideSizeText() {
        return mod.getOverrideScreenshotScale() ? new TranslatableText("addServer.resourcePack.enabled")
                : new TranslatableText("addServer.resourcePack.disabled");
    }
}
