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

import java.io.EOFException;
import java.io.IOException;
import java.util.regex.Pattern;

import okcronet.http.FormBody;
import okcronet.http.Headers;
import okcronet.http.HttpUrl;
import okcronet.http.MediaType;
import okcronet.http.MultipartBody;
import okcronet.http.Request;
import okcronet.http.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

final class RequestBuilder {
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final String PATH_SEGMENT_ALWAYS_ENCODE_SET = " \"<>^`{}|\\?#";

    /**
     * Matches strings that contain {@code .} or {@code ..} as a complete path segment. This also
     * matches dots in their percent-encoded form, {@code %2E}.
     *
     * <p>It is okay to have these strings within a larger path segment (like {@code a..z} or {@code
     * index.html}) but when alone they have a special meaning. A single dot resolves to no path
     * segment so {@code /one/./three/} becomes {@code /one/three/}. A double-dot pops the preceding
     * directory, so {@code /one/../three/} becomes {@code /three/}.
     *
     * <p>We forbid these in Retrofit paths because they're likely to have the unintended effect. For
     * example, passing {@code ..} to {@code DELETE /account/book/{isbn}/} yields {@code DELETE
     * /account/}.
     */
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(.*/)?(\\.|%2e|%2E){1,2}(/.*)?");

    private final String method;

    private final HttpUrl baseUrl;
    private @Nullable String relativeUrl;
    private @Nullable HttpUrl.Builder urlBuilder;

    private final Request.Builder requestBuilder = new Request.Builder();
    private final Headers.Builder headersBuilder;
    private @Nullable MediaType contentType;

    private final boolean hasBody;
    private @Nullable MultipartBody.Builder multipartBuilder;
    private @Nullable FormBody.Builder formBuilder;
    private @Nullable RequestBody body;

    private final int priority;

    private final boolean disableCache;

    RequestBuilder(
            String method,
            int priority,
            boolean disableCache,
            HttpUrl baseUrl,
            @Nullable String relativeUrl,
            @Nullable Headers headers,
            @Nullable MediaType contentType,
            boolean hasBody,
            boolean isFormEncoded,
            boolean isMultipart) {
        this.method = method;
        this.priority = priority;
        this.disableCache = disableCache;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.contentType = contentType;
        this.hasBody = hasBody;

        if (headers != null) {
            headersBuilder = headers.newBuilder();
        } else {
            headersBuilder = new Headers.Builder();
        }

        if (isFormEncoded) {
            // Will be set to 'body' in 'build'.
            formBuilder = new FormBody.Builder();
        } else if (isMultipart) {
            // Will be set to 'body' in 'build'.
            multipartBuilder = new MultipartBody.Builder();
            multipartBuilder.setType(MultipartBody.FORM);
        }
    }

    void setRelativeUrl(Object relativeUrl) {
        this.relativeUrl = relativeUrl.toString();
    }

    void addHeader(String name, String value, boolean allowUnsafeNonAsciiValues) {
        if ("Content-Type".equalsIgnoreCase(name)) {
            try {
                contentType = MediaType.toMediaType(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Malformed content type: " + value, e);
            }
        } else if (allowUnsafeNonAsciiValues) {
            headersBuilder.addUnsafeNonAscii(name, value);
        } else {
            headersBuilder.add(name, value);
        }
    }

    void addHeaders(Headers headers) {
        headersBuilder.addAll(headers);
    }

    void addPathParam(String name, String value, boolean encoded) {
        if (relativeUrl == null) {
            // The relative URL is cleared when the first query parameter is set.
            throw new AssertionError();
        }
        String replacement = canonicalizeForPath(value, encoded);
        String newRelativeUrl = relativeUrl.replace("{" + name + "}", replacement);
        if (PATH_TRAVERSAL.matcher(newRelativeUrl).matches()) {
            throw new IllegalArgumentException(
                    "@Path parameters shouldn't perform path traversal ('.' or '..'): " + value);
        }
        relativeUrl = newRelativeUrl;
    }

    private static String canonicalizeForPath(String input, boolean alreadyEncoded) {
        int codePoint;
        for (int i = 0, limit = input.length(); i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (codePoint < 0x20
                    || codePoint >= 0x7f
                    || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
                    || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
                // Slow path: the character at i requires encoding!
                Buffer out = new Buffer();
                out.writeUtf8(input, 0, i);
                canonicalizeForPath(out, input, i, limit, alreadyEncoded);
                return out.readUtf8();
            }
        }

        // Fast path: no characters required encoding.
        return input;
    }

    private static void canonicalizeForPath(
            Buffer out, String input, int pos, int limit, boolean alreadyEncoded) {
        Buffer utf8Buffer = null; // Lazily allocated.
        int codePoint;
        for (int i = pos; i < limit; i += Character.charCount(codePoint)) {
            codePoint = input.codePointAt(i);
            if (alreadyEncoded
                    && (codePoint == '\t' || codePoint == '\n' || codePoint == '\f' || codePoint == '\r')) {
                // Skip this character.
            } else if (codePoint < 0x20
                    || codePoint >= 0x7f
                    || PATH_SEGMENT_ALWAYS_ENCODE_SET.indexOf(codePoint) != -1
                    || (!alreadyEncoded && (codePoint == '/' || codePoint == '%'))) {
                // Percent encode this character.
                if (utf8Buffer == null) {
                    utf8Buffer = new Buffer();
                }
                utf8Buffer.writeUtf8CodePoint(codePoint);
                while (!utf8Buffer.exhausted()) {
                    int b;
                    try {
                        b = utf8Buffer.readByte() & 0xff;
                    } catch (EOFException e) {
                        throw new RuntimeException(e);
                    }
                    out.writeByte('%');
                    out.writeByte(HEX_DIGITS[(b >> 4) & 0xf]);
                    out.writeByte(HEX_DIGITS[b & 0xf]);
                }
            } else {
                // This character doesn't need encoding. Just copy it over.
                out.writeUtf8CodePoint(codePoint);
            }
        }
    }

    void addQueryParam(String name, @Nullable String value, boolean encoded) {
        if (relativeUrl != null) {
            // Do a one-time combination of the built relative URL and the base URL.
            urlBuilder = baseUrl.newBuilder(relativeUrl);
            if (urlBuilder == null) {
                throw new IllegalArgumentException(
                        "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
            }
            relativeUrl = null;
        }

        if (encoded) {
            //noinspection ConstantConditions Checked to be non-null by above 'if' block.
            urlBuilder.addEncodedQueryParameter(name, value);
        } else {
            //noinspection ConstantConditions Checked to be non-null by above 'if' block.
            urlBuilder.addQueryParameter(name, value);
        }
    }

        // Only called when isFormEncoded was true.
    void addFormField(String name, String value, boolean encoded) {
        assert formBuilder != null;
        if (encoded) {
            formBuilder.addEncoded(name, value);
        } else {
            formBuilder.add(name, value);
        }
    }

        // Only called when isMultipart was true.
    void addPart(Headers headers, RequestBody body) {
        if (multipartBuilder != null) {
            multipartBuilder.addPart(headers, body);
        }
    }

        // Only called when isMultipart was true.
    void addPart(MultipartBody.Part part) {
        if (multipartBuilder != null) {
            multipartBuilder.addPart(part);
        }
    }

    void setBody(@Nullable RequestBody body) {
        this.body = body;
    }

    <T> void addTag(Class<T> cls, @Nullable T value) {
        requestBuilder.tag(cls, value);
    }

    Request.Builder get() {
        HttpUrl url;
        HttpUrl.Builder urlBuilder = this.urlBuilder;
        if (urlBuilder != null) {
            url = urlBuilder.build();
        } else {
            // No query parameters triggered builder creation, just combine the relative URL and base URL.
            //noinspection ConstantConditions Non-null if urlBuilder is null.
            url = baseUrl.resolve(relativeUrl);
            if (url == null) {
                throw new IllegalArgumentException(
                        "Malformed URL. Base: " + baseUrl + ", Relative: " + relativeUrl);
            }
        }

        RequestBody body = this.body;
        if (body == null) {
            // Try to pull from one of the builders.
            if (formBuilder != null) {
                body = formBuilder.build();
            } else if (multipartBuilder != null) {
                body = multipartBuilder.build();
            } else if (hasBody) {
                // Body is absent, make an empty body.
                body = RequestBody.Companion.getEMPTY_REQUEST_BODY();
            }
        }

        MediaType contentType = this.contentType;
        if (contentType != null) {
            if (body != null) {
                body = new ContentTypeOverridingRequestBody(body, contentType);
            } else {
                headersBuilder.add("Content-Type", contentType.toString());
            }
        }

        if (disableCache) {
            requestBuilder.disableCache();
        }

        return requestBuilder.headers(headersBuilder.build())
                .method(method, body)
                .priority(priority)
                .url(url);
    }

    private static class ContentTypeOverridingRequestBody extends RequestBody {
        private final RequestBody delegate;
        private final MediaType contentType;

        ContentTypeOverridingRequestBody(RequestBody delegate, MediaType contentType) {
            this.delegate = delegate;
            this.contentType = contentType;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long length() throws IOException {
            return delegate.length();
        }

        @Override
        public void writeTo(@NotNull BufferedSink sink) throws IOException {
            delegate.writeTo(sink);
        }
    }
}
