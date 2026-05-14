package se.su.inlupp;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class ConnectionView {

    private final Place place1;
    private final Place place2;

    private final Circle circle1;
    private final Circle circle2;

    private final Line line;
    private final Text label;
    private final Group root;

    public ConnectionView(
            Place place1,
            Place place2,
            Circle circle1,
            Circle circle2,
            String connectionName,
            Paint color
    ) {
        this.place1 = place1;
        this.place2 = place2;
        this.circle1 = circle1;
        this.circle2 = circle2;

        line = new Line();
        line.startXProperty().bind(circle1.centerXProperty());
        line.startYProperty().bind(circle1.centerYProperty());
        line.endXProperty().bind(circle2.centerXProperty());
        line.endYProperty().bind(circle2.centerYProperty());
        line.setStroke(color);
        line.setStrokeWidth(3);

        label = new Text(connectionName);
        label.setFill(color);

        label.xProperty().bind(
                line.startXProperty().add(line.endXProperty()).divide(2)
        );

        label.yProperty().bind(
                line.startYProperty().add(line.endYProperty()).divide(2)
        );

        root = new Group(line, label);
    }

    public Group getRoot() {
        return root;
    }

    public boolean connects(Place a, Place b) {
        return (place1.equals(a) && place2.equals(b))
                || (place1.equals(b) && place2.equals(a));
    }

    public boolean comesFrom(Place place){
        return place1.equals(place) || place2.equals(place);
    }
}
