package playMarketParser.gui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import playMarketParser.Global;
import playMarketParser.Prefs;
import playMarketParser.tipsCollector.Query;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static playMarketParser.Global.showAlert;


public class TipsCollectorController implements Initializable {

    @FXML private Button addQueriesBtn;
    @FXML private Button importQueriesBtn;
    @FXML private Button clearBtn;
    @FXML private Button exportBtn;
    @FXML private Button startBtn;
    @FXML private Button abortBtn;
    @FXML private CheckBox titleFirstChb;
    @FXML private TableView<String> inputTable;
    @FXML private TableColumn<String, String> inputQueryCol;
    @FXML private TableView<Query> outputTable;
    @FXML private TableColumn outputQueryCol;
    @FXML private TableColumn tipCol;
    @FXML private VBox rootPane;

    private ResourceBundle rb;
    private Prefs prefs;

    private ObservableList<String> inputQueries = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rb = Global.getBundle();
        prefs = new Prefs();
        enableReadyMode();

        titleFirstChb.setSelected(prefs.getBoolean("title_first"));

        //inputTable
        inputQueryCol.prefWidthProperty().bind(inputTable.widthProperty().multiply(1));
        inputQueryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
        inputTable.setItems(inputQueries);
        //outputTable
        outputQueryCol.prefWidthProperty().bind(outputTable.widthProperty().multiply(0.4));
        tipCol.prefWidthProperty().bind(outputTable.widthProperty().multiply(0.6));
    }

    @FXML
    private void addQueries() {
        TextAreaDialog dialog = new TextAreaDialog("", rb.getString("enterQueries"), rb.getString("addingQueries"), "");

        Optional result = dialog.showAndWait();
        if (result.isPresent()) {
            System.out.println(result.get());
            Arrays.stream(((String) result.get()).split("\\r?\\n"))
                    .distinct()
                    .forEachOrdered(s -> inputQueries.add(s));
        }
    }

    @FXML
    private void importQueries() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(rb.getString("txtDescr"), "*.txt"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(rb.getString("csvDescr"), "*.csv"));
        fileChooser.setInitialDirectory(Global.getInitDir(prefs, "input_path"));
        File inputFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (inputFile == null) return;
        prefs.put("input_path", inputFile.getParentFile().toString());
        prefs.put("title_first", titleFirstChb.isSelected());

        try (Stream<String> lines = Files.lines(inputFile.toPath(), StandardCharsets.UTF_8)) {
            lines.skip(titleFirstChb.isSelected() ? 1 : 0)
                    .distinct()
                    .forEachOrdered(s -> inputQueries.add(s));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(rb.getString("error"), rb.getString("unableToReadFile"));
        }
    }

    @FXML
    private void exportResults() {

    }

    @FXML
    private void startTipsCollecting() {

    }

    @FXML
    private void abortTipsCollecting() {

    }

    @FXML
    private void clearQueries() {
        inputTable.getItems().clear();
        enableReadyMode();
    }

    private void enableReadyMode() {
        addQueriesBtn.setDisable(false);
        importQueriesBtn.setDisable(false);
        titleFirstChb.setDisable(false);
        clearBtn.setDisable(false);
        exportBtn.setDisable(true);
        startBtn.setManaged(true);
        abortBtn.setManaged(false);
    }

    private void enableLoadingMode() {
        addQueriesBtn.setDisable(true);
        importQueriesBtn.setDisable(true);
        titleFirstChb.setDisable(true);
        clearBtn.setDisable(true);
        exportBtn.setDisable(true);
        startBtn.setManaged(false);
        abortBtn.setManaged(true);
    }

    private void enableCompleteMode() {
        addQueriesBtn.setDisable(false);
        importQueriesBtn.setDisable(false);
        titleFirstChb.setDisable(false);
        clearBtn.setDisable(false);
        exportBtn.setDisable(false);
        startBtn.setManaged(true);
        abortBtn.setManaged(false);
    }
}
