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

import msnet.CallAdapter.Factory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Adapts a [Call] with response type `R` into the type of `T`. Instances are
 * created by [a factory][Factory] which is [ ][MSNet.Builder.addCallAdapterFactory] into the [MSNet] instance.
 */
interface CallAdapter<R, T> {
    /**
     * Returns the value type that this adapter uses when converting the HTTP response body to a Java
     * object. For example, the response type for `Call<Repo>` is `Repo`. This type is
     * used to prepare the `call` passed to `#adapt`.
     *
     *
     * Note: This is typically not the same type as the `returnType` provided to this call
     * adapter's factory.
     */
    fun responseType(): Type

    /**
     * Returns an instance of `T` which delegates to `call`.
     *
     *
     * For example, given an instance for a hypothetical utility, `Async`, this instance
     * would return a new `Async<R>` which invoked `call` when run.
     *
     * <pre>`
     * &#64;Override
     * public <R> Async<R> adapt(final Call<R> call) {
     * return Async.create(new Callable<Response<R>>() {
     * &#64;Override
     * public Response<R> call() throws Exception {
     * return call.execute();
     * }
     * });
     * }
    `</pre> *
     */
    fun adapt(call: Call<R>): T

    /**
     * Creates [CallAdapter] instances based on the return type of [ ][MSNet.create] methods.
     */
    abstract class Factory {
        /**
         * Returns a call adapter for interface methods that return `returnType`, or null if it
         * cannot be handled by this factory.
         */
        abstract operator fun get(
            returnType: Type, annotations: Array<Annotation>, msNet: MSNet
        ): CallAdapter<*, *>?

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
