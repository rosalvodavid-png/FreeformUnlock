package com.freeform.unlock;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Ponto de entrada do módulo.
 *
 * O LSPosed injeta este código em cada processo do escopo escolhido. Só nos
 * interessam o processo "android" (system_server / framework), onde ficam as
 * checagens de janela flutuante do HyperOS/MIUI.
 */
public final class ModuleEntry implements IXposedHookLoadPackage {

    /**
     * Interruptor de emergência: se este arquivo existir, nenhum hook é
     * instalado. Crie/remova com root:
     *   adb shell su -c 'touch /data/local/tmp/freeformunlock.disabled'
     */
    static final String KILL_SWITCH = "/data/local/tmp/freeformunlock.disabled";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName)) {
            return;
        }

        if (new File(KILL_SWITCH).exists()) {
            FreeformHooks.log("kill switch encontrado (" + KILL_SWITCH + "), hooks não instalados");
            return;
        }

        FreeformHooks.install(lpparam.classLoader);
    }
}
