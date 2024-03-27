/*
 * MIT License
 *
 * Copyright (c) 2023 LiMuYang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package msnet

import okio.Timeout
import java.io.IOException

/**
 * An invocation of a Retrofit method that sends a request to a webserver and returns a response.
 * Each call yields its own HTTP request and response pair. Use [.clone] to make multiple
 * calls with the same parameters to the same webserver; this may be used to implement polling or to
 * retry a failed call.
 *
 *
 * Calls may be executed synchronously with [.execute], or asynchronously with [ ][.enqueue]. In either case the call can be canceled at any time with [.cancel]. A call that
 * is busy writing its request or reading its response may receive a [IOException]; this is
 * working as designed.
 *
 * @param <T> Successful response body type.
</T> */
interface Call<T> : Cloneable {
    /**
     * Synchronously send the request and return its response.
     *
     * @throws IOException if a problem occurred talking to the server.
     * @throws RuntimeException (and subclasses) if an unexpected error occurs creating the request or
     * decoding the response.
     */
    @Throws(IOException::class)
    fun execute(): Response<T>

    /**
     * Asynchronously send the request and notify `callback` of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    fun enqueue(callback: Callback<T>)

    /**
     * Returns true if this call has been either [executed][.execute] or [ ][.enqueue]. It is an error to execute or enqueue a call more than once.
     */
    val isExecuted: Boolean

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    fun cancel()

    /** True if [.cancel] was called.  */
    val isCanceled: Boolean

    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call
     * has already been.
     */
    public override fun clone(): Call<T>

    /** The original HTTP request.  */
    fun request(): okcronet.http.Request

    /**
     * Returns a timeout that spans the entire call: resolving DNS, connecting, writing the request
     * body, server processing, and reading the response body. If the call requires redirects or
     * retries all must complete within one timeout period.
     */
    fun timeout(): Timeout?
}