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

import android.annotation.TargetApi
import okcronet.http.ResponseBody
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Optional

// Only added when Optional is available (Java 8+ / Android API 24+).
@TargetApi(24)
internal class OptionalConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type, annotations: Array<Annotation>, msNet: MSNet
    ): Converter<ResponseBody?, *>? {
        if (getRawType(type) != Optional::class.java) {
            return null
        }
        val innerType = getParameterUpperBound(0, type as ParameterizedType)
        val delegate: Converter<ResponseBody, Any> =
            msNet.responseBodyConverter(innerType, annotations)
        return OptionalConverter(delegate)
    }

    internal class OptionalConverter<T: Any>(private val delegate: Converter<ResponseBody, T>) :
        Converter<ResponseBody?, Optional<T>?> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): Optional<T> {
            return Optional.ofNullable<T>(delegate.convert(value))
        }
    }
}
