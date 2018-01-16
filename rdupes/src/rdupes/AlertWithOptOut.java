package rdupes;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;

public class AlertWithOptOut extends Alert
{

	public final CheckBox optOut=new CheckBox();
	private  AlertWithOptOut(AlertType alertType) {
		super(alertType);
	}

	public static AlertWithOptOut createAlertWithOptOut(AlertType type, String title, String headerText, String message,
			String optOutMessage, ButtonType... buttonTypes) {
		AlertWithOptOut alert = new AlertWithOptOut(type);
		// Need to force the alert to layout in order to grab the graphic,
		// as we are replacing the dialog pane with a custom pane
		alert.getDialogPane().applyCss();
		Node graphic = alert.getDialogPane().getGraphic();
		// Create a new dialog pane that has a checkbox instead of the hide/show
		// details button
		// Use the supplied callback for the action of the checkbox
		alert.setDialogPane(new DialogPane() {
			@Override
			protected Node createDetailsButton() {
				alert.optOut.setText(optOutMessage);
				//optOut.setOnAction(e -> optOutAction.accept(optOut.isSelected()));
				return alert.optOut;
			}
		});
		alert.getDialogPane().getButtonTypes().addAll(buttonTypes);
		alert.getDialogPane().setContentText(message);
		// Fool the dialog into thinking there is some expandable content
		// a Group won't take up any space if it has no children
		alert.getDialogPane().setExpandableContent(new Group());
		alert.getDialogPane().setExpanded(true);
		// Reset the dialog graphic using the default style
		alert.getDialogPane().setGraphic(graphic);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		return alert;
	}

}
