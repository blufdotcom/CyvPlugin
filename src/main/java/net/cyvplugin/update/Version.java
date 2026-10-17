package net.cyvplugin.update;

final class Version {
    private Version() {}

    static boolean isNewer(String candidate, String current) {
        if (candidate == null || current == null) return false;
        String[] a = normalize(candidate).split("\\.");
        String[] b = normalize(current).split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            String x = i < a.length ? a[i].trim() : "";
            String y = i < b.length ? b[i].trim() : "";
            int cmp = compareSegment(x, y);
            if (cmp != 0) return cmp > 0;
        }
        return false;
    }

    private static int compareSegment(String x, String y) {
        int nx = leadingNumber(x);
        int ny = leadingNumber(y);
        if (nx != ny) return nx < ny ? -1 : 1;
        return suffixOf(x).compareTo(suffixOf(y));
    }

    private static int leadingNumber(String s) {
        int end = digitsLength(s);
        if (end == 0) return 0;
        try {
            return Integer.parseInt(s.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String suffixOf(String s) {
        return s.substring(digitsLength(s));
    }

    private static int digitsLength(String s) {
        int end = 0;
        while (end < s.length() && Character.isDigit(s.charAt(end))) end++;
        return end;
    }

    private static String normalize(String version) {
        String s = version.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int dash = s.indexOf('-');
        if (dash >= 0) s = s.substring(0, dash);
        return s;
    }
}