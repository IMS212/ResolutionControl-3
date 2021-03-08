package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import io.github.ultimateboomer.resolutioncontrol.util.DynamicResolutionHandler;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
	@Shadow
	private Framebuffer entityOutlinesFramebuffer;

	@Shadow @Nullable public Framebuffer cloudsFramebuffer;

//	@Inject(at = @At("RETURN"), method = "loadEntityOutlineShader")
//	private void onLoadEntityOutlineShader(CallbackInfo ci) {
//		ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
//	}
//
//	@Inject(at = @At("RETURN"), method = "onResized")
//	private void onOnResized(CallbackInfo ci) {
//		if (entityOutlinesFramebuffer == null) return;
//		ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
//	}
//
//	@Inject(at = @At("RETURN"), method = "loadTransparencyShader")
//	private void onLoadTransparencyShader(CallbackInfo ci) {
//		ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
//	}
//
//	@Inject(method = "getEntityOutlinesFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetEntityOutlineFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	@Inject(method = "getTranslucentFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetTranslucentFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	@Inject(method = "getEntityFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetEntityFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	@Inject(method = "getParticlesFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetParticlesFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	@Inject(method = "getWeatherFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetWeatherFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	@Inject(method = "getCloudsFramebuffer", at = @At("RETURN"), cancellable = true)
//	private void onGetCloudsFramebuffer(CallbackInfoReturnable<Framebuffer> ci) {
//		handleGetFramebuffer(ci);
//	}
//
//	private void handleGetFramebuffer(CallbackInfoReturnable<Framebuffer> fb) {
//		if (fb.getReturnValue() != null && ResolutionControlMod.getInstance().getEnableFastDynamicResolution()) {
//			fb.setReturnValue(DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(fb.getReturnValue()));
//		}
//	}

	@Redirect(method = "render", at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/render/WorldRenderer;entityOutlinesFramebuffer:Lnet/minecraft/client/gl/Framebuffer;"))
	private Framebuffer onRender1(WorldRenderer renderer) {
		if (ResolutionControlMod.getInstance().getEnableFastDynamicResolution()) {
			return DynamicResolutionHandler.INSTANCE.getCurrentFramebuffer(this.entityOutlinesFramebuffer);
		} else {
			return this.entityOutlinesFramebuffer;
		}
	}

}
