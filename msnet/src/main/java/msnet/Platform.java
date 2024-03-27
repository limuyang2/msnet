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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.ChecksSdkIntAtLeast;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

abstract class Platform {
  private static final Platform PLATFORM = createPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform createPlatform() {
    if (Build.VERSION.SDK_INT >= 24) {
      return new Android24();
    }
    return new Android21();
  }

  abstract @Nullable Executor defaultCallbackExecutor();

  abstract List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
      @Nullable Executor callbackExecutor);

  abstract List<? extends Converter.Factory> createDefaultConverterFactories();

  abstract boolean isDefaultMethod(Method method);

  abstract @Nullable Object invokeDefaultMethod(
      Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable;

  private static final class Android21 extends Platform {
    @Override
    boolean isDefaultMethod(Method method) {
      return false;
    }

    @Nullable
    @Override
    Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) {
      throw new AssertionError();
    }

    @Override
    Executor defaultCallbackExecutor() {
      return MainThreadExecutor.INSTANCE;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return singletonList(new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return emptyList();
    }
  }

  // Only used on Android API 24+
  @TargetApi(24)
  private static final class Android24 extends Platform {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)

    private @Nullable Constructor<Lookup> lookupConstructor;

    @Override
    Executor defaultCallbackExecutor() {
      return MainThreadExecutor.INSTANCE;
    }

    @Override
    List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
        @Nullable Executor callbackExecutor) {
      return asList(
          new CompletableFutureCallAdapterFactory(),
          new DefaultCallAdapterFactory(callbackExecutor));
    }

    @Override
    List<? extends Converter.Factory> createDefaultConverterFactories() {
      return singletonList(new OptionalConverterFactory());
    }

    @Override
    public boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Nullable
    @Override
    public Object invokeDefaultMethod(
        Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable {
      if (Build.VERSION.SDK_INT < 26) {
        throw new UnsupportedOperationException(
            "Calling default methods on API 24 and 25 is not supported");
      }
      Constructor<Lookup> lookupConstructor = this.lookupConstructor;
      if (lookupConstructor == null) {
        lookupConstructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
        lookupConstructor.setAccessible(true);
        this.lookupConstructor = lookupConstructor;
      }
      return lookupConstructor
          .newInstance(declaringClass, -1 /* trusted */)
          .unreflectSpecial(method, declaringClass)
          .bindTo(proxy)
          .invokeWithArguments(args);
    }
  }


  private static final class MainThreadExecutor implements Executor {
    static final Executor INSTANCE = new MainThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable r) {
      handler.post(r);
    }
  }
}
