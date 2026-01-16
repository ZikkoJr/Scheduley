package com.scheduley;
import com.scheduley.db.ConnectDB;
import com.scheduley.db.Migrations;

import java.sql.*;

public class Main {
    public static void main(String[] args) {


        Migrations.init();



    }
}