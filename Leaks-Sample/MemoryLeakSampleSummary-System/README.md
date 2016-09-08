# 在Android系统源码上添加leakcanary lib分析问题
---
由于目前出现有部分系统应用出现内存泄漏，而系统应用编译一般都是在系统源码环境下编译的。leakcanary检测包合入原生代码，一般需要合入如下文件：
```java
            leakcanary-android-1.4-beta2.aar
            leakcanary-analyzer-1.4-beta2.jar
            leakcanary-watcher-1.4-beta2.jar
            haha-2.0.jar
```
Android 系统源码添加jar包比较简单：
```java
LOCAL_STATIC_JAVA_LIBRARIES := \
	lf-haha-1.3 \
	lf-leakcanary-watcher-1.4-beta2 \
	lf-leakcanary-analyzer-1.4-beta2
	
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    lf-haha-1.3:libs/haha-1.3.jar \
    lf-leakcanary-analyzer-1.4-beta2:libs/leakcanary-analyzer-1.4-beta2.jar \
    lf-leakcanary-watcher-1.4-beta2:libs/leakcanary-watcher-1.4-beta2.jar
```
通过以上方式可以引用jar包了。
而引用一个leakcanary-android-1.4-beta2.aar包，则不能通过jar包的方式添加了。
要引用一个aar文件，aar文件包含Android的资源文件，如：布局、样式、图片等，如果按照源码中jar的引用方式会遇到编译不过的问题，提示找不到相关的资源文件。
引用aar文件要按如下方式引用：
```java
LOCAL_STATIC_JAVA_AAR_LIBRARIES:= <aar alias>
include $(BUILD_PACKAGE)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := <aar alias>:libs/<lib file>.aar
include $(BUILD_MULTI_PREBUILT)
```
其中，LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := <aar alias>:libs/<lib file>.aar 也可以如下面这样写：
```java
include $(CLEAR_VARS)
LOCAL_MODULE := <aar alias>
LOCAL_SRC_FILES := <lib file>.aar
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_MODULE_SUFFIX := $(COMMON_JAVA_PACKAGE_SUFFIX)
LOCAL_BUILT_MODULE_STEM := javalib.jar
include $(BUILD_PREBUILT)
```
这里主要是LOCAL_STATIC_JAVA_AAR_LIBRARIES，剩下的和jar包大同小异，注意在manifest文件里minSdkVersion要满足aar文件的要求。
搜索Android源码，也可以发现：
```java
#LOCAL_STATIC_JAVA_AAR_LIBRARIES are special LOCAL_STATIC_JAVA_LIBRARIES 
LOCAL_STATIC_JAVA_LIBRARIES := (strip(LOCAL_STATIC_JAVA_LIBRARIES) $(LOCAL_STATIC_JAVA_AAR_LIBRARIES))
```
这一步完成后，代码可以顺利编译过了，不过在运行apk时如果使用到aar文件里面的资源还是crash，所以还需要加上以下语句：
```java
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.squareup.leakcanary
```
```java
AAPT(Android Asset Packaging Tool)在SDK的tools/目录下. 该工具可以查看, 创建, 更新ZIP格式的文档附件(zip, jar, apk). 也可将资源文件编译成二进制文件,尽管你可能没有直接使用过aapt工具, 但是build scripts和IDE插件会使用这个工具打包apk文件构成一个Android 应用程序
```
上面代码就是通过AAPT，把是一种将多个工程合并成一个apk的方法。
这样就可以把leakcanary检测包合入到我们demo应用中，其他系统应用也可以参考这样的方式添加检测lib。


目前的话，存在haha.jar可能无法兼容目前平台代码，2.0以上的版本有如下编译故障
```java

java -Xmx3500m -jar out/host/linux-x86/framework/jill.jar  --output out/target/common/obj/JAVA_LIBRARIES/lf-haha-2.0.2_intermediates/classes.jack.tmpjill.jack packages/apps/MemoryLeakSampleSummary/libs/haha-2.0.2.jar
Launching background server java -Dfile.encoding=UTF-8 -Xms2560m -XX:+TieredCompilation -jar out/host/linux-x86/framework/jack-launcher.jar -cp out/host/linux-x86/framework/jack.jar com.android.jack.server.JackSimpleServer
java -Xmx3500m -jar out/host/linux-x86/framework/jill.jar  --output out/target/common/obj/JAVA_LIBRARIES/lf-leakcanary-watcher-1.4-beta2_intermediates/classes.jack.tmpjill.jack packages/apps/MemoryLeakSampleSummary/libs/leakcanary-watcher-1.4-beta2.jar
java -Xmx3500m -jar out/host/linux-x86/framework/jill.jar  --output out/target/common/obj/JAVA_LIBRARIES/lf-leakcanary-analyzer-1.4-beta2_intermediates/classes.jack.tmpjill.jack packages/apps/MemoryLeakSampleSummary/libs/leakcanary-analyzer-1.4-beta2.jar
java -Xmx3500m -jar out/host/linux-x86/framework/jill.jar  --output out/target/common/obj/JAVA_LIBRARIES/lf-leakcanary-android-1.4-beta2_intermediates/classes.jack.tmpjill.jack out/target/common/obj/JAVA_LIBRARIES/lf-leakcanary-android-1.4-beta2_intermediates/aar/classes.jar
Building with Jack: out/target/common/obj/APPS/MemoryLeakSampleSummary_intermediates/with-local/classes.dex
com.android.sched.scheduler.RunnerProcessException: Error during 'TypeGenericSignatureSplitter' runner on 'class com.squareup.haha.guava.collect.AbstractMapBasedMultimap$SortedKeySet (AbstractMapBasedMultimap.java:0-0)'
        at com.android.sched.scheduler.ScheduleInstance.runWithLog(ScheduleInstance.java:156)
        at com.android.sched.scheduler.MultiWorkersScheduleInstance$SequentialTask.process(MultiWorkersScheduleInstance.java:408)
        at com.android.sched.scheduler.MultiWorkersScheduleInstance$Worker.run(MultiWorkersScheduleInstance.java:143)
Caused by: java.lang.reflect.GenericSignatureFormatError
        at com.android.jack.signature.GenericSignatureParser.expect(GenericSignatureParser.java:371)
        at com.android.jack.signature.GenericSignatureParser.parseClassTypeSignature(GenericSignatureParser.java:236)
        at com.android.jack.signature.GenericSignatureParser.parseClassSignature(GenericSignatureParser.java:136)
        at com.android.jack.signature.GenericSignatureParser.parseClassSignature(GenericSignatureParser.java:99)
        at com.android.jack.transformations.ast.string.TypeGenericSignatureSplitter.getSplittedSignature(TypeGenericSignatureSplitter.java:74)
        at com.android.jack.transformations.ast.string.TypeGenericSignatureSplitter.run(TypeGenericSignatureSplitter.java:51)
        at com.android.jack.transformations.ast.string.TypeGenericSignatureSplitter.run(TypeGenericSignatureSplitter.java:37)
        at com.android.sched.scheduler.ScheduleInstance.runWithLog(ScheduleInstance.java:154)
        ... 2 more
Internal compiler error (version 1.1-mr2 'Brest' (175100 000a2f91edbe638090dc577801ba49592229fd8b)).
Error during 'TypeGenericSignatureSplitter' runner on 'class com.squareup.haha.guava.collect.AbstractMapBasedMultimap$SortedKeySet (AbstractMapBasedMultimap.java:0-0)'.
Warning: This may have produced partial or corrupted output.
make: *** [out/target/common/obj/APPS/MemoryLeakSampleSummary_intermediates/with-local/classes.dex] Error 41


```
haha-2.0.jar 2.0以下的库则出现不能兼容leakcanary-1.4库的情况，在检测过程中，会出现报错现象。



