package com.example.tripbuddy;
import android.content.Context;
import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class ConfigReader {

    public static String getApiKey() {
        Properties properties = new Properties();
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find " + "config.properties");
                return null;
            }
            properties.load(input);
            return properties.getProperty("api_key");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readGoogleMapsApiKey(Context context,String key) {



        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();

        try (InputStream inputStream = assetManager.open("config.properties")) {
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
