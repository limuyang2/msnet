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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** An HTTP response. */
public final class Response<T> {

  /**
   * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
   * body.
   */
  public static <T> Response<T> success(@Nullable T body, @NotNull okcronet.http.Response rawResponse) {
    if (!rawResponse.isSuccessful()) {
      throw new IllegalArgumentException("rawResponse must be successful response");
    }
    return new Response<>(rawResponse, body, null);
  }

  /** Create an error response from {@code rawResponse} with {@code body} as the error body. */
  public static <T> Response<T> error(@NotNull okcronet.http.ResponseBody body,@NotNull okcronet.http.Response rawResponse) {
    if (rawResponse.isSuccessful()) {
      throw new IllegalArgumentException("rawResponse should not be successful response");
    }
    return new Response<>(rawResponse, null, body);
  }

  private final @NotNull okcronet.http.Response rawResponse;
  private final @Nullable T body;
  private final @Nullable okcronet.http.ResponseBody errorBody;

  private Response(
          @NotNull okcronet.http.Response rawResponse, @Nullable T body, @Nullable okcronet.http.ResponseBody errorBody) {
    this.rawResponse = rawResponse;
    this.body = body;
    this.errorBody = errorBody;
  }

  /** The raw response from the HTTP client. */
  @NotNull
  public okcronet.http.Response raw() {
    return rawResponse;
  }

  /** HTTP status code. */
  public int code() {
    return rawResponse.getCode();
  }

  /** HTTP status message or null if unknown. */
  public String message() {
    return  rawResponse.getUrlResponseInfo().getHttpStatusText();
  }

  /** HTTP headers. */
  public List<Map.Entry<String, String>> getAllHeadersAsList() {
    return rawResponse.getUrlResponseInfo().getAllHeadersAsList();
  }

  public Map<String, List<String>> getAllHeaders() {
    return rawResponse.getUrlResponseInfo().getAllHeaders();
  }

  /** Returns true if {@link #code()} is in the range [200..300). */
  public boolean isSuccessful() {
    return rawResponse.isSuccessful();
  }

  /** The deserialized response body of a {@linkplain #isSuccessful() successful} response. */
  public @Nullable T body() {
    return body;
  }

  /** The raw response body of an {@linkplain #isSuccessful() unsuccessful} response. */
  public @Nullable okcronet.http.ResponseBody errorBody() {
    return errorBody;
  }

  @NotNull
  @Override
  public String toString() {
    return rawResponse.toString();
  }
}
