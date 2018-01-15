package rdupes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
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
//		int cores = Math.max(Runtime.getRuntime().availableProcessors(), 1);
		new RDupesStage(rd, primaryStage).launch();
		primaryStage.show();
		try {
			rd.start(1, l);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}