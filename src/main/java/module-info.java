module com.classroomplus {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.classroomplus.controller to javafx.fxml;
    opens com.classroomplus.model to javafx.base;
    exports com.classroomplus;
}