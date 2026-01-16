package com.scheduley.models;

public class Course {

    private Long id;
    private String code, name;
    private String colour_hex = "#00A86B";
    private int credits;


    //constructor for new course before insert
    public Course(String code, String name, int credits, String colour_hex) {
        this(null, code, name, credits, colour_hex);
    }

    //Constructor for existing course from db
    public Course(Long id, String code, String name, int credits, String colour_hex) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.colour_hex = colour_hex;
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

    public String getColour_hex() {
        return colour_hex;
    }
    public void setColour_hex(String colour_hex) {
        this.colour_hex = colour_hex;
    }



    @Override
    public String toString() {
        return "Course{" + "id=" + id
                + ", code=" + code
                + ", name=" + name
                + ", credits=" + credits
                + ", colour_he=" + colour_hex + '}';
    }


}
