package rtandroid.main.serial;

public class SerialDriver
{
    public static native void open();
    public static native void logcat(String message);
    public static native void close();
}
