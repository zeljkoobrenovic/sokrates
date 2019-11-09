package nl.obren.sokrates.codeexplorer.search;

import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.util.List;

public class PredefinedFiltersMenuPopulator {

    public void populate(Button button, List<SourceFileFilter> filters, SearchPane searchPane) {
        button.setOnMousePressed(event -> {
            show(button, filters, searchPane);
        });
    }

    public void show(Button button, List<SourceFileFilter> filters, SearchPane searchPane) {
        final ContextMenu contextMenu = new ContextMenu();
        button.setContextMenu(contextMenu);

        Menu menu = new Menu("pre-defined filters");
        filters.forEach(filter -> {
            menu.getItems().add(getNewMenuItem(filter, searchPane));
        });
        contextMenu.getItems().add(menu);

        Menu stanadardFiltersMenu = new Menu("standard filters");
        stanadardFiltersMenu.getItems().add(getNewMenuItem(new SourceFileFilter("", "^[^a-zA-Z_$]|[^\\\\w$]"), searchPane));
        stanadardFiltersMenu.getItems().add(getNewMenuItem(new SourceFileFilter("", "[0-9]+"), searchPane));
        contextMenu.getItems().add(stanadardFiltersMenu);

        Bounds bounds = button.localToScreen(button.getLayoutBounds());
        contextMenu.show(button, bounds.getMinX(), bounds.getMaxY());
    }

    private MenuItem getNewMenuItem(SourceFileFilter filter, SearchPane searchPane) {
        MenuItem item = new MenuItem(filter.toString());
        item.setOnAction(event -> {
            searchPane.setPredefinedFilter(filter);
        });
        return item;
    }
}