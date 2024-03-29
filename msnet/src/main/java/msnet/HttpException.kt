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

import java.util.Objects

/** Exception for an unexpected, non-2xx HTTP response.  */
open class HttpException(@field:Transient private val response: Response<*>) : RuntimeException(
    getMessage(response)
) {
    private val _code: Int = response.code()
    private val _message: String = response.message()


    /** HTTP status code.  */
    fun code(): Int {
        return _code
    }

    /** HTTP status message.  */
    fun message(): String {
        return _message
    }

    /** The full HTTP response. This may be null if the exception was serialized.  */
    fun response(): Response<*> {
        return response
    }

    companion object {
        private fun getMessage(response: Response<*>): String {
            Objects.requireNonNull(response, "response == null")
            return "HTTP " + response.code() + " " + response.message()
        }
    }
}
