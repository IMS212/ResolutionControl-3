package resolutioncontrol.util;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.Scanner;

public enum ScalingAlgorithm {
    NEAREST(new LiteralText("Nearest"), GL11.GL_NEAREST),
    LINEAR(new LiteralText("Linear"), GL11.GL_LINEAR);

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
