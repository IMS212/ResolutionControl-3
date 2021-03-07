package io.github.ultimateboomer.resolutioncontrol.util;

public final class Config {
	public double scaleFactor = 1;

	public ScalingAlgorithm upscaleAlgorithm = ScalingAlgorithm.NEAREST;
	public ScalingAlgorithm downscaleAlgorithm = ScalingAlgorithm.LINEAR;

	public boolean overrideScreenshotScale = true;

	public int screenshotWidth = 3840;
	public int screenshotHeight = 2160;

	public boolean screenshotFramebufferAlwaysAllocated = false;

	public boolean enableDynamicResolution = false;
	public boolean fastDynamicResolution = false;

	public static Config getInstance() {
		return ConfigHandler.instance.getConfig();
	}

}
