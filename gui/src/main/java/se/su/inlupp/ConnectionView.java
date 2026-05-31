package se.su.inlupp;

import javafx.scene.Group;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class ConnectionView {

    private final PlaceView placeView1;
    private final PlaceView placeView2;

    private final Line line;
    private final Text label;
    private final Group root;

    public ConnectionView(
            PlaceView placeView1,
            PlaceView placeView2,
            String connectionName,
            Paint colour
    ) {
        this.placeView1 = placeView1;
        this.placeView2 = placeView2;

        line = new Line();
        line.startXProperty().bind(placeView1.getCircle().centerXProperty());
        line.startYProperty().bind(placeView1.getCircle().centerYProperty());
        line.endXProperty().bind(placeView2.getCircle().centerXProperty());
        line.endYProperty().bind(placeView2.getCircle().centerYProperty());
        line.setStroke(colour);
        line.setStrokeWidth(3);

        label = new Text(connectionName);
        label.setFill(colour);

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
        Place place1 = placeView1.getPlace();
        Place place2 = placeView2.getPlace();
        return (place1.equals(a) && place2.equals(b))
                || (place1.equals(b) && place2.equals(a));
    }

    public boolean comesFrom(Place place) {
        return placeView1.getPlace().equals(place) || placeView2.getPlace().equals(place);
    }

    public void highlight(Paint colour) {
        line.setStroke(colour);
        label.setFill(colour);
    }

    public void unhighlight(Paint colour) {
        line.setStroke(colour);
        label.setFill(colour);
    }
}
