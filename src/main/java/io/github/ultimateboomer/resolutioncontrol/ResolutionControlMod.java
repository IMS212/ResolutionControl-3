package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.SettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.util.Config;
import io.github.ultimateboomer.resolutioncontrol.util.ConfigHandler;
import io.github.ultimateboomer.resolutioncontrol.util.ScalingAlgorithm;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class ResolutionControlMod implements ModInitializer {
	public static final String MOD_ID = "resolutioncontrol";
	
	public static Identifier identifier(String path) {
		return new Identifier(MOD_ID, path);
	}
	
	private static final MinecraftClient client = MinecraftClient.getInstance();
	
	private static ResolutionControlMod instance;
	
	public static ResolutionControlMod getInstance() {
		return instance;
	}

	private static final String SCREENSHOT_PREFIX = "fb";
	
	private KeyBinding settingsKey;
	private KeyBinding screenshotKey;
	
	private boolean shouldScale = false;
	
	@Nullable
	private Framebuffer framebuffer;
	
	@Nullable
	private Framebuffer clientFramebuffer;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;
	
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		instance = this;
		
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.resolutioncontrol.settings",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				"key.categories.resolutioncontrol"));

		screenshotKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.resolutioncontrol.screenshot",
				InputUtil.Type.KEYSYM,
				-1,
				"key.categories.resolutioncontrol"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (settingsKey.wasPressed()) {
				client.openScreen(new SettingsScreen());
			}


		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (screenshotKey.wasPressed()) {
				if (framebuffer == null) return;
				if (client.player == null) return;

				ScreenshotUtils.saveScreenshot(client.runDirectory,
						SCREENSHOT_PREFIX + ScreenshotUtils.getScreenshotFilename(null),
						framebuffer.textureWidth, framebuffer.textureHeight, framebuffer,
						text -> client.player.sendMessage(text, false));
			}
		});
	}
	
	public void setShouldScale(boolean shouldScale) {
		if (shouldScale == this.shouldScale) return;
		
//		if (getScaleFactor() == 1) return;
		
		Window window = getWindow();
		if (framebuffer == null) {
			this.shouldScale = true; // so we get the right dimensions
			framebuffer = new Framebuffer(
				window.getFramebufferWidth(),
				window.getFramebufferHeight(),
				true,
				MinecraftClient.IS_SYSTEM_MAC
			);
		}
		
		this.shouldScale = shouldScale;
		
		client.getProfiler().swap(shouldScale ? "startScaling" : "finishScaling");
		
		// swap out framebuffers as needed
		if (shouldScale) {
			clientFramebuffer = client.getFramebuffer();
			setClientFramebuffer(framebuffer);
			framebuffer.beginWrite(true);
			// nothing on the client's framebuffer yet
		} else {
			setClientFramebuffer(clientFramebuffer);
			client.getFramebuffer().beginWrite(true);
			framebuffer.draw(
				window.getFramebufferWidth(),
				window.getFramebufferHeight()
			);
		}
		
		client.getProfiler().swap("level");
	}
	
	public double getScaleFactor() {
		return Config.getScaleFactor();
	}
	
	public void setScaleFactor(double scaleFactor) {
//		if (scaleFactor == Config.getScaleFactor()) return;
		
		Config.getInstance().scaleFactor = scaleFactor;
		
		updateFramebufferSize();
		
		ConfigHandler.instance.saveConfig();
	}

	public ScalingAlgorithm getUpscaleAlgorithm() {
		return Config.getUpscaleAlgorithm();
	}

	public void setUpscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getUpscaleAlgorithm()) return;

		Config.getInstance().upscaleAlgorithm = algorithm;

		updateFramebufferSize();

		ConfigHandler.instance.saveConfig();
	}

	public void nextUpscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getUpscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setUpscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setUpscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}

	public ScalingAlgorithm getDownscaleAlgorithm() {
		return Config.getDownscaleAlgorithm();
	}

	public void setDownscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getDownscaleAlgorithm()) return;

		Config.getInstance().downscaleAlgorithm = algorithm;

		updateFramebufferSize();

		ConfigHandler.instance.saveConfig();
	}

	public void nextDownscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getDownscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setDownscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setDownscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}
	
	public double getCurrentScaleFactor() {
		return shouldScale ? Config.getScaleFactor() : 1;
	}
	
	public void onResolutionChanged() {
		updateFramebufferSize();
	}
	
	@SuppressWarnings("ConstantConditions")
	private void updateFramebufferSize() {
		if (framebuffer == null) return;
		
//		if (getScaleFactor() != 1) {
//
//		}
		// resize if not unused
		resize(framebuffer);
		
		resize(client.worldRenderer.getEntityOutlinesFramebuffer());
	}
	
	public void resize(Framebuffer framebuffer) {
		boolean prev = shouldScale;
		shouldScale = true;
		framebuffer.resize(
			getWindow().getFramebufferWidth(),
			getWindow().getFramebufferHeight(),
			MinecraftClient.IS_SYSTEM_MAC
		);
		shouldScale = prev;

		currentWidth = framebuffer.textureWidth;
		currentHeight = framebuffer.textureHeight;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
	}
	
	private Window getWindow() {
		return client.getWindow();
	}
	
	private void setClientFramebuffer(Framebuffer framebuffer) {
		((MutableMinecraftClient) client).setFramebuffer(framebuffer);
	}

	public KeyBinding getSettingsKey() {
		return settingsKey;
	}

	public int getCurrentWidth() {
		return currentWidth;
	}

	public int getCurrentHeight() {
		return currentHeight;
	}

	public long getEstimatedMemory() {
		return estimatedMemory;
	}

	public interface MutableMinecraftClient {
		void setFramebuffer(Framebuffer framebuffer);
	}
}
