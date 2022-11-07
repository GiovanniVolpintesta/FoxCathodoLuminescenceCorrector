module foxinhead.foxcathodoluminescencecorrector {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;

    opens foxinhead.foxcathodoluminescencecorrector to javafx.fxml;
    exports foxinhead.foxcathodoluminescencecorrector;
}