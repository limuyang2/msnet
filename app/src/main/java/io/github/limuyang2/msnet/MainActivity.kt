package io.github.limuyang2.msnet

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.limuyang2.msnet.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import msnet.MSNet
import msnet.create
import okcronet.CronetClient
import org.chromium.net.CronetEngine
import org.chromium.net.impl.NativeCronetEngineBuilderImpl
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    // 全局唯一性
    private val cronetEngine: CronetEngine by lazy(LazyThreadSafetyMode.NONE) {
        val httpCacheDir =
            File(this.applicationContext.externalCacheDir ?: this.applicationContext.cacheDir, "http")

        if (!httpCacheDir.exists()) {
            httpCacheDir.mkdir()
        }

        CronetEngine.Builder(
            NativeCronetEngineBuilderImpl(this.applicationContext)
        )
            .setStoragePath(httpCacheDir.absolutePath)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISABLED, 1048576)
            .enableHttp2(true)
            .enableQuic(true)
            .setThreadPriority(-1)
            .enableBrotli(true)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewBinding.btnTest.setOnClickListener {
            request()
        }

    }

    private fun request() {
        lifecycleScope.launch {
            // 创建 CronetClient
            val cronetClient = CronetClient.Builder(cronetEngine)
                .setReadTimeoutMillis(10_000)
                .build()

            // 创建 msnet
            val msnet = MSNet.Builder()
                .cronet(cronetClient)
                .baseUrl("https://http3check.net/")
                .build()

            // 获取接口
            val api = msnet.create<Api>()

            // 请求网络获取结果
            val response = api.todayResponse()

            // 输出结果
            val str = "Result: ${response.message()} \n ${response.body()?.string()}"
            viewBinding.tvInfo.text = str
            Log.d("requset", str)
        }
    }
}