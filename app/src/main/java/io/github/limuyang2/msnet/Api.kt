package io.github.limuyang2.msnet

import msnet.Call
import msnet.Response
import msnet.annotation.DisableCache
import msnet.annotation.GET
import okcronet.http.ResponseBody

/**
 * @author 李沐阳
 * @date 2024/3/27
 * @description
 */
interface Api {

    @GET("lishi/api.php")
    @DisableCache
    fun todayCall(): Call<ResponseBody>

    @GET("/")
    @DisableCache
    suspend fun todayResponse(): Response<ResponseBody>
}