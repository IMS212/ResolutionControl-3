package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;
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
	private boolean screenshotShouldScale = false;
	
	@Nullable
	private Framebuffer framebuffer;

	@Nullable
	private Framebuffer screenshotFrameBuffer;
	
	@Nullable
	private Framebuffer clientFramebuffer;

	@Nullable
	private Framebuffer clientFramebuffer2;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;

	private boolean screenshot = false;
	
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
				client.openScreen(new MainSettingsScreen());
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (screenshotKey.wasPressed()) {
				this.screenshot = true;
			}
		});
	}
	
	public void setShouldScale(boolean shouldScale) {
		if (shouldScale == this.shouldScale) return;
		
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

			if (screenshot) {
				if (screenshotFrameBuffer != null) {
					screenshotFrameBuffer.delete();
				}

				resizeMinecraftFramebuffers();

				screenshotFrameBuffer = new Framebuffer(
						getScreenshotWidth(), getScreenshotHeight(),
						true, MinecraftClient.IS_SYSTEM_MAC);

				setClientFramebuffer(screenshotFrameBuffer);

				screenshotFrameBuffer.beginWrite(true);
			} else {
				setClientFramebuffer(framebuffer);

				framebuffer.beginWrite(true);
			}
			// nothing on the client's framebuffer yet
		} else {
			setClientFramebuffer(clientFramebuffer);
			client.getFramebuffer().beginWrite(true);

			// Screenshot framebuffer
			if (screenshot) {
				screenshotFrameBuffer.draw(
						window.getFramebufferWidth(), window.getFramebufferHeight()
				);

				ScreenshotUtils.saveScreenshot(client.runDirectory,
						SCREENSHOT_PREFIX + ScreenshotUtils.getScreenshotFilename(null),
						framebuffer.textureWidth, framebuffer.textureHeight, screenshotFrameBuffer,
						text -> client.player.sendMessage(text, false));

				screenshotFrameBuffer.delete();
				screenshotFrameBuffer = null;
				screenshot = false;
				resizeMinecraftFramebuffers();
			} else {
				framebuffer.draw(
						window.getFramebufferWidth(),
						window.getFramebufferHeight()
				);
			}
		}
		
		client.getProfiler().swap("level");
	}
	
	public double getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}
	
	public void setScaleFactor(double scaleFactor) {
//		if (scaleFactor == Config.getScaleFactor()) return;
		
		Config.getInstance().scaleFactor = scaleFactor;
		
		updateFramebufferSize();
		
		ConfigHandler.instance.saveConfig();
	}

	public ScalingAlgorithm getUpscaleAlgorithm() {
		return Config.getInstance().upscaleAlgorithm;
	}

	public void setUpscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().upscaleAlgorithm) return;

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
		return Config.getInstance().downscaleAlgorithm;
	}

	public void setDownscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().downscaleAlgorithm) return;

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
		return shouldScale ? Config.getInstance().scaleFactor : 1;
	}

	public boolean getUsingIndependentScreenshotScale() {
		return Config.getInstance().independentScreenshotScale;
	}

	public int getScreenshotWidth() {
		return Config.getInstance().screenshotWidth;
	}

	public void setScreenshotWidth(int width) {
		Config.getInstance().screenshotWidth = width;
	}

	public int getScreenshotHeight() {
		return Config.getInstance().screenshotHeight;
	}

	public void setScreenshotHeight(int height) {
		Config.getInstance().screenshotHeight = height;
	}
	
	public void onResolutionChanged() {
		updateFramebufferSize();
	}
	
	@SuppressWarnings("ConstantConditions")
	private void updateFramebufferSize() {
		if (framebuffer == null) return;

		resize(framebuffer);
		
		resizeMinecraftFramebuffers();

		currentWidth = framebuffer.textureWidth;
		currentHeight = framebuffer.textureHeight;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
	}

	public void resizeMinecraftFramebuffers() {
		resize(client.worldRenderer.getEntityOutlinesFramebuffer());
		resize(client.worldRenderer.getTranslucentFramebuffer());
		resize(client.worldRenderer.getEntityFramebuffer());
		resize(client.worldRenderer.getParticlesFramebuffer());
		resize(client.worldRenderer.getWeatherFramebuffer());
		resize(client.worldRenderer.getCloudsFramebuffer());
	}
	
	public void resize(@Nullable Framebuffer framebuffer) {
		if (framebuffer == null) return;

		boolean prev = shouldScale;
		shouldScale = true;
		if (screenshot) {
			framebuffer.resize(
					getScreenshotWidth(),
					getScreenshotHeight(),
					MinecraftClient.IS_SYSTEM_MAC
			);
		} else {
			framebuffer.resize(
					getWindow().getFramebufferWidth(),
					getWindow().getFramebufferHeight(),
					MinecraftClient.IS_SYSTEM_MAC
			);
		}
		shouldScale = prev;


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

	public boolean isScreenshotting() {
		return screenshot;
	}

	public interface MutableMinecraftClient {
		void setFramebuffer(Framebuffer framebuffer);
	}
}
