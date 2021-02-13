package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.Nullable;

public class ScreenshotSettingsButton extends SettingsScreen {
    private static final double[] scaleValues = {0.1, 0.25, 0.5, 1.0,
            2.0, 3.0, 4.0, 6.0, 8.0, 16.0};

    private TextFieldWidget widthTextField;
    private TextFieldWidget heightTextField;

    private boolean manualEntry = false;

    protected ScreenshotSettingsButton(@Nullable Screen parent) {
        super(text("settings.screenshot.title"), parent);
    }

    @Override
    protected void init() {
        super.init();
        screenshotSettingsButton.active = false;

        int buttonSize = 20;
        int buttonOffset = buttonSize / 2;
        int buttonY = centerY + 15 - buttonSize / 2;
        int textFieldSize = 60;

        widthTextField = new TextFieldWidget(client.textRenderer,
                centerX - 15 - textFieldSize / 2, centerY - 36,
                textFieldSize, buttonSize,
                LiteralText.EMPTY);
        widthTextField.setText(String.valueOf(mod.getScreenshotWidth()));
        addButton(widthTextField);

        heightTextField = new TextFieldWidget(client.textRenderer,
                centerX - 15 - textFieldSize / 2, centerY - 16,
                textFieldSize, buttonSize,
                LiteralText.EMPTY);
        heightTextField.setText(String.valueOf(mod.getScreenshotHeight()));
        addButton(heightTextField);
    }
}
