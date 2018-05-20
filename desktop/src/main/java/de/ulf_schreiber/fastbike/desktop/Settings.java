package de.ulf_schreiber.fastbike.desktop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Settings {
    public static final String settingsName = "settings.json";
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
        .create();
    public static Settings load(File file) throws IOException {
        if(file==null || ! file.exists()) {
            return new Settings();
        }
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        Settings settings = gson.fromJson(fileReader, Settings.class);
        fileReader.close();
        return settings;
    }
    public static void store(File file, Settings settings) throws IOException {
        BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        gson.toJson(settings, fileWriter);
        fileWriter.close();
    }

    public String mapsFolder;
    public String map;
    public String demFolder;
    double[] viewport;
}
