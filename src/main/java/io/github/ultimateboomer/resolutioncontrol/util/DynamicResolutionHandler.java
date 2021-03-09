package io.github.ultimateboomer.resolutioncontrol.util;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.text.LiteralText;

import java.util.*;

public class DynamicResolutionHandler {
    public static final DynamicResolutionHandler INSTANCE = new DynamicResolutionHandler();

    private final Map<Framebuffer, List<Framebuffer>> frameBufferMap = new HashMap<>();
    private final FloatList scales = new FloatArrayList();
    private int baseScale;

    private int timer = 10;
    private int currentScale;

    private int lastWidth;
    private int lastHeight;

    private DynamicResolutionHandler() {
        reset();
    }

    public void tick() {
        timer--;

        if (timer <= 0) {
            update();
        }
    }

    public void reset() {
        for (float i = Config.getInstance().drMinScale; i <= Config.getInstance().drMaxScale;
             i += Config.getInstance().drResStep) {
            scales.add(i);

            if (i == 1.0f) {
                baseScale = scales.size() - 1;
                currentScale = baseScale;
            }
        }
    }

    private void update() {
        MinecraftClient client = MinecraftClient.getInstance();

        final int smoothAmount = Config.getInstance().drFpsSmoothAmount;
        float sum = 0;
        for (int i = client.metricsData.getCurrentIndex() - smoothAmount;
             i < client.metricsData.getCurrentIndex(); i++) {

            sum += client.metricsData.getSamples()[Math.floorMod(i, 240)];
        }
        float fps = 1_000_000_000.0f / (sum / smoothAmount);

        if (fps > Config.getInstance().drMaxFps) {
            setCurrentScale(Math.min(currentScale + 1, scales.size() - 1));
            timer = 15;
        } else if (fps < Config.getInstance().drMinFps) {
            setCurrentScale(Math.max(currentScale - 1, 0));
            timer = 5;
        } else {
            timer = 3;
        }
    }

    private void setCurrentScale(int currentScale) {
        boolean equal = this.currentScale == currentScale;
        this.currentScale = currentScale;

        if (!equal) {
            ResolutionControlMod.getInstance().updateFramebufferSize();
            MinecraftClient.getInstance().player.sendMessage(
                    new LiteralText("Res: " + getCurrentScale()), false);
        }
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
        if (lastWidth != baseWidth && lastHeight != baseHeight) {
            frameBufferMap.forEach((framebuffer, list) -> list.forEach(Framebuffer::delete));
            frameBufferMap.clear();

            fbs.forEach(framebuffer -> {
                System.out.println(framebuffer.textureWidth + " " + framebuffer.textureHeight);

                List<Framebuffer> framebufferList = new ObjectArrayList<>(scales.size());
                for (int i = 0; i < scales.size(); i++) {
                    float scale = scales.getFloat(i);

                    Framebuffer newFb = new Framebuffer(
                            (int) Math.max(baseWidth * scale, 1), (int) Math.max(baseHeight * scale, 1),
                            true, false);
                    framebufferList.add(newFb);
                }
                frameBufferMap.put(framebuffer, framebufferList);
            });

            lastWidth = baseWidth;
            lastHeight = baseHeight;
        }
    }

    public boolean isFramebufferMapEmpty() {
        return frameBufferMap.isEmpty();
    }
}
