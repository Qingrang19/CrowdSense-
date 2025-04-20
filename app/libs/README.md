# 高德地图SDK集成说明

## 手动下载SDK文件

由于高德地图SDK的Maven仓库配置问题，建议手动下载SDK文件并放置在此目录下：

1. 访问高德开放平台官网：https://lbs.amap.com/api/android-sdk/download
2. 下载以下SDK包：
   - 3D地图 SDK V10.1.201（包名：com.amap.api.map3d）
   - 搜索 SDK V9.7.4（包名：com.amap.api.search）
   - 定位 SDK V6.4.9（包名：com.amap.api.location）
3. 将下载的SDK文件放置在此目录（app/libs/）下

## 注意事项

- 确保AAR文件名称与build.gradle.kts中引用的名称一致
- 如需更新SDK版本，请同时更新build.gradle.kts中的配置
- 使用高德地图SDK需要在AndroidManifest.xml中配置相应的权限和API Key