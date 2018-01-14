package rdupes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class RDupesUI extends Application {

//	private final Node rootIcon = new ImageView(
//	// new Image(getClass().getResourceAsStream("folder_16.png"))
//	);
	static List<Path> l;
	public static void main(String[] args) {
		l=new ArrayList<>();
		for(String s: args)
		{
			Path p=Paths.get(s);
			l.add(p);
		}

		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		RDupes rd = new RDupes();
		try {
			rd.start(l);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			rd.initializeDone.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		primaryStage.setTitle("RDupes duplicate file finder program.");

		RDupesObjectTree rootItem = new RDupesObjectTree(rd);
		rootItem.childrenFill(true);
		rootItem.setExpanded(true);
		rootItem.expanded(true);
		TreeView<RDupesObject> tree = new TreeView<RDupesObject>(rootItem);
		tree.setCellFactory(a->new RDTreeCell());
		StackPane root = new StackPane();
		root.getChildren().add(tree);
		primaryStage.setScene(new Scene(root, 300, 250));
		primaryStage.show();
	}
}