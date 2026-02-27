package Tunescom.TunescomJAVA.tools;

public final class SelectedContext {
    private SelectedContext() {}

    private static Integer selectedProduitId = null;

    public static Integer getSelectedProduitId() {
        return selectedProduitId;
    }

    public static void setSelectedProduitId(Integer id) {
        selectedProduitId = id;
    }
}
