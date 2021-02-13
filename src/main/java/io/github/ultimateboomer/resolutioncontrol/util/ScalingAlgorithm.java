package io.github.ultimateboomer.resolutioncontrol.util;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.lwjgl.opengl.GL11;

public enum ScalingAlgorithm {
    NEAREST(new TranslatableText("resolutioncontrol.scaling.nearest"), GL11.GL_NEAREST),
    LINEAR(new TranslatableText("resolutioncontrol.scaling.linear"), GL11.GL_LINEAR);

    private Text text;
    private int id;

    private ScalingAlgorithm next;

    ScalingAlgorithm(Text text, int id) {
        this.text = text;
        this.id = id;
        this.next = next;
    }

    public Text getText() {
        return text;
    }

    public int getId() {
        return id;
    }
}
