package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.ShowPreferencesAction;
import org.jabref.gui.preferences.table.TableTab;
import org.jabref.logic.l10n.Localization;

public class MainTableHeaderContextMenu extends ContextMenu {

    private static final int OUT_OF_BOUNDS = -1;
    MainTable mainTable;
    MainTableColumnFactory factory;
    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;

    public MainTableHeaderContextMenu(MainTable mainTable,
                                      MainTableColumnFactory factory,
                                      LibraryTabContainer tabContainer,
                                      DialogService dialogService) {
        super();
        this.tabContainer = tabContainer;
        this.mainTable = mainTable;
        this.factory = factory;
        this.dialogService = dialogService;

        constructItems();
    }

    /**
     * Handles showing the menu in the cursors position when right-clicked.
     */
    public void show(boolean show) {
        // TODO: 20/10/2022 unknown bug where issue does not show unless parameter is passed through this method.
        mainTable.setOnContextMenuRequested(event -> {
            // Display the menu if header is clicked, otherwise, remove from display.
            if (!(event.getTarget() instanceof StackPane) && show) {
                this.show(mainTable, event.getScreenX(), event.getScreenY());
            } else if (this.isShowing()) {
                this.hide();
            }
            event.consume();
        });
    }

    /**
     * Constructs the items for the list and places them in the menu.
     */
    private void constructItems() {
        // Reset the right-click menu
        this.getItems().clear();
        List<TableColumn<BibEntryTableViewModel, ?>> commonColumns = commonColumns();

        // Populate the menu with currently used fields
        for (TableColumn<BibEntryTableViewModel, ?> column : mainTable.getColumns()) {
            if (((MainTableColumn<?>) column).getModel().getType() == MainTableColumnModel.Type.MATCH_CATEGORY) {
                continue;
            }
            // Append only if the column has not already been added (a common column)
            RightClickMenuItem itemToAdd = createMenuItem(column, true);
            this.getItems().add(itemToAdd);

            // Remove from remaining common columns pool
            MainTableColumn<?> searchCol = (MainTableColumn<?>) column;
            if (isACommonColumn(searchCol)) {
                commonColumns.removeIf(tableCol -> ((MainTableColumn<?>) tableCol).getModel().equals(searchCol.getModel()));
            }
        }

        if (!commonColumns.isEmpty()) {
            this.getItems().add(new SeparatorMenuItem());

            // Append to the menu the current remaining columns in the common columns.
            for (TableColumn<BibEntryTableViewModel, ?> tableColumn : commonColumns) {
                RightClickMenuItem itemToAdd = createMenuItem(tableColumn, false);
                this.getItems().add(itemToAdd);
            }
        }

        this.getItems().add(new SeparatorMenuItem());
        ActionFactory actionfactory = new ActionFactory();
        MenuItem showMoreItem = actionfactory.createMenuItem(
                StandardActions.SHOW_PREFS.withText(Localization.lang("More options...")),
                new ShowPreferencesAction(tabContainer, TableTab.class, dialogService));
        this.getItems().add(showMoreItem);
    }

    /**
     * Creates an item for the menu constructed with the name/visibility of the table column.
     */
    private RightClickMenuItem createMenuItem(TableColumn<BibEntryTableViewModel, ?> column, boolean isDisplaying) {
        // Gets display name and constructs Radio Menu Item.
        MainTableColumn<?> tableColumn = (MainTableColumn<?>) column;
        String displayName = tableColumn.getDisplayName();
        return new RightClickMenuItem(displayName, tableColumn, isDisplaying);
    }

    /**
     * Returns the current position of the inputted column in the table (index).
     */
    private int obtainIndexOfColumn(MainTableColumn<?> searchColumn) {
        ObservableList<TableColumn<BibEntryTableViewModel, ?>> columns = mainTable.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<BibEntryTableViewModel, ?> column = columns.get(i);
            MainTableColumnModel model = ((MainTableColumn<?>) column).getModel();
            if (model.equals(searchColumn.getModel())) {
                return i;
            }
        }
        return OUT_OF_BOUNDS;
    }

    /**
     * Adds the column into the MainTable for display.
     */
    private void addColumn(MainTableColumn<?> tableColumn, int index) {
        if ((index <= OUT_OF_BOUNDS) || (index >= mainTable.getColumns().size())) {
            mainTable.getColumns().add(tableColumn);
        } else {
            mainTable.getColumns().add(index, tableColumn);
        }
    }

    /**
     * Removes the column from the MainTable to remove visibility.
     */
    private void removeColumn(MainTableColumn<?> tableColumn) {
        mainTable.getColumns().removeIf(tableCol -> ((MainTableColumn<?>) tableCol).getModel().equals(tableColumn.getModel()));
    }

    /**
     * Checks if a column is one of the commonly used columns.
     */
    private boolean isACommonColumn(MainTableColumn<?> tableColumn) {
        return isColumnInList(tableColumn, commonColumns());
    }

    /**
     * Determines if a list of TableColumns contains the searched column.
     */
    private boolean isColumnInList(MainTableColumn<?> searchColumn, List<TableColumn<BibEntryTableViewModel, ?>> tableColumns) {
        for (TableColumn<BibEntryTableViewModel, ?> column: tableColumns) {
            MainTableColumnModel model = ((MainTableColumn<?>) column).getModel();
            if (model.equals(searchColumn.getModel())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the list of the "commonly used" columns (currently based on the default preferences).
     */
    private List<TableColumn<BibEntryTableViewModel, ?>> commonColumns() {
        // Qualifier strings
        String entryTypeQualifier = "entrytype";
        String authorEditQualifier = "author/editor";
        String titleQualifier = "title";
        String yearQualifier = "year";
        String journalBookQualifier = "journal/booktitle";
        String rankQualifier = "ranking";
        String readStatusQualifier = "readstatus";
        String priorityQualifier = "priority";
        String progressQualifier = "progress";

        // Create the MainTableColumn Models from qualifiers + types.
        List<MainTableColumnModel> commonColumns = new ArrayList<>();
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.GROUPS));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.GROUP_ICONS));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.FILES));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, entryTypeQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, authorEditQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, titleQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, yearQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, journalBookQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, rankQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, readStatusQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, priorityQualifier));
        commonColumns.add(new MainTableColumnModel(MainTableColumnModel.Type.SPECIALFIELD, progressQualifier));

        // Create the Table Columns from the models using factory methods.
        List<TableColumn<BibEntryTableViewModel, ?>> commonTableColumns = new ArrayList<>();
        for (MainTableColumnModel columnModel: commonColumns) {
            TableColumn<BibEntryTableViewModel, ?> tableColumn = factory.createColumn(columnModel);
            commonTableColumns.add(tableColumn);
        }
        return commonTableColumns;
    }

    /**
     * RightClickMenuItem: RadioMenuItem holding position in MainTable and its visibility.
     */
    private class RightClickMenuItem extends RadioMenuItem {
        private int index;
        private boolean visibleInTable;

        RightClickMenuItem(String displayName, MainTableColumn<?> column, boolean isVisible) {
            super(displayName);
            setVisibleInTable(isVisible);
            // Flag item as selected if the item is already in the main table.
            this.setSelected(isVisible);

            setIndex(OUT_OF_BOUNDS);

            // Set action to toggle visibility from main table when item is clicked
            this.setOnAction(event -> {
                if (isVisibleInTable()) {
                    setIndex(obtainIndexOfColumn(column));
                    removeColumn(column);
                } else {
                    addColumn(column, this.index);
                    setIndex(obtainIndexOfColumn(column));
                }
                setVisibleInTable(!this.visibleInTable);
            });
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public void setVisibleInTable(boolean visibleInTable) {
            this.visibleInTable = visibleInTable;
        }

        public boolean isVisibleInTable() {
            return visibleInTable;
        }
    }
}
