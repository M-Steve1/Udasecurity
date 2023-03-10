module com.udacity.securityservice {
    requires java.desktop;
    requires miglayout;
    requires com.udacity.imageservice;
    requires java.prefs;
    opens com.udacity.securityservice.data to com.google.gson;
    requires com.google.gson;
    requires com.google.common;
}