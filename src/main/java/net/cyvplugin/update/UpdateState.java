package net.cyvplugin.update;

public final class UpdateState {
    private UpdateState() {}

    public static volatile boolean updateReady = false;
    public static volatile String newVersion = null;
    public static volatile String error = null;
    public static volatile String status = null;
    public static final String STATUS_CHECKING = "checking";
    public static final String STATUS_DOWNLOADING = "downloading";
    public static final String STATUS_RESTART = "restart";
    public static final String STATUS_UP_TO_DATE = "up-to-date";
    public static final String STATUS_ERROR = "error";
}