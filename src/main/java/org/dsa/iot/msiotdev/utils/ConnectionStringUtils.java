package org.dsa.iot.msiotdev.utils;

import java.util.HashMap;
import java.util.Map;

public class ConnectionStringUtils {
    public static Map<String, String> parse(String input) {
        String[] parts = input.split(";");

        Map<String, String> map = new HashMap<>();

        for (String part : parts) {
            String[] partials = part.split("=", 2);
            map.put(partials[0], partials[1]);
        }

        return map;
    }
}
