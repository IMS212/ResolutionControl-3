package resolutioncontrol.util;

public final class Config {
	public double scaleFactor = 1;
	public ScalingAlgorithm upscaleAlgorithm = ScalingAlgorithm.NEAREST;
	public ScalingAlgorithm downscaleAlgorithm = ScalingAlgorithm.LINEAR;

	public static Config getInstance() {
		return ConfigHandler.instance.getConfig();
	}
	
	public static double getScaleFactor() {
		return getInstance().scaleFactor;
	}

	public static ScalingAlgorithm getUpscaleAlgorithm() {
		return getInstance().upscaleAlgorithm;
	}

	public static ScalingAlgorithm getDownscaleAlgorithm() {
		return getInstance().downscaleAlgorithm;
	}

}
