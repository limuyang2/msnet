/*
 * Copyright (C) 2015 Square, Inc.
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
package msnet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import kotlin.Unit;
import msnet.annotation.Streaming;
import okcronet.http.RequestBody;
import okcronet.http.ResponseBody;

final class BuiltInConverters extends Converter.Factory {

  @Override
  public @Nullable Converter<ResponseBody, ?> responseBodyConverter(
          @NotNull Type type, @NotNull Annotation[] annotations, @NotNull MSNet msNet) {
    if (type == ResponseBody.class) {
      return Utils.isAnnotationPresent(annotations, Streaming.class)
          ? StreamingResponseBodyConverter.INSTANCE
          : BufferingResponseBodyConverter.INSTANCE;
    }
    if (type == Void.class) {
      return VoidResponseBodyConverter.INSTANCE;
    }
    if (Utils.isUnit(type)) {
      return UnitResponseBodyConverter.INSTANCE;
    }
    return null;
  }

  @Override
  public @Nullable Converter<?, RequestBody> requestBodyConverter(
          @NotNull Type type,
          @NotNull Annotation[] parameterAnnotations,
          @NotNull Annotation[] methodAnnotations,
          @NotNull MSNet msNet) {
    if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
      return RequestBodyConverter.INSTANCE;
    }
    return null;
  }

  static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
    static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

    @Override
    public Void convert(@NotNull ResponseBody value) {
      value.close();
      return null;
    }
  }

  static final class UnitResponseBodyConverter implements Converter<ResponseBody, Unit> {
    static final UnitResponseBodyConverter INSTANCE = new UnitResponseBodyConverter();

    @Override
    public Unit convert(@NotNull ResponseBody value) {
      value.close();
      return Unit.INSTANCE;
    }
  }

  static final class RequestBodyConverter implements Converter<RequestBody, RequestBody> {
    static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

    @Override
    public RequestBody convert(@NotNull RequestBody value) {
      return value;
    }
  }

  static final class StreamingResponseBodyConverter
      implements Converter<ResponseBody, ResponseBody> {
    static final StreamingResponseBodyConverter INSTANCE = new StreamingResponseBodyConverter();

    @Override
    public ResponseBody convert(@NotNull ResponseBody value) {
      return value;
    }
  }

  static final class BufferingResponseBodyConverter
      implements Converter<ResponseBody, ResponseBody> {
    static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

    @Override
    public ResponseBody convert(@NotNull ResponseBody value) throws IOException {
      try {
        // Buffer the entire body to avoid future I/O.
        return Utils.buffer(value);
      } finally {
        value.close();
      }
    }
  }

  static final class ToStringConverter implements Converter<Object, String> {
    static final ToStringConverter INSTANCE = new ToStringConverter();

    @Override
    public String convert(@NotNull Object value) {
      return value.toString();
    }
  }
}
