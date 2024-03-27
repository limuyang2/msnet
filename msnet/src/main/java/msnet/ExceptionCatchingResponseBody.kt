package msnet

import okcronet.http.MediaType
import okcronet.http.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.io.IOException

/**
 * @author 李沐阳
 * @date 2023/2/23
 * @description
 */
internal class ExceptionCatchingResponseBody(private val delegate: ResponseBody) : ResponseBody() {
    private val delegateSource: BufferedSource

    var thrownException: IOException? = null

    init {
        delegateSource = object : ForwardingSource(delegate.source()) {
            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                return try {
                    super.read(sink, byteCount)
                } catch (e: IOException) {
                    thrownException = e
                    throw e
                }
            }
        }.buffer()
    }

    override fun contentType(): MediaType? {
        return delegate.contentType()
    }

    override fun contentLength(): Long {
        return delegate.contentLength()
    }

    override fun source(): BufferedSource {
        return delegateSource
    }

    override fun close() {
        delegate.close()
    }

    @Throws(IOException::class)
    fun throwIfCaught() {
        thrownException?.let {
            throw it
        }
    }
}