package com.scheduley.models;

public class Course {

    private Long id;
    private String code, name;
    private String colourHex = "#00A86B";
    private int credits;


    //constructor for new course before insert
    public Course(String code, String name, int credits, String colourHex) {
        this(null, code, name, credits, colourHex);
    }

    //Constructor for existing course from db
    public Course(Long id, String code, String name, int credits, String colourHex) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.colourHex = colourHex;
        this.credits = credits;
    }


    // Getters and setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getCredits() {
        return credits;
    }
    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getColourHex() {
        return colourHex;
    }
    public void setColourHex(String colour_hex) {
        this.colourHex = colour_hex;
    }



    @Override
    public String toString() {
        return "Course{" + "id=" + id
                + ", code=" + code
                + ", name=" + name
                + ", credits=" + credits
                + ", colour_hex=" + colourHex + '}';
    }


}
