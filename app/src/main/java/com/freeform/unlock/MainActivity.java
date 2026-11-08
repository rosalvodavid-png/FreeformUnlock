package com.freeform.unlock;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Tela do módulo + diagnóstico embutido.
 *
 * O diagnóstico roda "su -c logcat -d -s FreeformUnlock" (o KernelSU/Vector
 * mostra o pedido de root) e mostra o resultado na tela, para não depender de
 * adb. Também imprime as props da ROM.
 */
public final class MainActivity extends Activity {

    private static final String TAG = "FreeformUnlock";

    private TextView output;
    private Button diagButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView info = new TextView(this);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        info.setTextIsSelectable(true);
        info.setText(
                "Freeform Unlock 1.2\n\n"
                        + "Escopo já vem marcado automaticamente (xposedscope):\n"
                        + "• Sistema Android (android)\n"
                        + "• System UI (com.android.systemui)\n"
                        + "• Launcher do sistema (com.miui.home)\n\n"
                        + "Se o Vector não marcar sozinho, marque os três na mão.\n"
                        + "Depois de ativar ou mudar o escopo, reinicie o celular.\n\n"
                        + "Se a janela flutuante do app continuar bloqueada, toque em "
                        + "\"Rodar diagnóstico\" e me envie o texto gerado.\n\n"
                        + "Emergência: crie o arquivo "
                        + ModuleEntry.KILL_SWITCH + " com root e reinicie.");

        diagButton = new Button(this);
        diagButton.setText("Rodar diagnóstico (pede root)");
        diagButton.setOnClickListener(v -> runDiagnostics());

        Button copyButton = new Button(this);
        copyButton.setText("Copiar resultado");
        copyButton.setOnClickListener(v -> {
            CharSequence text = output.getText();
            if (text != null && text.length() > 0) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText(TAG, text));
                }
            }
        });

        output = new TextView(this);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        output.setTextIsSelectable(true);
        output.setText("(toque em Rodar diagnóstico)");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(info, matchWrap());
        root.addView(diagButton, matchWrap());
        root.addView(copyButton, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void runDiagnostics() {
        diagButton.setEnabled(false);
        output.setText("Pedindo root e lendo o log...\n");

        new Thread(() -> {
            String log = exec("logcat -d -s FreeformUnlock");
            if (log.trim().isEmpty()) {
                log = exec("logcat -d | grep -i -E 'freeformunlock|freeform' | tail -200");
            }
            String props = exec(
                    "echo \"HyperOS: $(getprop ro.mi.os.version.name)\";"
                            + " echo \"MIUI: $(getprop ro.miui.ui.version.name)\";"
                            + " echo \"Android: $(getprop ro.build.version.release) (SDK $(getprop ro.build.version.sdk))\";"
                            + " echo \"Modelo: $(getprop ro.product.model)\";"
                            + " echo \"Vector: $(pm list packages | grep -c -i -E 'jingmatrix|vector|lsposed')\"");

            String text = "== Sistema ==\n" + props.trim()
                    + "\n\n== Log do módulo ==\n"
                    + (log.trim().isEmpty()
                    ? "(vazio) O módulo não foi carregado no system_server:\n"
                    + "confira se está ativo no Vector, com escopo Sistema Android, e reinicie."
                    : log.trim());

            final String result = text;
            runOnUiThread(() -> {
                output.setText(result);
                diagButton.setEnabled(true);
            });
        }).start();
    }

    private String exec(String cmd) {
        StringBuilder sb = new StringBuilder();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                sb.append("(timeout)\n");
            }
        } catch (Throwable t) {
            sb.append("erro ao executar: ").append(t).append('\n');
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return sb.toString();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
