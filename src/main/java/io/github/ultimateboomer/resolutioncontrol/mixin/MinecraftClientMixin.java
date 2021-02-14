package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements ResolutionControlMod.MutableMinecraftClient {
	@Mutable
	@Final
	@Shadow
	private Framebuffer framebuffer;
	
	@Override
	public void setFramebuffer(Framebuffer framebuffer) {
		this.framebuffer = framebuffer;
	}

	@Inject(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/gl/Framebuffer"))
	private void onInitFramebuffer(CallbackInfo ci) {
		ResolutionControlMod mod = ResolutionControlMod.getInstance();
		if (mod.getScreenshotFramebufferAlwaysAllocated()) {
			mod.initScreenshotFramebuffer();
		}
	}
}
