# jniLibs 目录

此目录用于存放高德地图SDK所需的.so库文件。

请将以下文件从`app/libs/AMap3DMap_AMapSearch_AMapLocation/arm64-v8a`和`app/libs/AMap3DMap_AMapSearch_AMapLocation/armeabi-v7a`目录复制到对应的子目录中：

- arm64-v8a/
  - libAMapSDK_MAP_v10_1_201.so
  - libAMapSDK_SEARCH_v9_7_4.so
  - libAMapSDK_LOCATION_v6_4_9.so
  - 其他相关.so文件

- armeabi-v7a/
  - libAMapSDK_MAP_v10_1_201.so
  - libAMapSDK_SEARCH_v9_7_4.so
  - libAMapSDK_LOCATION_v6_4_9.so
  - 其他相关.so文件

注意：这些.so文件是高德地图SDK的原生库，必须正确放置才能解决未解析引用和地图闪退问题。