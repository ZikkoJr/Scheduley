package com.scheduley;
import com.scheduley.db.ConnectDB;

import java.sql.*;

public class Main {
    public static void main(String[] args) {
        ConnectDB db = new ConnectDB();
        Connection connection = db.getConnection();



    }
}