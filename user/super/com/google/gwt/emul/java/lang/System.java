/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

import static com.google.gwt.core.shared.impl.InternalPreconditions.checkArrayType;
import static com.google.gwt.core.shared.impl.InternalPreconditions.checkNotNull;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.core.client.impl.Impl;
import com.google.gwt.lang.Array;

import java.io.OutputStream;
import java.io.PrintStream;

import com.google.gwt.core.client.impl.Impl;

/**
 * General-purpose low-level utility methods. GWT only supports a limited subset
 * of these methods due to browser limitations. Only the documented methods are
 * available.
 */
public final class System {

  /**
   * Does nothing in web mode. To get output in web mode, subclass PrintStream
   * and call {@link #setErr(PrintStream)}.
   */
  public static final PrintStream err = new PrintStream((OutputStream)null);

  /**
   * Does nothing in web mode. To get output in web mode, subclass
   * {@link PrintStream} and call {@link #setOut(PrintStream)}.
   */
  public static final PrintStream out = new PrintStream((OutputStream)null);

  public static void arraycopy(Object src, int srcOfs, Object dest, int destOfs, int len) {
    checkNotNull(src, "src");
    checkNotNull(dest, "dest");

    Class<?> srcType = src.getClass();
    Class<?> destType = dest.getClass();
    checkArrayType(srcType.isArray(), "srcType is not an array");
    checkArrayType(destType.isArray(), "destType is not an array");

    Class<?> srcComp = srcType.getComponentType();
    Class<?> destComp = destType.getComponentType();
    checkArrayType(arrayTypeMatch(srcComp, destComp), "Array types don't match");

    int srclen = getArrayLength(src);
    int destlen = getArrayLength(dest);
    if (srcOfs < 0 || destOfs < 0 || len < 0 || srcOfs + len > srclen || destOfs + len > destlen) {
      throw new IndexOutOfBoundsException();
    }
    /*
     * If the arrays are not references or if they are exactly the same type, we
     * can copy them in native code for speed. Otherwise, we have to copy them
     * in Java so we get appropriate errors.
     */
    if ((!srcComp.isPrimitive() || srcComp.isArray())
        && !srcType.equals(destType)) {
      // copy in Java to make sure we get ArrayStoreExceptions if the values
      // aren't compatible
      Object[] srcArray = (Object[]) src;
      Object[] destArray = (Object[]) dest;
      if (src == dest && srcOfs < destOfs) {
        // TODO(jat): how does backward copies handle failures in the middle?
        // copy backwards to avoid destructive copies
        srcOfs += len;
        for (int destEnd = destOfs + len; destEnd-- > destOfs;) {
          destArray[destEnd] = srcArray[--srcOfs];
        }
      } else {
        for (int destEnd = destOfs + len; destOfs < destEnd;) {
          destArray[destOfs++] = srcArray[srcOfs++];
        }
      }
    } else if (len > 0) {
      Array.nativeArraycopy(src, srcOfs, dest, destOfs, len);
    }
  }

  public static long currentTimeMillis() {
    return (long) currentTimeMillis0();
  }

  public static long nanoTime() {
    return (long) nanoTime0();
  }

  /**
   * Has no effect; just here for source compatibility.
   *
   * @skip
   */
  public static void gc() {
  }

  /**
   * The compiler replaces getProperty by the actual value of the property.
   */
  public static String getProperty(String key) {
    throw new AssertionError("System.getProperty should have been replaced by the compiler.");
  }

  /**
   * The compiler replaces getProperty by the actual value of the property.
   */
  public static String getProperty(String key, String def) {
    throw new AssertionError("System.getProperty should have been replaced by the compiler.");
  }

  public static void setProperty(String key, String value) {
    throw new AssertionError("System.setProperty should have been replaced by the compiler.");
  }

  public static SecurityManager getSecurityManager() {
    return null;// never any security manager!
  }

  public static int identityHashCode(Object o) {
    return (o == null) ? 0 : (!(o instanceof String)) ? Impl.getHashCode(o)
        : String.HashCache.getHashCode(unsafeCast(o));
  }

  // TODO(goktug): replace unsafeCast with a real cast when the compiler can optimize it.
  private static native String unsafeCast(Object string) /*-{
    return string;
  }-*/;

  public static native void setErr(PrintStream err) /*-{
    @java.lang.System::err = err;
  }-*/;

  public static native void setOut(PrintStream out) /*-{
    @java.lang.System::out = out;
  }-*/;

  private static boolean arrayTypeMatch(Class<?> srcComp, Class<?> destComp) {
    if (srcComp.isPrimitive()) {
      return srcComp.equals(destComp);
    } else {
      return !destComp.isPrimitive();
    }
  }

  private static native double currentTimeMillis0() /*-{
    return (new Date()).getTime();
  }-*/;

  private static native double nanoTime0() /*-{
    return performance && performance.now() || (new Date()).getTime()*1000000;
  }-*/;

  /**
   * Returns the length of an array via Javascript.
   */
  private static native int getArrayLength(Object array) /*-{
    return array.length;
  }-*/;

}
