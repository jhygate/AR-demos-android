#include <jni.h>

JNIEXPORT jdouble JNICALL
Java_com_example_arucodemo_MainActivity_celsiusToFahrenheit(
        JNIEnv* env,
        jclass classInstance,
        jdouble celsius
) {
    return  celsius * 9/5 + 32;
}

JNIEXPORT jdouble JNICALL
Java_com_example_arucodemo_MainActivity_fahrenheitToCelsius(
        JNIEnv* env,
        jclass classInstance,
        jdouble fahrenheit
) {
    return (fahrenheit - 32) * 5/9;
}



