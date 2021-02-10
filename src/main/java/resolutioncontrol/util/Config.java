package resolutioncontrol.util;

public final class Config {
	public static Config getInstance() {
		return ConfigHandler.instance.getConfig();
	}
	
	public static double getScaleFactor() {
		return getInstance().scaleFactor;
	}
	
	public double scaleFactor = 1;
}
