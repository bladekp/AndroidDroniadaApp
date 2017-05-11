#include <jni.h>
#include <fcntl.h>
#include <stdio.h>
#include <termios.h>

#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "SerialJNI", __VA_ARGS__)

// Save the file descriptor between multiple calls
int serialDescriptor = -1;

extern "C" void Java_rtandroid_main_serial_SerialDriver_open(JNIEnv* env, jobject /* this */)
{
    if (serialDescriptor > 0) { return; }

    // open in non blocking read/write mode
    serialDescriptor = open("/dev/ttyS0", O_WRONLY | O_NOCTTY | O_NDELAY | O_NONBLOCK);
    if (serialDescriptor == -1)
    {
        LOGI("ERR: unable to open UART");
        return;
    }

    struct termios options;
    int config = tcgetattr(serialDescriptor, &options);
    if (config < 0)
    {
        LOGI("ERR: failed to get UART config");
        return;
    }

    options.c_cflag = B9600 | CS8 | CREAD | CLOCAL;
    options.c_iflag = 0;
    options.c_oflag = 0;
    options.c_lflag = 0;
    options.c_cc[VMIN] = 0;
    options.c_cc[VTIME] = 0;

    tcflush(serialDescriptor, TCIFLUSH);
    tcsetattr(serialDescriptor, TCSANOW, &options);
}

extern "C" void write_message(const char message[])
{
    if (serialDescriptor < 0) { return; }

    ssize_t count = write(serialDescriptor, message, strlen(message));
    if (count < 0) { LOGI("ERR: failed to send data"); }
}

extern "C" void Java_rtandroid_main_serial_SerialDriver_logcat(JNIEnv* env, jobject, jstring message)
{
    jboolean isCopy;
    const char *inCStr = (env)->GetStringUTFChars(message, &isCopy);

    if (serialDescriptor < 0) { return; }

    write_message(inCStr);

}

extern "C" void Java_rtandroid_main_serial_SerialDriver_close(JNIEnv* env, jobject /* this */)
{
    if (serialDescriptor < 0) { return; }

    close(serialDescriptor);
    serialDescriptor = -1;
}
