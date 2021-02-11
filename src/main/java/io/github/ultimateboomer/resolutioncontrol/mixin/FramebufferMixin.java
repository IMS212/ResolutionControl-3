package io.github.ultimateboomer.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;

@Mixin(Framebuffer.class)
public abstract class FramebufferMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;texParameter(III)V"))
    private void onSetTexFilter(int target, int pname, int param) {
        if (pname == GL11.GL_TEXTURE_MIN_FILTER) {
            GlStateManager.texParameter(target, pname,
                    ResolutionControlMod.getInstance().getDownscaleAlgorithm().getId());
        } else if (pname == GL11.GL_TEXTURE_MAG_FILTER) {
            GlStateManager.texParameter(target, pname,
                    ResolutionControlMod.getInstance().getUpscaleAlgorithm().getId());
        } else if (pname == GL11.GL_TEXTURE_WRAP_S || pname == GL11.GL_TEXTURE_WRAP_T) {
            // Fix linear scaling creating black borders
            GlStateManager.texParameter(target, pname, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GlStateManager.texParameter(target, pname, param);
        }
    }
}
