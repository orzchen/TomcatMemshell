Tomcat 内存马复现

包括一个有反序列化入口的环境，一个servlet java版内存马，一个servlet jsp版内存马，和三个Tomcat通用java内存马。其中java版内存马均可直接用于TemplatesImpl注入

```shell
gcc -shared -o slib.so -fPIC -I/usr/lib/jvm/java-8-openjdk-amd64/include -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux JNIMemShell.c
```