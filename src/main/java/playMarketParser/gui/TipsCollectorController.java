package playMarketParser.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import playMarketParser.Global;
import playMarketParser.Prefs;
import playMarketParser.tipsCollector.Tip;
import playMarketParser.tipsCollector.TipsCollector;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

import static playMarketParser.Global.showAlert;


public class TipsCollectorController implements Initializable, TipsCollector.TipsLoadingListener {

    @FXML
    private Button addQueriesBtn;
    @FXML
    private Button importQueriesBtn;
    @FXML
    private Button clearBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button resumeBtn;
    @FXML
    private CheckBox titleFirstChb;
    @FXML
    private Label queriesCntLbl;
    @FXML
    private Label tipsCntLbl;
    @FXML
    private Label progLbl;
    @FXML
    private ProgressBar progBar;
    @FXML
    private TableView<String> inputTable;
    @FXML
    private TableColumn<String, String> inputQueryCol;
    @FXML
    private TableView<Tip> outputTable;
    @FXML
    private TableColumn<Tip, String> outputQueryCol;
    @FXML
    private TableColumn<Tip, String> tipCol;
    @FXML
    private TableColumn<Tip, Integer> depthCol;
    @FXML
    private VBox rootPane;

    private MenuItem removeItem;
    private ResourceBundle rb;
    private TipsCollector tipsCollector;

    private ObservableList<String> queries = FXCollections.observableArrayList();
    private ObservableList<Tip> tips = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rb = Global.getBundle();

        titleFirstChb.setSelected(Prefs.getBoolean("title_first"));

        //inputTable
        inputQueryCol.prefWidthProperty().bind(inputTable.widthProperty().multiply(1));
        inputQueryCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()));
        inputTable.setItems(queries);
        //outputTable
        outputQueryCol.prefWidthProperty().bind(outputTable.widthProperty().multiply(0.4));
        tipCol.prefWidthProperty().bind(outputTable.widthProperty().multiply(0.5));
        depthCol.prefWidthProperty().bind(outputTable.widthProperty().multiply(0.1));
        outputQueryCol.setCellValueFactory(new PropertyValueFactory<>("queryText"));
        tipCol.setCellValueFactory(new PropertyValueFactory<>("text"));
        depthCol.setCellValueFactory(new PropertyValueFactory<>("depth"));
        outputTable.setItems(tips);

        //Context menu
        TableContextMenu inputTableContextMenu = new TableContextMenu(inputTable);
        removeItem = inputTableContextMenu.getRemoveItem();
        TableContextMenu outputTableContextMenu = new TableContextMenu(outputTable);
        outputTableContextMenu.getRemoveItem().setVisible(false);

        queriesCntLbl.textProperty().bind(Bindings.size(queries).asString());

        enableReadyMode();
    }

    @FXML
    private void addQueries() {
        TextAreaDialog dialog = new TextAreaDialog("", rb.getString("enterQueries"), rb.getString("addingQueries"), "");

        Optional result = dialog.showAndWait();
        if (result.isPresent())
            Arrays.stream(((String) result.get()).split("\\r?\\n"))
                    .distinct()
                    .forEachOrdered(s -> queries.add(s));
    }

    @FXML
    private void importQueries() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(rb.getString("txtDescr"), "*.txt"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(rb.getString("csvDescr"), "*.csv"));
        fileChooser.setInitialDirectory(Global.getInitDir("input_path"));
        File inputFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (inputFile == null) return;
        Prefs.put("input_path", inputFile.getParentFile().toString());
        Prefs.put("title_first", titleFirstChb.isSelected());

        try (Stream<String> lines = Files.lines(inputFile.toPath(), StandardCharsets.UTF_8)) {
            lines.skip(titleFirstChb.isSelected() ? 1 : 0)
                    .distinct()
                    .forEachOrdered(s -> queries.add(s));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(rb.getString("error"), rb.getString("unableToReadFile"));
        }
    }

    @FXML
    private void exportResults() {
        if (tips == null || tips.size() == 0) {
            showAlert(rb.getString("error"), rb.getString("noResults"));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName(rb.getString("outTips"));
        fileChooser.setInitialDirectory(Global.getInitDir("output_path"));
        File outputFile = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (outputFile == null) return;
        if (!outputFile.getParentFile().canWrite()) {
            showAlert(rb.getString("error"), rb.getString("cantWrite"));
            return;
        }

        Prefs.put("output_path", outputFile.getParentFile().toString());

        try (PrintStream ps = new PrintStream(new FileOutputStream(outputFile))) {
            //��������� ��������� ����� UTF-8
            ps.write('\ufeef');
            ps.write('\ufebb');
            ps.write('\ufebf');

            //��������� ���������
            String firstRow = rb.getString("query") + Global.getCsvDelim() + rb.getString("tip")
                    + Global.getCsvDelim() + rb.getString("depth") + "\n";
            Files.write(outputFile.toPath(), firstRow.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

            List<String> newContent = new ArrayList<>();
            for (Tip tip : tips)
                newContent.add(tip.getQueryText() + Global.getCsvDelim() + tip.getText() + Global.getCsvDelim() + tip.getDepth());
            Files.write(outputFile.toPath(), newContent, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            showAlert(rb.getString("saved"), rb.getString("fileSaved"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showAlert(rb.getString("error"), rb.getString("alreadyUsing"));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(rb.getString("error"), rb.getString("fileNotSaved"));
        }
    }

    @FXML
    private void start() {
        if (queries.size() == 0) {
            showAlert(rb.getString("error"), rb.getString("noQueries"));
            return;
        }
        outputTable.getItems().clear();

        tipsCollector = new TipsCollector(queries,this);
        tipsCollector.setMaxThreadsCount(Prefs.getInt("tips_threads_cnt"));
        tipsCollector.setMaxDepth(Prefs.getInt("tips_parsing_depth"));
        tipsCollector.setAlphaType(Prefs.getString("alphabet"));
        if (!Prefs.getString("tips_lang").equals("-")) tipsCollector.setLanguage(Prefs.getString("tips_lang"));
        if (!Prefs.getString("tips_country").equals("-")) tipsCollector.setCountry(Prefs.getString("tips_country"));
        enableLoadingMode();
        Global.log(rb.getString("tipsStarted") + "\n" +
                String.format("%-25s%s%n", rb.getString("threadsCount"), Prefs.getInt("tips_threads_cnt")) +
                String.format("%-25s%s%n", rb.getString("tipsParsingDepth"), Prefs.getInt("tips_parsing_depth")) +
                String.format("%-25s%s%n", rb.getString("timeout"), Prefs.getInt("timeout")) +
                String.format("%-25s%s%n", rb.getString("proxy"), Prefs.getString("proxy")) +
                String.format("%-25s%s%n", rb.getString("acceptLang"), Prefs.getString("accept_language")) +
                String.format("%-25s%s%n", rb.getString("userAgent"), Prefs.getString("user_agent"))
        );
        tipsCollector.start();
    }

    @FXML
    private void pause() {
        tipsCollector.pause();
    }

    @FXML
    private void resume() {
        Global.log(rb.getString("tipsResumed"));
        enableLoadingMode();
        tipsCollector.start();
    }

    @FXML
    private void stop() {
        tipsCollector.stop();
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
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stopBtn.setDisable(true);
        removeItem.setDisable(false);
    }

    private void enableLoadingMode() {
        addQueriesBtn.setDisable(true);
        importQueriesBtn.setDisable(true);
        titleFirstChb.setDisable(true);
        clearBtn.setDisable(true);
        exportBtn.setDisable(true);
        startBtn.setDisable(true);
        pauseBtn.setDisable(false);
        resumeBtn.setDisable(true);
        stopBtn.setDisable(false);
        removeItem.setDisable(true);
    }

    private void enableCompleteMode() {
        addQueriesBtn.setDisable(false);
        importQueriesBtn.setDisable(false);
        titleFirstChb.setDisable(false);
        clearBtn.setDisable(false);
        exportBtn.setDisable(false);
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
        resumeBtn.setDisable(true);
        stopBtn.setDisable(true);
        removeItem.setDisable(false);
    }

    private void enablePauseMode() {
        addQueriesBtn.setDisable(true);
        importQueriesBtn.setDisable(true);
        titleFirstChb.setDisable(true);
        clearBtn.setDisable(true);
        exportBtn.setDisable(false);
        startBtn.setDisable(true);
        pauseBtn.setDisable(true);
        resumeBtn.setDisable(false);
        stopBtn.setDisable(false);
        removeItem.setDisable(true);
    }

    @Override
    public synchronized void onQueryProcessed(List<Tip> collectedTips, String queryText, boolean isSuccess) {
        if (!isSuccess) Global.log(String.format("%-30s%s", queryText, rb.getString("connTimeout")));
        tips.addAll(collectedTips);
        Platform.runLater(() -> tipsCntLbl.setText(String.valueOf(tips.size())));

        progBar.setProgress(tipsCollector.getProgress());
        Platform.runLater(() -> progLbl.setText(String.format("%.1f", tipsCollector.getProgress() * 100) + "%"));
    }

    @Override
    public void onPause() {
        Global.log(rb.getString("tipsPaused"));
        enablePauseMode();
    }

    @Override
    public void onFinish() {
        Global.log(rb.getString("tipsComplete"));
        enableCompleteMode();
        progBar.setProgress(tipsCollector.getProgress());
        Platform.runLater(() -> progLbl.setText(String.format("%.1f", tipsCollector.getProgress() * 100) + "%"));
    }
}
