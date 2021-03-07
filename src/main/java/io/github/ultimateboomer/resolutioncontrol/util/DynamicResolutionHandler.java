package io.github.ultimateboomer.resolutioncontrol.util;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicResolutionHandler {
    public static final DynamicResolutionHandler INSTANCE = new DynamicResolutionHandler();

    private final Map<Framebuffer, List<Framebuffer>> frameBufferMap = new HashMap<>();
    private final FloatList scales = new FloatArrayList();
    private int baseScale;

    private int timer = 0;
    private int currentScale;

    private DynamicResolutionHandler() {
        for (float i = 0.5f; i <= 2.0f; i += 0.125f) {
            scales.add(i);

            if (i == 1.0f) {
                baseScale = scales.size() - 1;
                currentScale = baseScale;
            }
        }
    }

    public void tick() {
        timer++;

        if (timer > 10) {
            timer = 0;
            update();
        }
    }

    private void update() {
        MinecraftClient client = MinecraftClient.getInstance();

        int fps = ((ResolutionControlMod.MutableMinecraftClient) client).getFps();

        if (fps > 130) {
            currentScale = Math.min(currentScale + 1, scales.size() - 1);
            ResolutionControlMod.getInstance().updateFramebufferSize();
        } else if (fps < 110) {
            currentScale = Math.max(currentScale - 1, 0);
            ResolutionControlMod.getInstance().updateFramebufferSize();
        }

        System.out.println("Res: " + getCurrentScale());
    }

    public double getCurrentScale() {
        return scales.getFloat(currentScale);
    }

    public Framebuffer getCurrentFramebuffer(Framebuffer key) {
        List<Framebuffer> list = frameBufferMap.get(key);
        if (list == null) {
            return null;
        } else {
            return list.get(currentScale);
        }
    }

    public void generateFrameBuffers(int baseWidth, int baseHeight, Collection<Framebuffer> fbs) {
        frameBufferMap.forEach((framebuffer, list) -> list.forEach(Framebuffer::delete));
        frameBufferMap.clear();

        fbs.forEach(framebuffer -> {
            List<Framebuffer> framebufferList = new ObjectArrayList<>(scales.size());
            for (int i = 0; i < scales.size(); i++) {
                float scale = scales.getFloat(i);
                framebufferList.add(new Framebuffer(
                        (int) Math.max(baseWidth * scale, 1), (int) Math.max(baseHeight * scale, 1),
                        true, true));
            }
            frameBufferMap.put(framebuffer, framebufferList);
        });
    }

    public boolean isFramebufferMapEmpty() {
        return frameBufferMap.isEmpty();
    }
}
