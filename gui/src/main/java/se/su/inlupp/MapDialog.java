package se.su.inlupp;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class MapDialog{

    public Optional<String> askForPlaceName() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Name");
        dialog.setHeaderText("Enter a name for the place");
        dialog.setContentText("Name:");

        return dialog.showAndWait();
    }

    public Optional<RoadInfo> askForRoadInfo() {
        Dialog<RoadInfo> dialog = new Dialog<>();

        dialog.setTitle("Road Info");
        dialog.setHeaderText("Enter road information");

        TextField nameField = new TextField();
        TextField distanceField = new TextField();

        VBox box = new VBox(
                10,
                new Label("Road name:"),
                nameField,
                new Label("Distance:"),
                distanceField
        );

        dialog.getDialogPane().setContent(box);

        ButtonType okButton = new ButtonType(
                "OK",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                okButton,
                ButtonType.CANCEL
        );

        dialog.setResultConverter(button -> {
            if (button == okButton) {
                String name = nameField.getText().trim();

                if (name.isEmpty()) {
                    showError("Road name cannot be empty.");
                    return null;
                }

                try {
                    int distance = Integer.parseInt(distanceField.getText().trim());

                    if (distance <= 0) {
                        showError("Distance must be greater than 0.");
                        return null;
                    }

                    return new RoadInfo(name, distance);

                } catch (NumberFormatException e) {
                    showError("Distance must be a number.");
                    return null;
                }
            }

            return null;
        });

        return dialog.showAndWait();
    }

    public void showError(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(errorMessage);

        alert.showAndWait();
    }

    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    public boolean confirmDiscardUnsavedChanges() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to continue and discard them?");

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public boolean confirmRemovePlace(String placeName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle("Remove place");
        alert.setHeaderText("Remove " + placeName + "?");
        alert.setContentText("This will also remove all roads connected to this place.");

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
    }
}