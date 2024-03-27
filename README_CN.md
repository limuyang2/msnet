# msnet
类似 Retrofit ，这是对 [okcronet](https://github.com/limuyang2/okcronet) 的封装。使用方式与 Retrofit 一致。

# 引入
## 引入本库
```
    // 引入
    implementation("io.github.limuyang2:msnet:1.0.0")
```

## 引入 cronet 库
```
    // 中国大陆建议的引入方式，其中包含了本地 so 库
    implementation("org.chromium.net:cronet-api:119.6045.31")
    implementation("org.chromium.net:cronet-common:119.6045.31")
    implementation("org.chromium.net:cronet-embedded:119.6045.31")

    
    // 如果你是直接使用 Google Play 的海外app，不需要考虑中国大陆的情况，可以直接使用 Google Play 提供的 so，不需要在APK中打包 so 文件
    // 参考链接 https://developer.android.com/develop/connectivity/cronet/start#kts
    //
    implementation("com.google.android.gms:play-services-cronet:18.0.1")
```

# 使用
* 创建接口
```kotlin
interface Api {

    @GET("lishi/api.php")
    fun todayCall(): Call<ResponseBody>

    @GET("lishi/api.php")
    @DisableCache
    suspend fun todayResponse(): Response<ResponseBody>
}
```

* 发起请求
```kotlin
    // 创建 CronetClient
    val cronetClient = CronetClient.Builder(cronetEngine).build()

    // 创建 msnet
    val msnet = MSNet.Builder()
        .cronet(cronetClient)
        .baseUrl("https://api.oick.cn/")
        .build()

    // 获取接口
    val api = msnet.create<Api>()

    // 请求网络获取结果
    val response = api.todayResponse()

    // 输出结果
    println("Result: ${response.isSuccessful} \n ${response.body()?.string()}")
```

## 新的接口注解
* `DisableCache` - 不使用缓存

* `PRIORITY` - 请求优先级

# 从 Retrofit 迁移
只需全局替换引用即可

mac全局替换快捷键：`shift + command + R`

# 混淆
已自动包含收缩和混淆规则。

# Thanks
[retrofit](https://github.com/square/retrofit)
