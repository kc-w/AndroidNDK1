package com.example.androidndk1;

public class Driver {

    private String Name;
    private String Path;

    public Driver(String name, String path) {
        this.Name = name;
        this.Path = path;
    }


    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String path) {
        this.Path = path;
    }
}
