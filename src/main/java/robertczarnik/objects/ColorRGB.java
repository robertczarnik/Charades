package robertczarnik.objects;

import javafx.scene.paint.Color;

import java.io.Serializable;

public class ColorRGB implements Serializable {
    private int R;
    private int G;
    private int B;

    public ColorRGB(Color color) {
        R = (int)(color.getRed()*255);
        G = (int)(color.getGreen()*255);
        B = (int)(color.getBlue()*255);
    }

    public int getR() {
        return R;
    }

    public int getG() {
        return G;
    }

    public int getB() {
        return B;
    }
}
