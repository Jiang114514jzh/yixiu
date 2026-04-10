package com.example.yixiu_1

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.yixiu_1.network.NetworkClient

class MyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        // 在这里全局统一定义 Coil 使用你写好的、绕过证书的 OkHttpClient
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkClient.okHttpClient)
            .crossfade(true) // 全局开启淡入效果
            .build()
    }
}
