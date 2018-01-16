package rdupes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import hu.qgears.commons.UtilFile;
import javafx.application.Application;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class RDupesUI extends Application {

	static List<Path> l;

	public static void main(String[] args) {
		l = new ArrayList<>();
		for (String s : args) {
			Path p = Paths.get(s);
			l.add(p);
		}

		launch(args);
	}

	RDupes rd = new RDupes();

	@Override
	public void start(Stage primaryStage) {
		File f=new File(System.getProperty("user.home")+"/"+".rdupes");
		boolean skip=false;
		try {
			if("accepted".equals(UtilFile.loadAsString(f)))
			{
				skip=true;
			}
		} catch (Exception e1) {
		}
		if(!skip)
		{
			AlertWithOptOut alert = AlertWithOptOut.createAlertWithOptOut(AlertType.CONFIRMATION, "WARNING", "Using the tool may lead to data loss! Do you accept this?",
					"Creator of the program is not responsible for any data loss as a result of using the program.",
					"I accept it and don't want to see this message again! (Saves selection to: "+f+")", ButtonType.YES, ButtonType.NO);
			ButtonType ret=alert.showAndWait().get();
			if(ret==ButtonType.NO)
			{
				return;
			}
			if(alert.optOut.isSelected())
			{
				try {
					UtilFile.saveAsFile(f, "accepted");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		// int cores = Math.max(Runtime.getRuntime().availableProcessors(), 1);
		new RDupesStage(this, rd, primaryStage).launch();
		primaryStage.show();
		try {
			rd.start(1, l);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
