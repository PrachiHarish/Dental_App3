package com.example.dentalapp;

public class Patient {
    private String patientID;
    private String name;
    private String age;
    private String password;

    // Constructor
    public Patient(String patientID, String name, String age, String password) {
        this.patientID = patientID;
        this.name = name;
        this.age = age;
        this.password = password;
    }

    // Getter and Setter methods
    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
