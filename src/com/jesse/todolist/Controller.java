package com.jesse.todolist;

import com.jesse.todolist.dataModel.TodoData;
import com.jesse.todolist.dataModel.TodoItem;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Controller {
    private List<TodoItem> todoItems;
    private FilteredList<TodoItem> filteredList;

    @FXML
    private ListView<TodoItem> todoListView;

    @FXML
    private TextArea itemDetailsTextArea;

    @FXML
    private Label deadlineLabel;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private ContextMenu listContextMenu;

    @FXML
    private ToggleButton filterToggleButton;


    public void initialize() {
        listContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");

        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TodoItem todoItem = todoListView.getSelectionModel().getSelectedItem();
                deleteItem(todoItem);
            }
        });

        MenuItem markMenuItem = new MenuItem("Mark");
        listContextMenu.getItems().addAll(deleteMenuItem,markMenuItem);
        todoListView.getSelectionModel().selectedItemProperty().addListener((observableValue, todoItem, t1) -> {
            if(t1 != null) {
                TodoItem item5 = todoListView.getSelectionModel().getSelectedItem();
                itemDetailsTextArea.setText(item5.getDetails());
                DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                deadlineLabel.setText(df.format(item5.getDeadline()));
            }
        });
        filteredList = new FilteredList<TodoItem>(TodoData.getInstance().getTodoItems(), todoItem -> true);
        SortedList<TodoItem> sortedList = new SortedList<TodoItem>(filteredList, new Comparator<TodoItem>() {
            @Override
            public int compare(TodoItem o1, TodoItem o2) {
                return o1.getDeadline().compareTo(o2.getDeadline());
            }
        });

    //    todoListView.setItems(TodoData.getInstance().getTodoItems());
        todoListView.setItems(sortedList);
        todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        todoListView.getSelectionModel().selectFirst();
        todoListView.setCellFactory(new Callback<ListView<TodoItem>, ListCell<TodoItem>>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> todoItemListView) {
                ListCell<TodoItem> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(TodoItem todoItem, boolean empty) {
                        super.updateItem(todoItem, empty);
                        if(empty) {
                            setText(null);
                        } else {
                            setText(todoItem.getShortDescription());
                            if(todoItem.getDeadline().isBefore(LocalDate.now().plusDays(1)))
                                setTextFill(Color.RED);
                            else if(todoItem.getDeadline().equals(LocalDate.now().plusDays(1)))
                                setTextFill(Color.ORANGE);
                        }
                    }
                };
                cell.emptyProperty().addListener((obs,wasEmpty, isNowEmpty) -> {
                    if(isNowEmpty) {
                        cell.setContextMenu(null);
                    } else {
                        cell.setContextMenu(listContextMenu);
                    }
                });
                return cell;
//                return new ListCell<>() {
//                    @Override
//                    protected void updateItem(TodoItem todoItem, boolean empty) {
//                        super.updateItem(todoItem, empty);
//                        if(empty) setText(null);
//                        else {
//                            setText(todoItem.getShortDescription());
//                            if(todoItem.getDeadline().isBefore(LocalDate.now().plusDays(1))){
//                                setTextFill(Color.RED);
//                            } else if(todoItem.getDeadline().equals(LocalDate.now().plusDays(1))) {
//                                setTextFill(Color.ORANGE);
//                            }
//                        }
//                    }
//
//                };
            }
        });
    }
    @FXML
    public void showNewItemDialog(){
        String location = "todoItemDialog.fxml";
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Add New Todo Item");
        dialog.setHeaderText("Add new item");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(Objects.requireNonNull(getClass().getResource(location)));
        try{
            dialog.getDialogPane().setContent(fxmlLoader.load());
        }catch (IOException e){
            System.out.println("Couldn't load the dialog");
            e.printStackTrace();
            return;
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent() && result.get()== ButtonType.OK){
            DialogController controller = fxmlLoader.getController();
            TodoItem newItem = controller.processResults();
            todoListView.getSelectionModel().select(newItem);
        }else {
            System.out.println("Cancel pressed");
        }
    }

    // Using keyboard to delete
    @FXML
    public void handleKeyPressed(KeyEvent keyEven) {
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(selectedItem != null) {
            if(keyEven.getCode().equals(KeyCode.DELETE)){
                deleteItem(selectedItem);
            }
        }
    }

    @FXML
    public void handleClickListView() {
        TodoItem item = todoListView.getSelectionModel().getSelectedItem();
        itemDetailsTextArea.setText(item.getDetails());
        deadlineLabel.setText(item.getDeadline().toString());
    }

    public void deleteItem (TodoItem todoItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Todo Item");
        alert.setHeaderText("Delete item " + todoItem.getShortDescription());
        alert.setContentText("Are you sure? Press OK to confirm, or cancel to back out");
        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK) {
            TodoData.getInstance().deleteTodoItem(todoItem);
        }
    }

    public void handleFilterButton() {
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(filterToggleButton.isSelected()){
            filteredList.setPredicate(todoItem -> todoItem.getDeadline().equals(LocalDate.now()));
            if(filteredList.isEmpty()) {
                itemDetailsTextArea.clear();
                deadlineLabel.setText("");
            } else if (filteredList.contains(selectedItem)) {
                todoListView.getSelectionModel().select(selectedItem);
            } else {
                todoListView.getSelectionModel().selectFirst();
            }
        } else {
            filteredList.setPredicate(todoItem -> true);
            todoListView.getSelectionModel().select(selectedItem);
        }
    }

    public void handleExist() {
        Platform.exit();
    }

}
