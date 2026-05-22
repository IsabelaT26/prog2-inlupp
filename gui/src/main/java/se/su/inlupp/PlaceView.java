package se.su.inlupp;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class PlaceView {

    private static final double RADIUS = 20;
    private static final double LABEL_X_OFFSET = -20;
    private static final double LABEL_Y_OFFSET = -25;
    private Color normalColor;

    private final Place place;
    private final Circle circle;
    private final Text label;
    private final Group root;



    public PlaceView(Place place, Color normalColor) {
        this.place = place;
        this.normalColor = normalColor;

        circle = new Circle(place.getX(), place.getY(), RADIUS);
        circle.setFill(normalColor);

        label = new Text(place.getX() + LABEL_X_OFFSET, place.getY() + LABEL_Y_OFFSET, place.getName());
        label.setFill(normalColor);

        root = new Group(circle, label);
    }

    public Place getPlace() {
        return place;
    }

    public Group getRoot() {
        return root;
    }

    public Circle getCircle() {
        return circle;
    }

    public Text getLabel() {
        return label;
    }

    public void moveTo(double x, double y) {
        circle.setCenterX(x);
        circle.setCenterY(y);

        label.setX(x + LABEL_X_OFFSET);
        label.setY(y + LABEL_Y_OFFSET);

        place.setPosition(x, y);
    }

    public void setColour(Paint colour) {
        circle.setFill(colour);
        label.setFill(colour);
    }

    public void setNormal() {
        setColour(normalColor);
    }

    public void setSelected(Paint selectedColour) {
        setColour(selectedColour);
    }

    public void highlight(Paint highlightColour) {
        setColour(highlightColour);
    }
}
