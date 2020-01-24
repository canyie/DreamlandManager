package com.canyie.dreamland.manager.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.canyie.dreamland.manager.AppGlobals;
import com.canyie.dreamland.manager.utils.reflect.Reflection;
import com.canyie.dreamland.manager.utils.reflect.ReflectiveException;

/**
 * @author canyie
 */
public final class ToastCompat {
    private static final String TAG = "ToastCompat";
    private static Reflection.FieldWrapper sTNField;
    private static Reflection.FieldWrapper sTNHandlerField;
    //private static Reflection.FieldWrapper sTNShowField;
    private static boolean hookable;

    static {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
            DLog.d(TAG, "Android 7.1, preparing hook...");
            try {
                sTNField = Reflection.field(Toast.class, "mTN");
                Class<?> tnType = Reflection.findClassOrNull("android.widget.Toast$TN");
                if (tnType == null) {
                    DLog.w(TAG, "Class android.widget.Toast$TN not found, try sTNField.getType()");
                    tnType = sTNField.getType();
                    DLog.d(TAG, "sTNField.getType() == %s", tnType.toString());
                }

                /*Field show = Reflection.findField(tnType, "mShow");
                if (show != null) {
                    sTNShowField = Reflection.wrap(show);
                    hookable = true;
                } else {
                    DLog.i(TAG, "Field Toast$TN.mShow is unavailable, ignore");
                    // Toast$TN.mShow has removed on Android 7.1.1
                }*/

                try {
                    sTNHandlerField = Reflection.field(tnType, "mHandler");
                    hookable = true;
                } catch (ReflectiveException e) {
                    DLog.w(TAG, "Field Toast$TN.mHandler is unavailable", e);
                }
                if (hookable) {
                    DLog.d(TAG, "prepare hook done.");
                } else {
                    DLog.e(TAG, "prepare hook failed.");
                }
            } catch (ReflectiveException e) {
                DLog.e(TAG, "prepare hook Toast failed", e);
            }
        }
    }

    private ToastCompat() {
    }

    public static Toast showToast(CharSequence content) {
        return showToast(AppGlobals.getApp(), content);
    }

    public static Toast showToast(Context context, CharSequence content) {
        Toast toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);
        hook(toast);
        toast.show();
        return toast;
    }

    public static Toast showToast(@StringRes int resId) {
        return showToast(AppGlobals.getApp(), resId);
    }

    public static Toast showToast(Context context, @StringRes int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        hook(toast);
        toast.show();
        return toast;
    }

    public static Toast hook(Toast toast) {
        if (!hookable) {
            return toast;
        }
        try {
            Object tn = sTNField.getValue(toast);
            if (tn != null) {
                if (hookTNHandler(tn)/* | hookTNShowAction(tn)*/) {
                    DLog.i(TAG, "hook success");
                } else {
                    DLog.e(TAG, "hook failed");
                }
            } else {
                DLog.e(TAG, "Toast.mTN == null");
            }
        } catch (Exception e) {
            DLog.e(TAG, "hook Toast failed", e);
        }
        return toast;
    }

    /*private static boolean hookTNShowAction(Object tn) {
        if (sTNShowField != null) {
            try {
                Runnable showAction = sTNShowField.getValue(tn);
                if (showAction instanceof SafelyRunnableWrapper) {
                    return true;
                }
                Runnable wrapper = new SafelyRunnableWrapper(showAction);
                sTNShowField.setValue(tn, wrapper);
                DLog.i(TAG, "Replaced Toast$TN.mShow.");
                return true;
            } catch (Exception e) {
                DLog.e(TAG, "Failed to replace Toast$TN.mShow", e);
            }
        }
        return false;
    }*/

    private static boolean hookTNHandler(Object tn) {
        if (sTNHandlerField != null) {
            try {
                Handler handler = sTNHandlerField.getValue(tn);
                if (handler != null) {
                    if (handler instanceof SafelyHandlerWrapper) {
                        return true;
                    }
                    Handler wrapper = new SafelyHandlerWrapper(handler);
                    sTNHandlerField.setValue(tn, wrapper);
                    return true;
                } else {
                    DLog.e(TAG, "Toast$TN.mHandler == null");
                }
            } catch (Exception e) {
                DLog.e(TAG, "Failed to replace Toast$TN.mHandler", e);
            }
        }
        return false;
    }

    private static final class SafelyHandlerWrapper extends Handler {
        private Handler impl;

        SafelyHandlerWrapper(Handler impl) {
            this.impl = impl;
        }

        @Override public void dispatchMessage(@NonNull Message msg) {
            try {
                impl.dispatchMessage(msg);
            } catch (WindowManager.BadTokenException e) {
                DLog.e(TAG, "BadTokenException thrown by Toast(catch by SafelyHandlerWrapper)", e);
            }
        }
    }

    /*private static final class SafelyRunnableWrapper implements Runnable {
        private Runnable impl;

        SafelyRunnableWrapper(Runnable impl) {
            this.impl = impl;
        }

        @Override public void run() {
            try {
                impl.run();
            } catch (WindowManager.BadTokenException e) {
                DLog.e(TAG, "BadTokenException thrown by Toast(catch by SafelyRunnableWrapper)", e);
            }
        }
    }*/
}