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

import okcronet.http.ResponseBody

/** An HTTP response.  */
class Response<T> private constructor(
    private val rawResponse: okcronet.http.Response,
    private val body: T?,
    private val errorBody: ResponseBody?
) {
    /** The raw response from the HTTP client.  */
    fun raw(): okcronet.http.Response {
        return rawResponse
    }

    /** HTTP status code.  */
    fun code(): Int {
        return rawResponse.code
    }

    /** HTTP status message or null if unknown.  */
    fun message(): String {
        return rawResponse.urlResponseInfo.httpStatusText
    }

    val allHeadersAsList: List<Map.Entry<String, String>>
        /** HTTP headers.  */
        get() = rawResponse.urlResponseInfo.allHeadersAsList
    val allHeaders: Map<String, List<String>>
        get() = rawResponse.urlResponseInfo.allHeaders
    val isSuccessful: Boolean
        /** Returns true if [.code] is in the range [200..300).  */
        get() = rawResponse.isSuccessful

    /** The deserialized response body of a [successful][.isSuccessful] response.  */
    fun body(): T? {
        return body
    }

    /** The raw response body of an [unsuccessful][.isSuccessful] response.  */
    fun errorBody(): ResponseBody? {
        return errorBody
    }

    override fun toString(): String {
        return rawResponse.toString()
    }

    companion object {
        /**
         * Create a successful response from `rawResponse` with `body` as the deserialized
         * body.
         */
        fun <T> success(body: T?, rawResponse: okcronet.http.Response): Response<T> {
            require(rawResponse.isSuccessful) { "rawResponse must be successful response" }
            return Response(rawResponse, body, null)
        }

        /** Create an error response from `rawResponse` with `body` as the error body.  */
        fun <T> error(body: ResponseBody, rawResponse: okcronet.http.Response): Response<T> {
            require(!rawResponse.isSuccessful) { "rawResponse should not be successful response" }
            return Response(rawResponse, null, body)
        }
    }
}
