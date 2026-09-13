package com.example.backend.audit;

public class AuditContext {
    private static final ThreadLocal<String> ip = new ThreadLocal<>();
    private static final ThreadLocal<String> path = new ThreadLocal<>();
    private static final ThreadLocal<String> method = new ThreadLocal<>();

    public static void set(String ipVal, String methodVal, String pathVal) {
        ip.set(ipVal);
        method.set(methodVal);
        path.set(pathVal);
    }

    public static String ip() { return ip.get(); }
    public static String path() { return path.get(); }
    public static String method() { return method.get(); }

    public static void clear() {
        ip.remove();
        method.remove();
        path.remove();
    }
}

