package rdupes;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class RDupesStage {
	private RDupes rd;
	private Stage primaryStage;
	private RDupesUI app;
	public RDupesStage(RDupesUI app, RDupes rd, Stage primaryStage) {
		this.app=app;
		this.rd = rd;
		this.primaryStage = primaryStage;
	}
	private Label selectedInfo;

	public void launch() {
		primaryStage.setTitle("RDupes duplicate file finder program.");

		RDupesObjectTree rootItem = new RDupesObjectTree(rd);
		rootItem.childrenFill(true);
		rootItem.setExpanded(true);
		rootItem.expanded(true);
		TreeView<RDupesObject> tree = new TreeView<RDupesObject>(rootItem);
		tree.setCellFactory(a -> new RDTreeCell());
		tree.getSelectionModel().selectedItemProperty().addListener(e->updateSelected(tree.getSelectionModel().getSelectedItem()));
		installDropListener(tree);
		StackPane root = new StackPane();
		root.getChildren().add(tree);
		MenuBar menu = createMenu();
		Button heap = new Button();
		heap.setOnAction(e -> System.gc());
		Label statusbar = new Label();
		VBox.setVgrow(root, Priority.ALWAYS);
		TextField nCores = new TextField();
		nCores.setPromptText("Number of hashing threads");
		nCores.textProperty().addListener(x -> setNCores(nCores.getText()));
		nCores.setMinWidth(150);
		HBox tools = new HBox(heap, nCores);
		selectedInfo=new Label(" ");
		VBox box = new VBox(menu, root, selectedInfo, statusbar, tools);
		primaryStage.setScene(new Scene(box, 640, 480));
		Timeline updateStatus = new Timeline(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				statusbar.setText((rd.tasks.get() == 0 ? "DONE " : "WORKING... ") + " Files indexed: "
						+ rd.filesProcessed.get() + " in " + rd.foldersProcessed.get() + " folders ongoing hash: "
						+ rd.nBytesToHahs.get() + " bytes in " + rd.nFileToHash.get() + " files");
				long free = Runtime.getRuntime().freeMemory();
				long total = Runtime.getRuntime().totalMemory();
				long max = Runtime.getRuntime().maxMemory();
				long alloc = total - free;
				int divisor = Math.max(rd.foldersProcessed.get()+rd.filesProcessed.get(), 1);
				heap.setText("Program Memory usage: " + formatMemory(alloc) + " bytes of max: " + formatMemory(max)
						+ " bytes/file: " + (alloc / divisor));
				updateSelected(tree.getSelectionModel().getSelectedItem());
			}
		}));
		updateStatus.setCycleCount(Timeline.INDEFINITE);
		updateStatus.play();
		
		primaryStage.getScene().getStylesheets().add(getClass().getResource("rdupes.css").toString());

	}

	private Object updateSelected(TreeItem<RDupesObject> treeItem) {
		if(treeItem!=null&&treeItem.getValue()!=null)
		{
			selectedInfo.setText(""+treeItem.getValue().getStringInfo());
		}
		return null;
	}

	private void installDropListener(TreeView<RDupesObject> tree) {
		tree.setOnDragOver(new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				if (event.getGestureSource() != tree && event.getDragboard().hasFiles()) {
					event.acceptTransferModes(TransferMode.LINK);
				}

				event.consume();
			}
		});
		tree.setOnDragDropped(new EventHandler<DragEvent>() {
			public void handle(DragEvent event) {
				List<File> fs = event.getDragboard().getFiles();
				boolean success = false;
				if (fs != null) {
					success = true;
					for (File f : fs) {
						if(validateSelectedFile(f))
						{
							rd.addFolder(f);
						}else
						{
							success=false;
						}
					}
				}
				event.setDropCompleted(success);
				event.consume();
			}
		});
	}

	private Object setNCores(String text) {
		try {
			rd.setNCores(Integer.parseInt(text));
		} catch (NumberFormatException e) {
		}
		return null;
	}

	DirectoryChooser addDirChooser = new DirectoryChooser();

	private MenuBar createMenu() {
		MenuBar menuBar = new MenuBar();

		// --- Menu File
		Menu menuFile = new Menu("File");

		MenuItem add = new MenuItem("Add root folder...");
		add.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				addDirChooser.setTitle("Add folder to find duplicates");
				File selected = addDirChooser.showDialog(primaryStage);
				if (selected != null) {
					if (validateSelectedFile(selected)) {
						rd.addFolder(selected);
					}
				}
			}
		});
		MenuItem newWindow = new MenuItem("Create new window");
		newWindow.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Stage secondStage = new Stage();
				new RDupesStage(app, rd, secondStage).launch();
				secondStage.show();
			}
		});
		menuFile.getItems().addAll(add, newWindow);

		
		// --- Menu File
		Menu menuHelp = new Menu("Help");

		MenuItem about = new MenuItem("About");
		about.setOnAction(e->showHelp());
		menuHelp.getItems().addAll(about);

		
		menuBar.getMenus().addAll(menuFile, menuHelp);
		return menuBar;
	}

	private Object showHelp() {
		AlertHelp.createAlertHelp(app, "RDupes application", "version: 0.0.0", "", ButtonType.OK).showAndWait();
		return null;
	}

	protected boolean validateSelectedFile(File selected) {
		if(!selected.isDirectory()||!selected.exists())
		{
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Parameter is not a folder");
			alert.setHeaderText("Parameter is not a folder");
			alert.setContentText(selected.getAbsolutePath());
			alert.showAndWait();
			return false;
		}
		for(RDupesObject root: rd.getChildren())
		{
			RDupesFolder r=(RDupesFolder) root;
			try {
				String p1=r.file.toFile().getCanonicalPath()+File.separator;
				String p2=selected.getCanonicalPath()+File.separator;
				if(p1.startsWith(p2) || p2.startsWith(p1))
				{
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle("Error adding root folder");
					alert.setHeaderText("The new root folder has a containment relation with an existing root: "+" "+selected+" against: "+r.file);
					alert.setContentText("The new root folder has a containment relation with an existing root: "+" "+selected+" against: "+r.file);
					alert.showAndWait();
					return false;
				}
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Error validating folder to add");
				alert.setHeaderText("Error vlaidating folder to add: "+" "+selected+" against: "+r.file);
				alert.setContentText(e.getLocalizedMessage());
				alert.showAndWait();
				return false;
			}
		}
		return true;
	}

	private static DecimalFormat df = new DecimalFormat("##.###");

	public static String formatMemory(long m) {
		long gig = 1024l * 1024l * 1024l;
		long meg = 1024l * 1024l;
		long kilo = 1024l;
		if (m >= gig) {
			double v = ((double) m) / gig;
			return df.format(v) + "Gib";
		}
		if (m >= meg) {
			double v = ((double) m) / meg;
			return df.format(v) + "Mib";
		}
		if (m >= kilo) {
			double v = ((double) m) / kilo;
			return df.format(v) + "Kib";
		}
		return "" + m + " bytes";
	}

}
