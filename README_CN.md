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
```