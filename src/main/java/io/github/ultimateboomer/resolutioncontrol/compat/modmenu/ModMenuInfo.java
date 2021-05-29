package io.github.ultimateboomer.resolutioncontrol.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;

public final class ModMenuInfo implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MainSettingsScreen::new;
	}
}
