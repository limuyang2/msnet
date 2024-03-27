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

import msnet.annotation.*
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Convert objects to and from their representation in HTTP. Instances are created by [ ] which is [installed][MSNet.Builder.addConverterFactory]
 * into the [MSNet] instance.
 */
interface Converter<F, T> {
    @Throws(IOException::class)
    fun convert(value: F & Any): T?

    /** Creates [Converter] instances based on a type and target usage.  */
    abstract class Factory {
        /**
         * Returns a [Converter] for converting an HTTP response body to `type`, or null if
         * `type` cannot be handled by this factory. This is used to create converters for
         * response types such as `SimpleResponse` from a `Call<SimpleResponse>`
         * declaration.
         */
        open fun responseBodyConverter(
            type: Type, annotations: Array<Annotation>, msNet: MSNet
        ): Converter<okcronet.http.ResponseBody?, *>? {
            return null
        }

        /**
         * Returns a [Converter] for converting `type` to an HTTP request body, or null if
         * `type` cannot be handled by this factory. This is used to create converters for types
         * specified by [@Body][Body], [@Part][Part], and [@PartMap][PartMap] values.
         */
        open fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            MSNet: MSNet
        ): Converter<*, okcronet.http.RequestBody?>? {
            return null
        }

        /**
         * Returns a [Converter] for converting `type` to a [String], or null if
         * `type` cannot be handled by this factory. This is used to create converters for types
         * specified by [Field], [FieldMap] values, [Header],
         * [HeaderMap], [@Path][Path], [Query], and [QueryMap] values.
         */
        fun stringConverter(
            type: Type, annotations: Array<Annotation>, msNet: MSNet
        ): Converter<*, String>? {
            return null
        }

        companion object {
            /**
             * Extract the upper bound of the generic parameter at `index` from `type`. For
             * example, index 1 of `Map<String, ? extends Runnable>` returns `Runnable`.
             */
            @JvmStatic
            protected fun getParameterUpperBound(index: Int, type: ParameterizedType?): Type {
                return Utils.getParameterUpperBound(index, type)
            }

            /**
             * Extract the raw class type from `type`. For example, the type representing `List<? extends Runnable>` returns `List.class`.
             */
            @JvmStatic
            protected fun getRawType(type: Type?): Class<*> {
                return Utils.getRawType(type)
            }
        }
    }
}