package me.eren.skriptplus.utils;

public class Version {
    private final String version;

    public Version(String ver) {
        this.version = ver;
    }

    public boolean isLargerThan(Version ver) {
        String[] v1Parts = this.version.replaceAll("[^0-9.]", "").split("\\.");
        String[] v2Parts = ver.version.replaceAll("[^0-9.]", "").split("\\."); // Remove non-numeric characters

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Value = (i < v1Parts.length) ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Value = (i < v2Parts.length) ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Value < v2Value) {
                return false;
            } else if (v1Value > v2Value) {
                return true;
            }
        }
        return false; // Versions are equal
    }

    public String toString() {
        return this.version;
    }
}
