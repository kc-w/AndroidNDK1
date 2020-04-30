#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>


#include "android/log.h"

static const char *TAG = "serial_port";
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

/**
 * 获取相应波特率
 * @param baudrate 波特率
 * @return
 */
static speed_t getBaudrate(jint baudrate) {
    switch (baudrate) {
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 115200:
            return B115200;
        default:
            return -1;
    }
}

/*
 * Class:     android_serialport_SerialPort
 * Method:    打开串口，主要就是返回FileDescriptor这个对象，供java层调用。
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_androidndk1_SerialPort_open
        (JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint flags) {
    int fd;
    speed_t speed;
    jobject mFileDescriptor;

    /* 检查参数 */
    {
        speed = getBaudrate(baudrate);
        if (speed == -1) {
            /* TODO: throw an exception */
            LOGE("so库报错:串口开启异常!");
            return NULL;
        }
    }

    /* 打开设备 */
    {
        jboolean iscopy;
        const char *path_utf = env->GetStringUTFChars(path, &iscopy);
        LOGD("so库日志:开启串口%s 标记 0x%x", path_utf, O_RDWR | flags);
        fd = open(path_utf, O_RDWR | flags );//加上 O_NDELAY 参数，读取数据是异步的，不是阻塞的。目前没有加上此参数，所以说读取数据是阻塞的方式，具体加不加视情况而定。
        LOGD("so库日志:开启fd的返回值fd = %d", fd);
        env->ReleaseStringUTFChars(path, path_utf);
        if (fd == -1) {
            /* Throw an exception */
            LOGE("so库报错:没有开启串口!");
            /* TODO: throw an exception */
            return NULL;
        }
    }

    /* 设置参数 */
    {
        struct termios cfg;
        LOGD("so库日志:配置串口");
        if (tcgetattr(fd, &cfg)) {
            LOGE("tcgetattr() failed");
            close(fd);
            /* TODO: throw an exception */
            return NULL;
        }

        cfmakeraw(&cfg);
        cfsetispeed(&cfg, speed);
        cfsetospeed(&cfg, speed);

        if (tcsetattr(fd, TCSANOW, &cfg)) {
            LOGE("tcsetattr() failed");
            close(fd);
            /* TODO: throw an exception */
            return NULL;
        }
    }

    /* 创建一个 descriptor，用来操作这个串口文件 */
    {
        jclass cFileDescriptor = env->FindClass( "java/io/FileDescriptor");//获得类
        jmethodID iFileDescriptor = env->GetMethodID( cFileDescriptor, "<init>", "()V");//找到
        jfieldID descriptorID = env->GetFieldID(cFileDescriptor, "descriptor", "I");
        mFileDescriptor = env->NewObject( cFileDescriptor, iFileDescriptor);
        env->SetIntField( mFileDescriptor, descriptorID, (jint) fd);
    }

    return mFileDescriptor;
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    关闭串口
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL
Java_com_example_androidndk1_SerialPort_close
        (JNIEnv *env, jclass thiz) {
    jclass SerialPortClass = env->GetObjectClass( thiz);
    jclass FileDescriptorClass = env->FindClass("java/io/FileDescriptor");

    jfieldID mFdID = env->GetFieldID( SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = env->GetFieldID( FileDescriptorClass, "descriptor", "I");

    jobject mFd = env->GetObjectField( thiz, mFdID);
    jint descriptor = env->GetIntField( mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}
