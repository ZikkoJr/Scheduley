package com.scheduley.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;

import java.util.List;

record ColorChoice(String name, String hex) {
    private static final List<ColorChoice> PRESETS = List.of(
            new ColorChoice("Blue", "#3B82F6"),
            new ColorChoice("Green", "#22C55E"),
            new ColorChoice("Purple", "#8B5CF6"),
            new ColorChoice("Orange", "#F97316"),
            new ColorChoice("Red", "#EF4444"),
            new ColorChoice("Yellow", "#EAB308"),
            new ColorChoice("Pink", "#EC4899"),
            new ColorChoice("Gray", "#6B7280")
    );

    static ComboBox<ColorChoice> comboBox(String initialHex, String fallbackHex) {
        ComboBox<ColorChoice> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(PRESETS);
        String selectedHex = normalize(initialHex == null || initialHex.isBlank() ? fallbackHex : initialHex);
        ColorChoice selected = findByHex(selectedHex);
        if (selected == null) {
            selected = new ColorChoice("Saved color", selectedHex);
            comboBox.getItems().add(selected);
        }
        comboBox.setValue(selected);
        comboBox.setCellFactory(list -> new ColorCell());
        comboBox.setButtonCell(new ColorCell());
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    static String selectedHex(ComboBox<ColorChoice> comboBox) {
        return comboBox.getValue() == null ? null : comboBox.getValue().hex();
    }

    static void selectHex(ComboBox<ColorChoice> comboBox, String hex) {
        String normalized = normalize(hex);
        ColorChoice choice = findByHex(normalized);
        if (choice == null) {
            choice = new ColorChoice("Saved color", normalized);
            comboBox.getItems().removeIf(item -> "Saved color".equals(item.name()));
            comboBox.getItems().add(choice);
        }
        comboBox.setValue(choice);
    }

    static String displayText(String hex) {
        String normalized = normalize(hex);
        ColorChoice choice = findByHex(normalized);
        return (choice == null ? "Saved color" : choice.name()) + "  " + normalized;
    }

    static Region swatch(String hex) {
        Region swatch = new Region();
        swatch.setPrefSize(18, 18);
        swatch.setMinSize(18, 18);
        swatch.setMaxSize(18, 18);
        swatch.setStyle("""
                -fx-background-color: %s;
                -fx-border-color: rgba(0,0,0,0.25);
                -fx-border-radius: 4;
                -fx-background-radius: 4;
                """.formatted(normalize(hex)));
        return swatch;
    }

    private static ColorChoice findByHex(String hex) {
        return PRESETS.stream()
                .filter(choice -> choice.hex().equalsIgnoreCase(hex))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String hex) {
        if (hex == null || hex.isBlank()) {
            return "#6B7280";
        }
        return hex.trim().toUpperCase();
    }

    private static final class ColorCell extends ListCell<ColorChoice> {
        @Override
        protected void updateItem(ColorChoice item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item.name() + "  " + item.hex());
            setGraphic(swatch(item.hex()));
        }
    }
}
