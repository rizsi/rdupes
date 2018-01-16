package rdupes;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AlertHelp extends Alert {
	private AlertHelp(AlertType alertType) {
		super(alertType);
	}
	
	public static AlertHelp createAlertHelp(RDupesUI app, String title, String headerText, String message,
			ButtonType... buttonTypes) {
		AlertHelp alert = new AlertHelp(AlertType.INFORMATION);
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
				VBox ret = new VBox();
				ret.getChildren().add(new Label("This program is free software."));
				ret.getChildren().add(new Label("Created by Schmidt András."));
				String url = "https://github.com/rizsi/rdupes/";
				final Hyperlink hpl = new Hyperlink(url);
				hpl.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent e) {
						try {
							new ProcessBuilder("xdg-open", url).start();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});
				ret.getChildren().add(hpl);
				// optOut.setOnAction(e ->
				// optOutAction.accept(optOut.isSelected()));
				return ret;
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
