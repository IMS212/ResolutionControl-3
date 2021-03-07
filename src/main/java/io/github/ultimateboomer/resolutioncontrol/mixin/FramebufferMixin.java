package io.github.ultimateboomer.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import io.github.ultimateboomer.resolutioncontrol.util.ConfigHandler;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL45;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

@Mixin(Framebuffer.class)
public abstract class FramebufferMixin {
    @Unique private boolean isMipmapped;

    @Shadow public abstract int getColorAttachment();

    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texParameter(III)V"))
    private void onSetTexFilter(int target, int pname, int param) {
        if (pname == GL11.GL_TEXTURE_MIN_FILTER) {
            GlStateManager.texParameter(target, pname,
                    ResolutionControlMod.getInstance().getUpscaleAlgorithm().getId(isMipmapped));
        } else if (pname == GL11.GL_TEXTURE_MAG_FILTER) {
            GlStateManager.texParameter(target, pname,
                    ResolutionControlMod.getInstance().getDownscaleAlgorithm().getId(isMipmapped));
        } else if (pname == GL11.GL_TEXTURE_WRAP_S || pname == GL11.GL_TEXTURE_WRAP_T) {
            // Fix linear scaling creating black borders
            GlStateManager.texParameter(target, pname, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GlStateManager.texParameter(target, pname, param);
        }
    }

    @Redirect(method = "initFbo", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texImage2D(IIIIIIIILjava/nio/IntBuffer;)V"))
    private void onTexImage(int target, int level, int internalFormat, int width, int height, int border, int format,
                            int type, IntBuffer pixels) {
        if (ConfigHandler.instance.getConfig().scaleFactor > 2.0) {
            int mipmapLevel = MathHelper.ceil(Math.log(ConfigHandler.instance.getConfig().scaleFactor)
                    / Math.log(2));
            for (int i = 0; i < mipmapLevel; i++) {
                GlStateManager.texImage2D(target, 0, internalFormat, width, height, border, format, type, pixels);
            }

            isMipmapped = true;
        } else {
            GlStateManager.texImage2D(target, 0, internalFormat, width, height, border, format, type, pixels);
            isMipmapped = false;
        }

    }

    @Inject(method = "drawInternal", at = @At("HEAD"))
    private void onDraw(int width, int height, boolean bl, CallbackInfo ci) {
        if (isMipmapped) {
            GlStateManager.bindTexture(this.getColorAttachment());
            GL45.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        }
    }
}
