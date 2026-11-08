package com.freeform.unlock;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Tela só informativa: não há nada para configurar. Serve para o LSPosed listar
 * o app e para lembrar o escopo correto depois de reiniciar o celular.
 */
public final class MainActivity extends Activity {

    private static final String LSPOSED_PKG = "org.lsposed.manager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        text.setPadding(pad, pad, pad, pad);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(buildText());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        setContentView(scroll);
    }

    private CharSequence buildText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Freeform Unlock 1.0\n\n");
        sb.append("Objetivo: liberar janelas flutuantes (freeform / 小窗) em apps que o ")
                .append("HyperOS/MIUI bloqueia, como o Kwai.\n\n");

        sb.append("Como ativar\n");
        sb.append("1. Ative o módulo no LSPosed.\n");
        sb.append("2. Escopo: Sistema Android (android).\n");
        sb.append("   Opcional: System UI, para o botão de janela flutuante.\n");
        sb.append("3. Reinicie o celular (o system_server precisa reiniciar).\n");
        sb.append("4. Abra o Kwai e tente abrir em janela flutuante.\n\n");

        sb.append("Se algo der errado\n");
        sb.append("• Interruptor de emergência: crie o arquivo ")
                .append(ModuleEntry.KILL_SWITCH)
                .append(" com root (touch) e reinicie.\n");
        sb.append("• Ou desative o módulo no LSPosed e reinicie.\n\n");

        sb.append("Diagnóstico\n");
        sb.append("• Logs: logcat -s FreeformUnlock\n");
        sb.append("• Também aparecem em LSPosed > Logs.\n\n");

        sb.append("Ambiente\n");
        sb.append("• Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("• LSPosed: ").append(isPackageInstalled(LSPOSED_PKG) ? "instalado" : "não encontrado")
                .append("\n");
        sb.append("• Fabricante: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n");

        return sb.toString();
    }

    private boolean isPackageInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
