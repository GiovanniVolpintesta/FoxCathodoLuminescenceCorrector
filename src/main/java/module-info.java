module foxinhead.foximageconverter {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;

    opens foxinhead.foxcathodoluminescencecorrector to javafx.fxml;
    //opens foxinhead.foximageconverter.tests to javafx.fxml;

    exports foxinhead.foxcathodoluminescencecorrector;
    //exports foxinhead.foximageconverter.tests;
}