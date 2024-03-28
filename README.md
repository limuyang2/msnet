# msnet
A type-safe HTTP client for Android, based on okcronet.

Similar to Retrofit, this is a wrapper for [okcronet](https://github.com/limuyang2/okcronet). Usage is consistent with Retrofit.

Even low-cost migration can be achieved by simply replacing the corresponding "import" reference.

[中文](https://github.com/limuyang2/msnet/blob/main/README_CN.md)

# Advantages of msnet
* Extensibility: msnet allows you to extend its functionality using interceptors implemented with OkCronet, such as adding new protocol support or customizing network behavior.
* Completeness: msnet provides a complete HTTP3/QUIC solution, including support for QUIC connection migration.
* Ease of use: msnet's usage is consistent with Retrofit, making it easy for developers to migrate.

# Import
## Importing the library
```
implementation("io.github.limuyang2:msnet:1.0.0")
```

## Importing the Cronet library
### Recommended method for mainland China(Contains complete local `so` library):
```
implementation("org.chromium.net:cronet-api:119.6045.31")
implementation("org.chromium.net:cronet-common:119.6045.31")
implementation("org.chromium.net:cronet-embedded:119.6045.31")
```
### Method for using Google Play overseas:
Reference link - [android develop](https://developer.android.com/develop/connectivity/cronet/start#kts)
```
implementation("com.google.android.gms:play-services-cronet:18.0.1")
```

# Using
* Create interface
```kotlin
interface Api {

    @GET("lishi/api.php")
    fun todayCall(): Call<ResponseBody>

    @GET("lishi/api.php")
    @DisableCache
    suspend fun todayResponse(): Response<ResponseBody>
}
```

* Make a request
```kotlin
    // Create CronetClient
    val cronetClient = CronetClient.Builder(cronetEngine).build()

    // Create msnet
    val msnet = MSNet.Builder()
        .cronet(cronetClient)
        .baseUrl("https://api.oick.cn/")
        .build()

    // Get interface
    val api = msnet.create<Api>()

    // Request network to get results
    val response = api.todayResponse()

    // Output results
    println("Result: ${response.isSuccessful} \n ${response.body()?.string()}")
```

## New interface annotations
* `DisableCache` - Do not use caching

* `PRIORITY` - Request priority

# Migrate from Retrofit
Just replace the reference globally

mac global replacement shortcut keys：`shift + command + R`

# Proguard
Shrinking and obfuscation rules are automatically included.

# Thanks
[retrofit](https://github.com/square/retrofit)
