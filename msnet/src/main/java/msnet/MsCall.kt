package msnet

import okcronet.*
import okcronet.http.*
import okio.BufferedSource
import okio.Timeout
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author 李沐阳
 * @date 2023/2/22
 * @description cronet 请求实现类
 */
internal class MsCall<T>(
    private val callFactory: okcronet.Call.Factory,
    private val requestFactory: RequestFactory,
    private val args: Array<Any>,
    private val responseConverter: Converter<ResponseBody, T>
) : Call<T> {

    private val request: Request = requestFactory.create(args)

    private val executed = AtomicBoolean()

    private var canceled: Boolean = false

    private val rawCall by lazy(LazyThreadSafetyMode.NONE) { callFactory.newCall(request) }

    @Throws(IOException::class)
    override fun execute(): Response<T> {
        check(!executed.getAndSet(true)) {
            "Already Executed"
        }

        val rawResponse =  rawCall.execute()

        return parseResponse(rawResponse)
    }

    override fun enqueue(callback: Callback<T>) {
        check(!executed.getAndSet(true)) {
            "Already Executed"
        }

        rawCall.enqueue(object : okcronet.Callback {
            override fun onResponse(call: okcronet.Call, response: okcronet.http.Response) {
                val msResponse: Response<T>
                try {
                    msResponse = parseResponse(response)
                } catch (e: Throwable) {
                    Utils. throwIfFatal(e)
                    callFailure(e)
                    return
                }

                try {
                    callback.onResponse(this@MsCall, msResponse)
                } catch (t: Throwable) {
                    Utils.throwIfFatal(t)
                    t.printStackTrace()
                }
            }

            override fun onFailure(call: okcronet.Call, e: IOException) {
                callFailure(e)
            }

            private fun callFailure(e: Throwable) {
                try {
                    callback.onFailure(this@MsCall, e)
                } catch (t: Throwable) {
                    Utils.throwIfFatal(t)
                    t.printStackTrace()
                }
            }
        })

    }

    override val isExecuted: Boolean
        get() = executed.get()

    override val isCanceled: Boolean
        get() {
            if (canceled) {
                return true
            }
            return rawCall.isCanceled
        }

    override fun cancel() {
        canceled = true
        rawCall.cancel()
    }

    override fun clone(): Call<T> {
        return MsCall(callFactory, requestFactory, args, responseConverter)
    }

    override fun request(): Request= request

    @Synchronized
    override fun timeout(): Timeout = rawCall.timeout()

    @Throws(IOException::class)
    fun parseResponse(response: okcronet.http.Response): Response<T> {
        var rawResponse = response
        val rawBody = response.body ?: throw IllegalStateException("Raw response body is null.")

        // Remove the body's source (the only stateful object) so we can pass the response along.
        rawResponse = rawResponse
            .newBuilder()
            .body(NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
            .build()
        val code = response.urlResponseInfo.httpStatusCode
        if (code < 200 || code >= 300) {
            return rawBody.use {
                // Buffer the entire body to avoid future I/O.
                val bufferedBody = Utils.buffer(it)
                Response.error(bufferedBody, rawResponse)
            }
        }
        if (code == 204 || code == 205) {
            rawBody.close()
            return Response.success(null, rawResponse)
        }
        val catchingBody = ExceptionCatchingResponseBody(rawBody)
        return try {
            val body = responseConverter.convert(catchingBody)
            Response.success(body, rawResponse)
        } catch (e: java.lang.RuntimeException) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            catchingBody.throwIfCaught()
            throw e
        }
    }

    private class NoContentResponseBody(
        private val contentType: MediaType?,
        private val contentLength: Long
    ) : ResponseBody() {
        override fun contentType(): MediaType? {
            return contentType
        }

        override fun contentLength(): Long {
            return contentLength
        }

        override fun source(): BufferedSource {
            throw IllegalStateException("Cannot read raw response body of a converted body.")
        }
    }
}