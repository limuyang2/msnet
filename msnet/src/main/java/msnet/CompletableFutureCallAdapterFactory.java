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

import android.annotation.TargetApi;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

// Only added when CompletableFuture is available (Java 8+ / Android API 24+).
@TargetApi(24)
final class CompletableFutureCallAdapterFactory extends CallAdapter.Factory {
  @Override
  public @Nullable CallAdapter<?, ?> get(
          @NonNull Type returnType, @NonNull Annotation[] annotations, @NonNull MSNet msNet) {
    if (getRawType(returnType) != CompletableFuture.class) {
      return null;
    }
    if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "CompletableFuture return type must be parameterized"
              + " as CompletableFuture<Foo> or CompletableFuture<? extends Foo>");
    }
    Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

    if (getRawType(innerType) != Response.class) {
      // Generic type is not Response<T>. Use it for body-only adapter.
      return new BodyCallAdapter<>(innerType);
    }

    // Generic type is Response<T>. Extract T and create the Response version of the adapter.
    if (!(innerType instanceof ParameterizedType)) {
      throw new IllegalStateException(
          "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
    }
    Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
    return new ResponseCallAdapter<>(responseType);
  }

  private static final class BodyCallAdapter<R> implements CallAdapter<R, CompletableFuture<R>> {
    private final Type responseType;

    BodyCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @NotNull
    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public CompletableFuture<R> adapt(@NotNull final Call<R> call) {
      CompletableFuture<R> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new BodyCallback(future));
      return future;
    }

    private class BodyCallback implements Callback<R> {
      private final CompletableFuture<R> future;

      public BodyCallback(CompletableFuture<R> future) {
        this.future = future;
      }

      @Override
      public void onResponse(@NotNull Call<R> call, @NotNull Response<R> response) {
        if (response.isSuccessful()) {
          future.complete(response.body());
        } else {
          future.completeExceptionally(new HttpException(response));
        }
      }

      @Override
      public void onFailure(@NotNull Call<R> call, @NotNull Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }

  private static final class ResponseCallAdapter<R>
      implements CallAdapter<R, CompletableFuture<Response<R>>> {
    private final Type responseType;

    ResponseCallAdapter(Type responseType) {
      this.responseType = responseType;
    }

    @NotNull
    @Override
    public Type responseType() {
      return responseType;
    }

    @Override
    public CompletableFuture<Response<R>> adapt(@NotNull final Call<R> call) {
      CompletableFuture<Response<R>> future = new CallCancelCompletableFuture<>(call);
      call.enqueue(new ResponseCallback(future));
      return future;
    }

    private class ResponseCallback implements Callback<R> {
      private final CompletableFuture<Response<R>> future;

      public ResponseCallback(CompletableFuture<Response<R>> future) {
        this.future = future;
      }

      @Override
      public void onResponse(@NotNull Call<R> call, @NotNull Response<R> response) {
        future.complete(response);
      }

      @Override
      public void onFailure(@NotNull Call<R> call, @NotNull Throwable t) {
        future.completeExceptionally(t);
      }
    }
  }

  private static final class CallCancelCompletableFuture<T> extends CompletableFuture<T> {
    private final Call<?> call;

    CallCancelCompletableFuture(Call<?> call) {
      this.call = call;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      if (mayInterruptIfRunning) {
        call.cancel();
      }
      return super.cancel(mayInterruptIfRunning);
    }
  }
}
