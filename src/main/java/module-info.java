module com.volpintesta.IBBIC {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;

    opens com.volpintesta.IBBIC to javafx.fxml;
    exports com.volpintesta.IBBIC;
}