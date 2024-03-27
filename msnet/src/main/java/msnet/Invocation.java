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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single invocation of a Retrofit service interface method. This class captures both the method
 * that was called and the arguments to the method.
 *
 * <p>Retrofit automatically adds an invocation to each OkHttp request as a tag. You can retrieve
 * the invocation in an OkHttp interceptor for metrics and monitoring.
 *
 * <pre><code>
 * class InvocationLogger implements Interceptor {
 *   &#64;Override public Response intercept(Chain chain) throws IOException {
 *     Request request = chain.request();
 *     Invocation invocation = request.tag(Invocation.class);
 *     if (invocation != null) {
 *       System.out.printf("%s.%s %s%n",
 *           invocation.method().getDeclaringClass().getSimpleName(),
 *           invocation.method().getName(), invocation.arguments());
 *     }
 *     return chain.proceed(request);
 *   }
 * }
 * </code></pre>
 *
 * <strong>Note:</strong> use caution when examining an invocation's arguments. Although the
 * arguments list is unmodifiable, the arguments themselves may be mutable. They may also be unsafe
 * for concurrent access. For best results declare Retrofit service interfaces using only immutable
 * types for parameters!
 */
public final class Invocation {
  public static Invocation of(Method method, List<?> arguments) {
    Objects.requireNonNull(method, "method == null");
    Objects.requireNonNull(arguments, "arguments == null");
    return new Invocation(method, new ArrayList<>(arguments)); // Defensive copy.
  }

  private final Method method;
  private final List<?> arguments;

  /** Trusted constructor assumes ownership of {@code arguments}. */
  Invocation(Method method, List<?> arguments) {
    this.method = method;
    this.arguments = Collections.unmodifiableList(arguments);
  }

  public Method method() {
    return method;
  }

  public List<?> arguments() {
    return arguments;
  }

  @NotNull
  @Override
  public String toString() {
    return String.format(
        "%s.%s() %s", method.getDeclaringClass().getName(), method.getName(), arguments);
  }
}
