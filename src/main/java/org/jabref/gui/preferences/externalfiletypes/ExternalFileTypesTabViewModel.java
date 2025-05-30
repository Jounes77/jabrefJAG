package org.jabref.gui.preferences.externalfiletypes;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalFileTypesTabViewModel implements PreferenceTabViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFileTypesTabViewModel.class);
    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes = FXCollections.observableArrayList();

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final DialogService dialogService;

    public ExternalFileTypesTabViewModel(ExternalApplicationsPreferences externalApplicationsPreferences, DialogService dialogService) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        fileTypes.clear();
        fileTypes.addAll(externalApplicationsPreferences.getExternalFileTypes().stream()
                                                        .map(ExternalFileTypeItemViewModel::new)
                                                        .toList());
        fileTypes.sort(Comparator.comparing(ExternalFileTypeItemViewModel::getName));
    }

    public void storeSettings() {
        Set<ExternalFileType> saveList = new HashSet<>();

        fileTypes.stream().map(ExternalFileTypeItemViewModel::toExternalFileType)
                 .forEach(type -> ExternalFileTypes.getDefaultExternalFileTypes().stream()
                                                   .filter(type::equals).findAny()
                                                   .ifPresentOrElse(saveList::add, () -> saveList.add(type)));

        externalApplicationsPreferences.getExternalFileTypes().clear();
        externalApplicationsPreferences.getExternalFileTypes().addAll(saveList);
    }

    public void resetToDefaults() {
        fileTypes.setAll(ExternalFileTypes.getDefaultExternalFileTypes().stream()
                                          .map(ExternalFileTypeItemViewModel::new)
                                          .toList());
        fileTypes.sort(Comparator.comparing(ExternalFileTypeItemViewModel::getName));
    }

    public boolean addNewType() {
        ExternalFileTypeItemViewModel item = new ExternalFileTypeItemViewModel();
        showEditDialog(item, Localization.lang("Add new file type"));

        if (!isValidExternalFileType(item)) {
            return false;
        }

        fileTypes.add(item);
        return true;
    }

    public ObservableList<ExternalFileTypeItemViewModel> getFileTypes() {
        return fileTypes;
    }

    protected void showEditDialog(ExternalFileTypeItemViewModel item, String dialogTitle) {
        dialogService.showCustomDialogAndWait(new EditExternalFileTypeEntryDialog(item, dialogTitle, fileTypes));
    }

    public boolean edit(ExternalFileTypeItemViewModel type) {
        ExternalFileTypeItemViewModel typeToModify = new ExternalFileTypeItemViewModel(type.toExternalFileType());
        showEditDialog(typeToModify, Localization.lang("Edit file type"));

        if (type.extensionProperty().get().equals(typeToModify.extensionProperty().get())) {
            if (hasEmptyValue(typeToModify)) {
                LOGGER.warn("One of the fields is empty or invalid. Not saving.");
                return false;
            }
        } else if (!isValidExternalFileType(typeToModify)) {
            return false;
        }

        fileTypes.remove(type);
        fileTypes.add(typeToModify);
        return true;
    }

    public void remove(ExternalFileTypeItemViewModel type) {
        fileTypes.remove(type);
    }

    public boolean isValidExternalFileType(ExternalFileTypeItemViewModel item) {
        if (hasEmptyValue(item)) {
            LOGGER.warn("One of the fields is empty or invalid. Not saving.");
            return false;
        }

        if (!isUniqueExtension(item)) {
            LOGGER.warn("File Extension already exists. Not saving.");
            return false;
        }

        return true;
    }

    private boolean hasEmptyValue(ExternalFileTypeItemViewModel item) {
        return item.getName().isEmpty() || item.extensionProperty().get().isEmpty() || item.mimetypeProperty().get().isEmpty();
    }

    private boolean isUniqueExtension(ExternalFileTypeItemViewModel item) {
        // check extension need to be unique in the list
        String newExt = item.extensionProperty().get();
        for (ExternalFileTypeItemViewModel fileTypeItem : fileTypes) {
            if (newExt.equalsIgnoreCase(fileTypeItem.extensionProperty().get())) {
                return false;
            }
        }
        return true;
    }
}
