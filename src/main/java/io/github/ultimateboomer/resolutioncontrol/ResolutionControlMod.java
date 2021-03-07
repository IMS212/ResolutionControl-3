package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.SettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.client.util.Window;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

	private boolean optifineInstalled;
	
	private KeyBinding settingsKey;
	private KeyBinding screenshotKey;
	
	private boolean shouldScale = false;
	
	@Nullable
	private Framebuffer framebuffer;

	@Nullable
	private Framebuffer screenshotFrameBuffer;
	
	@Nullable
	private Framebuffer clientFramebuffer;

	private Set<Framebuffer> scaledFramebuffers;

	private Set<Framebuffer> minecraftFramebuffers;

	private Class<? extends SettingsScreen> lastSettingsScreen = MainSettingsScreen.class;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;

	private boolean screenshot = false;
	
	@Override
	public void onInitialize() {
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
				client.openScreen(SettingsScreen.getScreen(lastSettingsScreen));
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (screenshotKey.wasPressed()) {
				if (getOverrideScreenshotScale()) {
					this.screenshot = true;
					client.player.sendMessage(
							new TranslatableText("resolutioncontrol.screenshot.wait"), false);
				} else {
					saveScreenshot(framebuffer);
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (ConfigHandler.instance.getConfig().enableDynamicResolution && client.world != null) {
				DynamicResolutionHandler.INSTANCE.tick();
			}
		});

		optifineInstalled = FabricLoader.getInstance().isModLoaded("optifabric");
	}

	private void saveScreenshot(Framebuffer fb) {
		ScreenshotUtils.saveScreenshot(client.runDirectory,
				RCUtil.getScreenshotFilename(client.runDirectory).toString(),
				fb.textureWidth, fb.textureHeight, fb,
				text -> client.player.sendMessage(text, false));
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
			calculateSize();
		}

		if (getEnableFastDynamicResolution()) {
			if (DynamicResolutionHandler.INSTANCE.isFramebufferMapEmpty()) {
				initScaledFramebuffers();

				DynamicResolutionHandler.INSTANCE.generateFrameBuffers(
						getWindow().getFramebufferWidth(), getWindow().getFramebufferHeight(), scaledFramebuffers);
			}

		}
		
		this.shouldScale = shouldScale;
		
		client.getProfiler().swap(shouldScale ? "startScaling" : "finishScaling");
		
		// swap out framebuffers as needed
		if (shouldScale) {
			clientFramebuffer = client.getFramebuffer();

			if (screenshot) {
				resizeMinecraftFramebuffers();

				if (!isScreenshotFramebufferAlwaysAllocated() && screenshotFrameBuffer != null) {
					screenshotFrameBuffer.delete();
				}

				if (screenshotFrameBuffer == null) {
					initScreenshotFramebuffer();
				}

				setClientFramebuffer(screenshotFrameBuffer);

				screenshotFrameBuffer.beginWrite(true);
			} else {
				setClientFramebuffer(getFramebuffer());

				getFramebuffer().beginWrite(true);
			}
			// nothing on the client's framebuffer yet
		} else {
			setClientFramebuffer(clientFramebuffer);
			client.getFramebuffer().beginWrite(true);

			// Screenshot framebuffer
			if (screenshot) {
				saveScreenshot(screenshotFrameBuffer);

				if (!isScreenshotFramebufferAlwaysAllocated()) {
					screenshotFrameBuffer.delete();
					screenshotFrameBuffer = null;
				}

				screenshot = false;
				resizeMinecraftFramebuffers();
			} else {
				getFramebuffer().draw(
						window.getFramebufferWidth(),
						window.getFramebufferHeight()
				);
			}
		}
		
		client.getProfiler().swap("level");
	}

	private void initScaledFramebuffers() {
		if (scaledFramebuffers != null) {
			scaledFramebuffers.clear();
		} else {
			scaledFramebuffers = new HashSet<>();
		}

		scaledFramebuffers.add(framebuffer);
		initMinecraftFramebuffers();
		scaledFramebuffers.addAll(minecraftFramebuffers);
		scaledFramebuffers.remove(null);
	}

	private void initMinecraftFramebuffers() {
		if (minecraftFramebuffers != null) {
			minecraftFramebuffers.clear();
		} else {
			minecraftFramebuffers = new HashSet<>();
		}

		minecraftFramebuffers.add(client.worldRenderer.getEntityOutlinesFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getTranslucentFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getEntityFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getParticlesFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getWeatherFramebuffer());
		minecraftFramebuffers.add(client.worldRenderer.getCloudsFramebuffer());
		minecraftFramebuffers.remove(null);
	}

	public Framebuffer getFramebuffer() {
		if (getEnableFastDynamicResolution()) {
			if (DynamicResolutionHandler.INSTANCE.isFramebufferMapEmpty()) {
				DynamicResolutionHandler.INSTANCE.generateFrameBuffers(
						getWindow().getFramebufferWidth(), getWindow().getFramebufferHeight(), scaledFramebuffers);
			}

			return DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(framebuffer);
		} else {
			return framebuffer;
		}
	}

	public void initScreenshotFramebuffer() {
		if (Objects.nonNull(screenshotFrameBuffer)) screenshotFrameBuffer.delete();

		screenshotFrameBuffer = new Framebuffer(
				getScreenshotWidth(), getScreenshotHeight(),
				true, MinecraftClient.IS_SYSTEM_MAC);
	}
	
	public double getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}
	
	public void setScaleFactor(double scaleFactor) {
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
		return shouldScale ?
				Config.getInstance().enableDynamicResolution ?
						DynamicResolutionHandler.INSTANCE.getCurrentScale() : Config.getInstance().scaleFactor : 1;
	}

	public boolean getOverrideScreenshotScale() {
		return Config.getInstance().overrideScreenshotScale;
	}

	public void setOverrideScreenshotScale(boolean value) {
		Config.getInstance().overrideScreenshotScale = value;
		if (value && isScreenshotFramebufferAlwaysAllocated()) {
			initScreenshotFramebuffer();
		} else {
			if (screenshotFrameBuffer != null) {
				screenshotFrameBuffer.delete();
				screenshotFrameBuffer = null;
			}
		}
	}

	public int getScreenshotWidth() {
		return Math.max(Config.getInstance().screenshotWidth, 1);
	}

	public void setScreenshotWidth(int width) {
		Config.getInstance().screenshotWidth = width;
	}

	public int getScreenshotHeight() {
		return Math.max(Config.getInstance().screenshotHeight, 1);
	}

	public void setScreenshotHeight(int height) {
		Config.getInstance().screenshotHeight = height;
	}

	public boolean isScreenshotFramebufferAlwaysAllocated() {
		return Config.getInstance().screenshotFramebufferAlwaysAllocated;
	}

	public void setScreenshotFramebufferAlwaysAllocated(boolean value) {
		Config.getInstance().screenshotFramebufferAlwaysAllocated = value;

		if (value) {
			if (getOverrideScreenshotScale() && Objects.isNull(this.screenshotFrameBuffer)) {
				initScreenshotFramebuffer();
			}
		} else {
			if (this.screenshotFrameBuffer != null) {
				this.screenshotFrameBuffer.delete();
				this.screenshotFrameBuffer = null;
			}
		}
	}

	public void setEnableDynamicResolution(boolean enableDynamicResolution) {
		Config.getInstance().enableDynamicResolution = enableDynamicResolution;

		if (enableDynamicResolution) {

		}
	}

	public boolean getEnableFastDynamicResolution() {
		return Config.getInstance().enableDynamicResolution && Config.getInstance().fastDynamicResolution;
	}

	public void onResolutionChanged() {
		if (getWindow() != null && getEnableFastDynamicResolution()) {
			DynamicResolutionHandler.INSTANCE.generateFrameBuffers(
					getWindow().getFramebufferWidth(), getWindow().getFramebufferHeight(), scaledFramebuffers);
		}

		updateFramebufferSize();
	}
	
	public void updateFramebufferSize() {
		if (framebuffer == null) return;

		resize(framebuffer);
		resizeMinecraftFramebuffers();

		calculateSize();
	}

	public void resizeMinecraftFramebuffers() {
		initMinecraftFramebuffers();

		if (getEnableFastDynamicResolution()) {
			client.worldRenderer.entityOutlinesFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.entityOutlinesFramebuffer);
			client.worldRenderer.translucentFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.translucentFramebuffer);
			client.worldRenderer.entityFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.entityFramebuffer);
			client.worldRenderer.particlesFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.particlesFramebuffer);
			client.worldRenderer.weatherFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.weatherFramebuffer);
			client.worldRenderer.cloudsFramebuffer
					= DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(
							client.worldRenderer.cloudsFramebuffer);
		} else {
			minecraftFramebuffers.forEach(this::resize);
		}

	}

	public void calculateSize() {
		currentWidth = framebuffer.textureWidth;
		currentHeight = framebuffer.textureHeight;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
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
			if (getEnableFastDynamicResolution()) {
				framebuffer.resize(
						getWindow().getWidth(),
						getWindow().getHeight(),
						MinecraftClient.IS_SYSTEM_MAC
				);
			} else {
				framebuffer.resize(
						getWindow().getFramebufferWidth(),
						getWindow().getFramebufferHeight(),
						MinecraftClient.IS_SYSTEM_MAC
				);
			}
		}
		shouldScale = prev;
	}
	
	private Window getWindow() {
		return client.getWindow();
	}
	
	private void setClientFramebuffer(Framebuffer framebuffer) {
		client.framebuffer = framebuffer;
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

	public boolean isOptifineInstalled() {
		return optifineInstalled;
	}

	public void saveSettings() {
		ConfigHandler.instance.saveConfig();
	}

	public void setLastSettingsScreen(Class<? extends SettingsScreen> ordinal) {
		this.lastSettingsScreen = ordinal;
	}

}
