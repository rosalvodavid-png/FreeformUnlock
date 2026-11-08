package com.freeform.unlock;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Hooks que removem as restrições de janela flutuante (freeform / 小窗) do
 * MIUI/HyperOS.
 *
 * Todos os pontos abaixo são checagens em tempo de execução dentro do
 * system_server. Não existe arquivo, prop ou configuração equivalente, por isso
 * isso precisa ser um módulo Xposed e não um módulo KernelSU.
 *
 * Estratégia: em vez de depender de um nome exato (que muda entre versões da
 * ROM), procuramos métodos por padrão de nome e embrulhamos cada hook em
 * try/catch. Se um ponto não existir na sua ROM, ele é simplesmente ignorado.
 */
final class FreeformHooks {

    private static final String TAG = "FreeformUnlock";
    private static final Method[] NO_METHODS = new Method[0];

    private interface NameFilter {
        boolean accept(String name);
    }

    private interface AfterAction {
        void run(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    private FreeformHooks() {
    }

    static void install(ClassLoader cl) {
        log("instalando hooks | Android " + android.os.Build.VERSION.RELEASE
                + " (SDK " + android.os.Build.VERSION.SDK_INT + ") | ROM: " + romTag());

        // ------------------------------------------------------------------
        // 1) AOSP: suporte a multi-janela / freeform
        // ------------------------------------------------------------------
        hookBooleanTrue(cl, "android.app.ActivityTaskManager", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "supportsSplitScreen".equals(name)
                        || "supportsMultiWindow".equals(name)
                        || "supportsFreeform".equals(name);
            }
        });

        // ------------------------------------------------------------------
        // 2) AOSP: "forçar atividades redimensionáveis" (equivalente à opção
        //    de desenvolvedor, mas ligada sempre, inclusive para apps que
        //    declaram resizeableActivity=false, caso do Kwai)
        // ------------------------------------------------------------------
        hookAfter(cl, "com.android.server.wm.ActivityTaskManagerService", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "retrieveSettings".equals(name);
            }
        }, new AfterAction() {
            @Override
            public void run(XC_MethodHook.MethodHookParam param) {
                forceBooleanFieldTrue(param.thisObject, "mDevEnableNonResizableMultiWindow");
                forceBooleanFieldTrue(param.thisObject, "mForceResizableActivities");
                forceBooleanFieldTrue(param.thisObject, "mSupportsFreeformWindowManagement");
                forceBooleanFieldTrue(param.thisObject, "mSupportsMultiWindow");
            }
        });

        // Mantém o flag ligado quando o sistema relê as configurações.
        hookAfter(cl, "com.android.server.wm.WindowManagerService$SettingsObserver", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "updateDevEnableNonResizableMultiWindow".equals(name)
                        || "onChange".equals(name);
            }
        }, new AfterAction() {
            @Override
            public void run(XC_MethodHook.MethodHookParam param) {
                Object wms;
                try {
                    wms = XposedHelpers.getObjectField(param.thisObject, "this$0");
                } catch (Throwable t) {
                    wms = null;
                }
                Object atm = null;
                if (wms != null) {
                    try {
                        atm = XposedHelpers.getObjectField(wms, "mAtmService");
                    } catch (Throwable ignored) {
                    }
                }
                forceBooleanFieldTrue(atm, "mDevEnableNonResizableMultiWindow");
                forceBooleanFieldTrue(atm, "mForceResizableActivities");
                forceBooleanFieldTrue(wms, "mSupportsFreeformWindowManagement");
                forceBooleanFieldTrue(wms, "mDevEnableNonResizableMultiWindow");
            }
        });

        // ------------------------------------------------------------------
        // 3) HyperOS/MIUI: utilidades de multi-janela
        //    (MiuiMultiWindowUtils.isForceResizeable, supportFreeform, ...)
        // ------------------------------------------------------------------
        hookBooleanTrue(cl, "android.util.MiuiMultiWindowUtils", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return isFreeformGate(name);
            }
        });

        // Listas de apps bloqueados (blacklist) do MIUI/HyperOS.
        hookEmptyCollection(cl, "android.util.MiuiMultiWindowAdapter");
        hookEmptyCollection(cl, "android.util.MiuiMultiWindowUtils");

        // ------------------------------------------------------------------
        // 4) HyperOS/MIUI: task "não redimensionável" -> redimensionável
        // ------------------------------------------------------------------
        hookBooleanTrue(cl, "com.android.server.wm.Task", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "isResizeable".equals(name) || "isResizable".equals(name);
            }
        });
        hookBooleanTrue(cl, "com.android.server.wm.TaskRecord", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "isResizeable".equals(name) || "isResizable".equals(name);
            }
        });

        // ------------------------------------------------------------------
        // 5) HyperOS: limite de quantidade e bloqueio de novas janelas
        // ------------------------------------------------------------------
        hookConstant(cl, "com.android.server.wm.MiuiFreeFormStackDisplayStrategy", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "getMaxMiuiFreeFormStackCount".equals(name)
                        || "getMaxMiuiFreeFormStackCountForFlashBack".equals(name);
            }
        }, 256);

        hookBooleanFalse(cl, "com.android.server.wm.MiuiFreeFormStackDisplayStrategy", new NameFilter() {
            @Override
            public boolean accept(String name) {
                return "shouldStopStartFreeform".equals(name);
            }
        });

        log("hooks instalados");
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /** Nomes de métodos do MIUI/HyperOS que funcionam como "porta" do freeform. */
    private static boolean isFreeformGate(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        boolean related = n.contains("freeform") || n.contains("forceresizeable") || n.contains("forceresizable");
        if (!related) {
            return false;
        }
        return n.startsWith("is") || n.startsWith("support") || n.startsWith("can")
                || n.startsWith("multi") || n.startsWith("has");
    }

    private static void hookBooleanTrue(ClassLoader cl, String className, NameFilter filter) {
        hookConstant(cl, className, filter, Boolean.TRUE);
    }

    private static void hookBooleanFalse(ClassLoader cl, String className, NameFilter filter) {
        hookConstant(cl, className, filter, Boolean.FALSE);
    }

    private static void hookConstant(ClassLoader cl, String className, NameFilter filter, Object value) {
        for (Method method : declaredMethods(cl, className)) {
            try {
                if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                    continue;
                }
                if (!filter.accept(method.getName())) {
                    continue;
                }
                Class<?> ret = method.getReturnType();
                if (value instanceof Boolean && ret != boolean.class && ret != Boolean.class) {
                    continue;
                }
                if (value instanceof Integer && ret != int.class && ret != Integer.class) {
                    continue;
                }
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(value));
                log("hook " + className + "#" + method.getName() + " -> " + value);
            } catch (Throwable t) {
                log("falha ao hookar " + className + "#" + method.getName() + ": " + t);
            }
        }
    }

    private static void hookEmptyCollection(ClassLoader cl, String className) {
        for (Method method : declaredMethods(cl, className)) {
            try {
                if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                    continue;
                }
                String n = method.getName().toLowerCase(Locale.ROOT);
                if (!n.contains("blacklist") && !n.contains("blocklist")) {
                    continue;
                }
                Object empty = emptyFor(method.getReturnType());
                if (empty == null) {
                    continue;
                }
                XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(empty));
                log("hook " + className + "#" + method.getName() + " -> " + empty.getClass().getSimpleName() + " vazio");
            } catch (Throwable t) {
                log("falha ao hookar " + className + "#" + method.getName() + ": " + t);
            }
        }
    }

    /** Coleção/mapa vazio compatível com o tipo de retorno declarado. */
    private static Object emptyFor(Class<?> ret) {
        if (ret == null) {
            return null;
        }
        if (Map.class.isAssignableFrom(ret)) {
            return new HashMap<>();
        }
        if (Set.class.isAssignableFrom(ret)) {
            return new HashSet<>();
        }
        if (Collection.class.isAssignableFrom(ret)) {
            return new ArrayList<>();
        }
        return null;
    }

    private static void hookAfter(ClassLoader cl, String className, NameFilter filter, final AfterAction action) {
        for (final Method method : declaredMethods(cl, className)) {
            try {
                if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                    continue;
                }
                if (!filter.accept(method.getName())) {
                    continue;
                }
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            action.run(param);
                        } catch (Throwable t) {
                            log("erro no after de " + className + "#" + method.getName() + ": " + t);
                        }
                    }
                });
                log("hook after " + className + "#" + method.getName());
            } catch (Throwable t) {
                log("falha ao hookar " + className + "#" + method.getName() + ": " + t);
            }
        }
    }

    private static Method[] declaredMethods(ClassLoader cl, String className) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, cl);
            if (clazz == null) {
                log("classe ausente (ok, ROM diferente): " + className);
                return NO_METHODS;
            }
            return clazz.getDeclaredMethods();
        } catch (Throwable t) {
            log("erro ao ler " + className + ": " + t);
            return NO_METHODS;
        }
    }

    private static void forceBooleanFieldTrue(Object target, String fieldName) {
        if (target == null) {
            return;
        }
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            if (field.getType() != boolean.class && field.getType() != Boolean.class) {
                return;
            }
            boolean old = field.getBoolean(target);
            if (!old) {
                field.setBoolean(target, true);
                log("campo " + target.getClass().getSimpleName() + "." + fieldName + ": false -> true");
            }
        } catch (Throwable t) {
            log("falha no campo " + fieldName + ": " + t);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Class<?> k = clazz; k != null && k != Object.class; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static String romTag() {
        String inc = safe("ro.mi.os.version.incremental");
        String miui = safe("ro.miui.ui.version.name");
        String hyper = safe("ro.mi.os.version.name");
        return "hyper=" + hyper + " miui=" + miui + " inc=" + inc;
    }

    private static String safe(String prop) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method get = sp.getMethod("get", String.class);
            Object v = get.invoke(null, prop);
            return v == null ? "?" : v.toString();
        } catch (Throwable t) {
            return "?";
        }
    }

    static void log(String message) {
        String line = "FreeformUnlock: " + message;
        try {
            XposedBridge.log(line);
        } catch (Throwable ignored) {
        }
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {
        }
    }
}
