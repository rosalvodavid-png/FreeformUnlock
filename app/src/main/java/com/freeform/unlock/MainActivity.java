package com.freeform.unlock;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tela informativa do módulo (aberta apenas via gerenciador LSPosed).
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setText("Freeform Unlock 1.0 (Estável)");
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView info = new TextView(this);
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        info.setTextIsSelectable(true);
        info.setPadding(0, dp(16), 0, 0);
        info.setText(
                "Módulo Xposed ativo e funcionando.\n\n"
                        + "Escopo configurado:\n"
                        + "• Sistema Android (android)\n"
                        + "• System UI (com.android.systemui)\n\n"
                        + "Lembre-se de reiniciar o dispositivo após ativar ou modificar o escopo.\n\n"
                        + "Interruptor de emergência: crie o arquivo "
                        + ModuleEntry.KILL_SWITCH + " com root para desativar os hooks.");

        root.addView(title, matchWrap());
        root.addView(info, matchWrap());

        setContentView(root);
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
