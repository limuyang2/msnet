/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package msnet

import msnet.annotation.*
import okcronet.CronetClient
import okcronet.http.HttpUrl
import okcronet.http.HttpUrl.Companion.toHttpUrl
import okcronet.http.RequestBody
import okcronet.http.ResponseBody
import java.lang.reflect.*
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor


/**
 * MShare 网络请求框架。改造自 Retrofit，使用 Cronet 进行网络请求
 *
 */
class MSNet private constructor(
    val cronetClient: CronetClient,
    val baseUrl: HttpUrl,
    val converterFactories: List<Converter.Factory>,
    val defaultConverterFactoriesSize: Int,
    val callAdapterFactories: List<CallAdapter.Factory>,
    val defaultCallAdapterFactoriesSize: Int,
    val callbackExecutor: Executor?,
    val validateEagerly: Boolean
) {
    private val serviceMethodCache: MutableMap<Method, ServiceMethod<*>> = ConcurrentHashMap()


    fun cronetEngineVersionString() = cronetClient.cronetEngine.versionString

    /**
     * Create an implementation of the API endpoints defined by the `service` interface.
     *
     *
     * The relative path for a given method is obtained from an annotation on the method describing
     * the request type. The built-in methods are [GET], [PUT], [POST],
     * [PATCH], [HEAD], [DELETE] and [OPTIONS].
     * You can use a custom HTTP method with [@HTTP][HTTP]. For a dynamic URL, omit the path on
     * the annotation and annotate the first parameter with [@Url][Url].
     *
     *
     * Method parameters can be used to replace parts of the URL by annotating them with [ ]. Replacement sections are denoted by an identifier surrounded by
     * curly braces (e.g., "{foo}"). To add items to the query string of a URL use [@Query][Query].
     *
     *
     * The body of a request is denoted by the [@Body][Body] annotation. The
     * object will be converted to request representation by one of the [Converter.Factory]
     * instances. A [RequestBody] can also be used for a raw representation.
     *
     *
     * Alternative request body formats are supported by method annotations and corresponding
     * parameter annotations:
     *
     *
     *  * [@FormUrlEncoded][FormUrlEncoded] - Form-encoded data with key-value
     * pairs specified by the [Field] parameter annotation.
     *  * [Multipart] - RFC 2388-compliant multipart data with
     * parts specified by the [Part] parameter annotation.
     *
     *
     *
     * Additional static headers can be added for an endpoint using the [@Headers][Headers]
     * method annotation. For per-request control over a header
     * annotate a parameter with [@Header][Header].
     *
     *
     * By default, methods return a [Call] which represents the HTTP request. The generic
     * parameter of the call is the response body type and will be converted by one of the [ ] instances. [ResponseBody] can also be used for a raw representation.
     * [Void] can be used if you do not care about the body contents.
     *
     *
     * For example:
     *
     * <pre>
     * public interface CategoryService {
     * &#64;POST("category/{cat}/")
     * Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("cat") String a, @Query("page") int b);
     * }
    </pre> *
     */
    // Single-interface proxy creation guarded by parameter safety.
    fun <T> create(service: Class<T>): T {
        validateServiceInterface(service)
        return Proxy.newProxyInstance(
            service.classLoader, arrayOf<Class<*>>(service),
            object : InvocationHandler {

                @Throws(Throwable::class)
                override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
                    // If the method is a method from Object then defer to normal invocation.
                    if (method.declaringClass == Any::class.java) {
                        return method.invoke(this, args)
                    }

                    val mArgs: Array<Any> = args ?: emptyArray()
                    val platform = Platform.get()
                    return if (platform.isDefaultMethod(method)) platform.invokeDefaultMethod(
                        method,
                        service,
                        proxy,
                        *mArgs
                    ) else loadServiceMethod(method).invoke(mArgs)
                }
            }) as T
    }

    private fun validateServiceInterface(service: Class<*>) {
        require(service.isInterface) { "API declarations must be interfaces." }
        val check: Deque<Class<*>> = ArrayDeque(1)
        check.add(service)
        while (!check.isEmpty()) {
            val candidate = check.removeFirst()
            if (candidate.typeParameters.isNotEmpty()) {
                val message =
                    StringBuilder("Type parameters are unsupported on ").append(candidate.name)
                if (candidate != service) {
                    message.append(" which is an interface of ").append(service.name)
                }
                throw IllegalArgumentException(message.toString())
            }
            Collections.addAll(check, *candidate.interfaces)
        }
        if (validateEagerly) {
            val platform = Platform.get()
            for (method in service.declaredMethods) {
                if (!platform.isDefaultMethod(method) && !Modifier.isStatic(method.modifiers)) {
                    loadServiceMethod(method)
                }
            }
        }
    }

    private fun loadServiceMethod(method: Method): ServiceMethod<*> {
        val result = serviceMethodCache[method]
        if (result != null) return result
        return synchronized(serviceMethodCache) {
            serviceMethodCache[method] ?: ServiceMethod.parseAnnotations<Any>(
                this,
                method,
                cronetClient
            ).apply {
                serviceMethodCache[method] = this
            }
        }
    }

    /** The API base URL.  */
    fun baseUrl(): HttpUrl {
        return baseUrl
    }

    /**
     * Returns a list of the factories tried when creating a [.callAdapter] call adapter}.
     */
    fun callAdapterFactories(): List<CallAdapter.Factory> {
        return callAdapterFactories
    }

    /**
     * Returns the [CallAdapter] for `returnType` from the available [ ][.callAdapterFactories].
     *
     * @throws IllegalArgumentException if no call adapter available for `type`.
     */
    fun callAdapter(returnType: Type, annotations: Array<Annotation>): CallAdapter<*, *> {
        return nextCallAdapter(null, returnType, annotations)
    }

    /**
     * Returns the [CallAdapter] for `returnType` from the available [ ][.callAdapterFactories] except `skipPast`.
     *
     * @throws IllegalArgumentException if no call adapter available for `type`.
     */
    fun nextCallAdapter(
        skipPast: CallAdapter.Factory?, returnType: Type, annotations: Array<Annotation>
    ): CallAdapter<*, *> {
        val start = callAdapterFactories.indexOf(skipPast) + 1
        run {
            var i = start
            val count = callAdapterFactories.size
            while (i < count) {
                val adapter = callAdapterFactories[i][returnType, annotations, this]
                if (adapter != null) {
                    return adapter
                }
                i++
            }
        }
        val builder =
            StringBuilder("Could not locate call adapter for ").append(returnType).append(".\n")
        if (skipPast != null) {
            builder.append("  Skipped:")
            for (i in 0 until start) {
                builder.append("\n   * ").append(callAdapterFactories[i].javaClass.name)
            }
            builder.append('\n')
        }
        builder.append("  Tried:")
        var i = start
        val count = callAdapterFactories.size
        while (i < count) {
            builder.append("\n   * ").append(callAdapterFactories[i].javaClass.name)
            i++
        }
        throw IllegalArgumentException(builder.toString())
    }

    /**
     * Returns an unmodifiable list of the factories tried when creating a [ ][.requestBodyConverter], a [ ][.responseBodyConverter], or a [ ][.stringConverter].
     */
    fun converterFactories(): List<Converter.Factory?> {
        return converterFactories
    }

    /**
     * Returns a [Converter] for `type` to [RequestBody] from the available
     * [factories][.converterFactories].
     *
     * @throws IllegalArgumentException if no converter available for `type`.
     */
    fun <T> requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>
    ): Converter<T, RequestBody> {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations)
    }

    /**
     * Returns a [Converter] for `type` to [RequestBody] from the available
     * [factories][.converterFactories] except `skipPast`.
     *
     * @throws IllegalArgumentException if no converter available for `type`.
     */
    private fun <T> nextRequestBodyConverter(
        skipPast: Converter.Factory?,
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>
    ): Converter<T, RequestBody> {
        val start = converterFactories.indexOf(skipPast) + 1
        run {
            var i = start
            val count = converterFactories.size
            while (i < count) {
                val factory = converterFactories[i]
                val converter = factory.requestBodyConverter(
                    type,
                    parameterAnnotations,
                    methodAnnotations,
                    this
                )
                if (converter != null) {
                    return converter as Converter<T, RequestBody>
                }
                i++
            }
        }
        val builder =
            StringBuilder("Could not locate RequestBody converter for ").append(type).append(".\n")
        if (skipPast != null) {
            builder.append("  Skipped:")
            for (i in 0 until start) {
                builder.append("\n   * ").append(converterFactories[i].javaClass.name)
            }
            builder.append('\n')
        }
        builder.append("  Tried:")
        var i = start
        val count = converterFactories.size
        while (i < count) {
            builder.append("\n   * ").append(converterFactories[i].javaClass.name)
            i++
        }
        throw IllegalArgumentException(builder.toString())
    }

    /**
     * Returns a [Converter] for [ResponseBody] to `type` from the available
     * [factories][.converterFactories].
     *
     * @throws IllegalArgumentException if no converter available for `type`.
     */
    fun <T> responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>
    ): Converter<ResponseBody, T> {
        return nextResponseBodyConverter(null, type, annotations)
    }

    /**
     * Returns a [Converter] for [ResponseBody] to `type` from the available
     * [factories][.converterFactories] except `skipPast`.
     *
     * @throws IllegalArgumentException if no converter available for `type`.
     */
    private fun <T> nextResponseBodyConverter(
        skipPast: Converter.Factory?, type: Type, annotations: Array<Annotation>
    ): Converter<ResponseBody, T> {
        val start = converterFactories.indexOf(skipPast) + 1

        var i = start
        val count = converterFactories.size
        while (i < count) {
            val converter = converterFactories[i]
                .responseBodyConverter(type, annotations, this)
            if (converter != null) {
                return converter as Converter<ResponseBody, T>
            }
            i++
        }

        val builder = StringBuilder("Could not locate ResponseBody converter for ")
            .append(type)
            .append(".\n")
        if (skipPast != null) {
            builder.append("  Skipped:")
            for (n in 0 until start) {
                builder.append("\n   * ").append(converterFactories[n].javaClass.name)
            }
            builder.append('\n')
        }
        builder.append("  Tried:")
        var i2 = start
        val count2 = converterFactories.size
        while (i < count2) {
            builder.append("\n   * ").append(converterFactories[i2].javaClass.name)
            i2++
        }
        throw IllegalArgumentException(builder.toString())
    }

    /**
     * Returns a [Converter] for `type` to [String] from the available [ ][.converterFactories].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> stringConverter(type: Type, annotations: Array<Annotation>): Converter<T, String> {
        var i = 0
        val count = converterFactories.size
        while (i < count) {
            val converter = converterFactories[i]
                .stringConverter(type, annotations, this)
            if (converter != null) {
                return converter as Converter<T, String>
            }
            i++
        }

        // Nothing matched. Resort to default converter which just calls toString().
        return BuiltInConverters.ToStringConverter.INSTANCE as Converter<T, String>
    }

    /**
     * The executor used for [Callback] methods on a [Call]. This may be `null`, in
     * which case callbacks should be made synchronously on the background thread.
     */
    fun callbackExecutor(): Executor? {
        return callbackExecutor
    }

    fun newBuilder(): Builder {
        return Builder(this)
    }

    /**
     * Build a new [MSNet].
     *
     *
     * Calling [.baseUrl] is required before calling [.build]. All other methods are
     * optional.
     */
    class Builder {
        private var baseUrl: HttpUrl? = null
        private val converterFactories: MutableList<Converter.Factory> = ArrayList()
        private val callAdapterFactories: MutableList<CallAdapter.Factory> = ArrayList()
        private var callbackExecutor: Executor? = null
        private var validateEagerly = false
        private var cronetClient: CronetClient? = null

        constructor()
        internal constructor(msNet: MSNet) {
            cronetClient = msNet.cronetClient
            baseUrl = msNet.baseUrl

            // Do not add the default BuiltIntConverters and platform-aware converters added by build().
            run {
                var i = 1
                val size = msNet.converterFactories.size - msNet.defaultConverterFactoriesSize
                while (i < size) {
                    converterFactories.add(msNet.converterFactories[i])
                    i++
                }
            }

            // Do not add the default, platform-aware call adapters added by build().
            var i = 0
            val size = msNet.callAdapterFactories.size - msNet.defaultCallAdapterFactoriesSize
            while (i < size) {
                callAdapterFactories.add(msNet.callAdapterFactories[i])
                i++
            }
            callbackExecutor = msNet.callbackExecutor
            validateEagerly = msNet.validateEagerly
        }

        fun cronet(cronetClient: CronetClient?): Builder {
            this.cronetClient = cronetClient
            return this
        }

        /**
         * Set the API base URL.
         *
         * @see .baseUrl
         */
        fun baseUrl(baseUrl: URL): Builder {
            return baseUrl(baseUrl.toString().toHttpUrl())
        }

        /**
         * Set the API base URL.
         *
         * @see .baseUrl
         */
        fun baseUrl(baseUrl: String): Builder {
            return baseUrl(baseUrl.toHttpUrl())
        }

        /**
         * Set the API base URL.
         *
         *
         * The specified endpoint values (such as with [@GET][GET]) are resolved against this
         * value using [HttpUrl.resolve]. The behavior of this matches that of an `<a href="">` link on a website resolving on the current URL.
         *
         *
         * **Base URLs should always end in `/`.**
         *
         *
         * A trailing `/` ensures that endpoints values which are relative paths will correctly
         * append themselves to a base which has path components.
         *
         *
         * **Correct:**<br></br>
         * Base URL: http://example.com/api/<br></br>
         * Endpoint: foo/bar/<br></br>
         * Result: http://example.com/api/foo/bar/
         *
         *
         * **Incorrect:**<br></br>
         * Base URL: http://example.com/api<br></br>
         * Endpoint: foo/bar/<br></br>
         * Result: http://example.com/foo/bar/
         *
         *
         * This method enforces that `baseUrl` has a trailing `/`.
         *
         *
         * **Endpoint values which contain a leading `/` are absolute.**
         *
         *
         * Absolute values retain only the host from `baseUrl` and ignore any specified path
         * components.
         *
         *
         * Base URL: http://example.com/api/<br></br>
         * Endpoint: /foo/bar/<br></br>
         * Result: http://example.com/foo/bar/
         *
         *
         * Base URL: http://example.com/<br></br>
         * Endpoint: /foo/bar/<br></br>
         * Result: http://example.com/foo/bar/
         *
         *
         * **Endpoint values may be a full URL.**
         *
         *
         * Values which have a host replace the host of `baseUrl` and values also with a scheme
         * replace the scheme of `baseUrl`.
         *
         *
         * Base URL: http://example.com/<br></br>
         * Endpoint: https://github.com/square/retrofit/<br></br>
         * Result: https://github.com/square/retrofit/
         *
         *
         * Base URL: http://example.com<br></br>
         * Endpoint: //github.com/square/retrofit/<br></br>
         * Result: http://github.com/square/retrofit/ (note the scheme stays 'http')
         */
        fun baseUrl(baseUrl: HttpUrl): Builder {
            val pathSegments = baseUrl.pathSegments
            require("" == pathSegments[pathSegments.size - 1]) { "baseUrl must end in /: $baseUrl" }
            this.baseUrl = baseUrl
            return this
        }

        /** Add converter factory for serialization and deserialization of objects.  */
        fun addConverterFactory(factory: Converter.Factory): Builder {
            converterFactories.add(factory)
            return this
        }

        /**
         * Add a call adapter factory for supporting service method return types other than [ ].
         */
        fun addCallAdapterFactory(factory: CallAdapter.Factory): Builder {
            callAdapterFactories.add(factory)
            return this
        }

        /**
         * The executor on which [Callback] methods are invoked when returning [Call] from
         * your service method.
         *
         *
         * Note: `executor` is not used for [custom method][.addCallAdapterFactory].
         */
        fun callbackExecutor(executor: Executor): Builder {
            callbackExecutor = executor
            return this
        }

        /** Returns a modifiable list of call adapter factories.  */
        fun callAdapterFactories(): List<CallAdapter.Factory> {
            return callAdapterFactories
        }

        /** Returns a modifiable list of converter factories.  */
        fun converterFactories(): List<Converter.Factory> {
            return converterFactories
        }

        /**
         * When calling [.create] on the resulting [MSNet] instance, eagerly validate the
         * configuration of all methods in the supplied interface.
         */
        fun validateEagerly(validateEagerly: Boolean): Builder {
            this.validateEagerly = validateEagerly
            return this
        }

        /**
         * Create the [MSNet] instance using the configured values.
         */
        fun build(): MSNet {
            checkNotNull(baseUrl) { "Base URL required." }
            checkNotNull(cronetClient) { "CronetClient required." }

            val platform = Platform.get()
            val callbackExecutor = callbackExecutor ?: platform.defaultCallbackExecutor()

            // Make a defensive copy of the adapters and add the default Call adapter.
            val callAdapterFactories: MutableList<CallAdapter.Factory> = ArrayList(
                callAdapterFactories
            )
            val defaultCallAdapterFactories =
                platform.createDefaultCallAdapterFactories(callbackExecutor)
            callAdapterFactories.addAll(defaultCallAdapterFactories)

            // Make a defensive copy of the converters.
            val defaultConverterFactories = platform.createDefaultConverterFactories()
            val defaultConverterFactoriesSize = defaultConverterFactories.size
            val converterFactories: MutableList<Converter.Factory> =
                ArrayList(1 + converterFactories.size + defaultConverterFactoriesSize)

            // Add the built-in converter factory first. This prevents overriding its behavior but also
            // ensures correct behavior when using converters that consume all types.
            converterFactories.add(BuiltInConverters())
            converterFactories.addAll(this.converterFactories)
            converterFactories.addAll(defaultConverterFactories)
            return MSNet(
                cronetClient!!,
                baseUrl!!,
                converterFactories,
                defaultConverterFactoriesSize,
                callAdapterFactories,
                defaultCallAdapterFactories.size,
                callbackExecutor,
                validateEagerly
            )
        }
    }
}