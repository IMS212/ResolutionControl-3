package resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import resolutioncontrol.ResolutionControlMod;
import resolutioncontrol.util.Config;


public final class SettingsScreen extends Screen {
	private static final Identifier backgroundTexture = ResolutionControlMod.identifier("textures/gui/settings.png");

	private static final double[] scaleValues = {0.1, 0.25, 0.5, 0.75, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 4.0, 8.0};
	
	private static Text text(String path, Object... args) {
		return new TranslatableText("screen." + ResolutionControlMod.MOD_ID + ".settings." + path, args);
	}
	
	private final int containerWidth = 192;
	private final int containerHeight = 128;
	
	private final ResolutionControlMod mod = ResolutionControlMod.getInstance();
	
	@Nullable
	private final Screen parent;
	
	private ButtonWidget increaseButton;
	private ButtonWidget decreaseButton;

	private ButtonWidget upscaleAlgoButton;
	private ButtonWidget downscaleAlgoButton;

	private ButtonWidget doneButton;
	
	private int centerX;
	private int centerY;
	private int startX;
	private int startY;
	
	public SettingsScreen(Screen parent) {
		super(text("title"));
		
		this.parent = parent;
	}
	
	public SettingsScreen() {
		this(MinecraftClient.getInstance().currentScreen);
	}
	
	@Override
	protected void init() {
		super.init();
		
		centerX = width / 2;
		centerY = height / 2;
		startX = centerX - containerWidth / 2;
		startY = centerY - containerHeight / 2;
		
		int buttonSize = 20;
		int buttonOffset = buttonSize / 2;
		int buttonY = centerY + 5 - buttonSize / 2;
		
		decreaseButton = new ButtonWidget(
			centerX - 65 - buttonOffset - buttonSize / 2, buttonY,
			buttonSize, buttonSize,
			new LiteralText("-"),
			button -> changeScaleFactor(false));
		addButton(decreaseButton);
		
		increaseButton = new ButtonWidget(
			centerX - 65 + buttonOffset - buttonSize / 2, buttonY,
			buttonSize, buttonSize,
				new LiteralText("+"),
			button -> changeScaleFactor(true)
		);
		addButton(increaseButton);

		upscaleAlgoButton = new ButtonWidget(
			centerX + 5, centerY - 28,
			60, buttonSize,
			Config.getUpscaleAlgorithm().getText(),
			button -> {
				ResolutionControlMod.getInstance().nextUpscaleAlgorithm();
				button.setMessage(Config.getUpscaleAlgorithm().getText());
			}
		);
		addButton(upscaleAlgoButton);

		downscaleAlgoButton = new ButtonWidget(
				centerX + 5, centerY + 8,
				60, buttonSize,
				Config.getDownscaleAlgorithm().getText(),
				button -> {
					ResolutionControlMod.getInstance().nextDownscaleAlgorithm();
					button.setMessage(Config.getDownscaleAlgorithm().getText());
				}
		);
		addButton(downscaleAlgoButton);
		
		doneButton = new ButtonWidget(
			centerX - 80, startY + containerHeight - 30,
			60, buttonSize,
			new TranslatableText("gui.done"),
			button -> client.openScreen(parent)
		);
		addButton(doneButton);
		
		updateButtons();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ResolutionControlMod.getInstance().getSettingsKeyBinding().matchesKey(keyCode, scanCode)) {
			this.client.openScreen(null);
			this.client.mouse.lockCursor();
			return true;
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float time) {
		assert client != null;
		
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
		
		drawCenteredString(matrices, getTitle().getString(), centerX, startY + 10, 0x404040);
		
		String scaleFactor = String.format("\u00a7%s%s\u00a7rx",
				mod.getScaleFactor() > 2.0 ? "4" : "0", mod.getScaleFactor());
		drawCenteredString(matrices, scaleFactor, centerX - 65, centerY - 34, 0x000000);

		drawLeftAlignedString(matrices, "Upscale:", centerX + 5, centerY - 40, 0x000000);
		drawLeftAlignedString(matrices, "Downscale:", centerX + 5, centerY - 5, 0x000000);
		
		super.render(matrices, mouseX, mouseY, time); // buttons
	}
	
	private void drawCenteredString(MatrixStack matrices, String text, int x, int y, int color) {
		textRenderer.draw(matrices, text, x - textRenderer.getWidth(text) / 2, y, color);
	}

	private void drawLeftAlignedString(MatrixStack matrices, String text, int x, int y, int color) {
		textRenderer.draw(matrices, text, x, y, color);
	}

	private void drawRightAlignedString(MatrixStack matrices, String text, int x, int y, int color) {
		textRenderer.draw(matrices, text, x - textRenderer.getWidth(text), y, color);
	}
	
	private void changeScaleFactor(boolean add) {
		double currentScale = mod.getScaleFactor();
		int nextIndex = ArrayUtils.indexOf(scaleValues, currentScale);
		if (nextIndex == -1) {
			for (int i = -1; i <= scaleValues.length; ++i) {
				double scale1 = i == -1 ? 0.0 : scaleValues[i - 1];
				double scale2 = i == scaleValues.length ? Double.POSITIVE_INFINITY : scaleValues[i];

				if (currentScale > scale1 && currentScale < scale2) {
					nextIndex = i + (add ? 1 : 0);
					break;
				}
			}
		} else {
			nextIndex += add ? 1 : -1;
		}

		mod.setScaleFactor(scaleValues[nextIndex]);

		updateButtons();
	}
	
	private void updateButtons() {
		increaseButton.active = mod.getScaleFactor() < scaleValues[scaleValues.length - 1];
		decreaseButton.active = mod.getScaleFactor() > scaleValues[0];
	}
}
