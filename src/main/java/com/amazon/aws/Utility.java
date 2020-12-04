package com.amazon.aws;

import java.io.BufferedReader;
import java.io.FileReader;

public class Utility {
    public String getSchema() throws Exception {
        StringBuffer jsonSchema = new StringBuffer();
        try {
            String schemaFile = "/home/ubuntu/CS643_Project2/Data/wine-schema.json";

            try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {
                reader.lines().forEach(line -> jsonSchema.append(line + "\n"));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error reading schema", ex);
        }
        return jsonSchema.toString();
    }
}
