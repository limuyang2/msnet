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
package msnet;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * Change the behavior of a {@code Call<BodyType>} return type to not use the {@linkplain
 * MSNet#callbackExecutor() callback executor} for invoking the {@link Callback#onResponse( Call ,
 * Response ) onResponse} or {@link Callback#onFailure(Call, Throwable) onFailure} methods.
 *
 * <pre><code>
 * &#64;SkipCallbackExecutor
 * &#64;GET("user/{id}/token")
 * Call&lt;String&gt; getToken(@Path("id") long id);
 * </code></pre>
 *
 * This annotation can also be used when a {@link CallAdapter.Factory} <em>explicitly</em> delegates
 * to the built-in factory for {@link Call} via {@link MSNet#nextCallAdapter(CallAdapter.Factory,
 * Type, Annotation[])} in order for the returned {@link Call} to skip the executor. (Note: by
 * default, a {@link Call} supplied directly to a {@link CallAdapter} will already skip the callback
 * executor. The annotation is only useful when looking up the built-in adapter.)
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface SkipCallbackExecutor {}
