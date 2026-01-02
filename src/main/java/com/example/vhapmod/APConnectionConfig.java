package com.example.vhapmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class APConnectionConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/vhapmod_connection.json";

    public String host = "";
    public int port = 0;
    public String slotName = "";
    public String password = "";
    public boolean autoConnect = false;

    public static APConnectionConfig load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return new APConnectionConfig();
        }

        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, APConnectionConfig.class);
        } catch (Exception e) {
            return new APConnectionConfig();
        }
    }

    public void save() {
        try {
            File file = new File(CONFIG_FILE);
            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean hasConnectionInfo() {
        return !host.isEmpty() && port > 0 && !slotName.isEmpty();
    }
}