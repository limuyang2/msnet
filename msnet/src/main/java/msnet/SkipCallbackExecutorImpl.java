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

import java.lang.annotation.Annotation;


// This class conforms to the annotation requirements documented on Annotation.
final class SkipCallbackExecutorImpl implements SkipCallbackExecutor {
  private static final SkipCallbackExecutor INSTANCE = new SkipCallbackExecutorImpl();

  static Annotation[] ensurePresent(Annotation[] annotations) {
    if (Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)) {
      return annotations;
    }

    Annotation[] newAnnotations = new Annotation[annotations.length + 1];
    // Place the skip annotation first since we're guaranteed to check for it in the call adapter.
    newAnnotations[0] = SkipCallbackExecutorImpl.INSTANCE;
    System.arraycopy(annotations, 0, newAnnotations, 1, annotations.length);
    return newAnnotations;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return SkipCallbackExecutor.class;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SkipCallbackExecutor;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @NotNull
  @Override
  public String toString() {
    return "@" + SkipCallbackExecutor.class.getName() + "()";
  }
}
