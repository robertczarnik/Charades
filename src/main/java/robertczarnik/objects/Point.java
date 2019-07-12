package robertczarnik.objects;
import java.io.Serializable;

public class Point implements Serializable {
    private double x;
    private double y;
    private int size;
    private boolean singlePoint;

    public Point(double x, double y, int size, boolean singlePoint){
        this.x=x;
        this.y=y;
        this.size=size;
        this.singlePoint=singlePoint;
    }

    //GETTERS
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public boolean isSinglePoint() {
        return singlePoint;
    }
}
