package ru.veris.metalcalc;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int BG = Color.rgb(21,24,28);
    private static final int CARD = Color.rgb(34,39,45);
    private static final int FIELD = Color.rgb(43,49,56);
    private static final int LINE = Color.rgb(59,66,74);
    private static final int TEXT = Color.rgb(244,246,248);
    private static final int MUTED = Color.rgb(174,182,191);
    private static final int ACCENT = Color.rgb(255,193,7);

    private EditText diameterInput, thicknessInput, priceInput;
    private TextView materialValue, weightValue, costValue, volumeValue, densityValue, errorValue;
    private Button steelBtn, aluminumBtn, bronzeBtn, brazhBtn;
    private SharedPreferences prefs;

    private String material = "steel";
    private double density = 7850.0;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("metal_calc", MODE_PRIVATE);
        material = prefs.getString("material", "steel");

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(main, new ScrollView.LayoutParams(-1, -2));

        TextView brand = text("ВЕРИС", 14, ACCENT, true);
        brand.setLetterSpacing(0.08f);
        main.addView(brand);

        TextView title = text("Стоимость круглой заготовки", 26, TEXT, true);
        LinearLayout.LayoutParams titleLp = lp(-1, -2);
        titleLp.setMargins(0, dp(4), 0, dp(14));
        main.addView(title, titleLp);

        GridLayout materialGrid = new GridLayout(this);
        materialGrid.setColumnCount(2);
        materialGrid.setRowCount(2);
        main.addView(materialGrid, lp(-1, -2));

        steelBtn = materialButton("Сталь", "steel");
        aluminumBtn = materialButton("Алюминий", "aluminum");
        bronzeBtn = materialButton("Бронза", "bronze");
        brazhBtn = materialButton("БрАЖ", "brazh");

        addGrid(materialGrid, steelBtn, 0, 0);
        addGrid(materialGrid, aluminumBtn, 0, 1);
        addGrid(materialGrid, bronzeBtn, 1, 0);
        addGrid(materialGrid, brazhBtn, 1, 1);

        LinearLayout dims = new LinearLayout(this);
        dims.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dimsLp = lp(-1, -2);
        dimsLp.setMargins(0, dp(12), 0, 0);
        main.addView(dims, dimsLp);

        diameterInput = numberField("200");
        thicknessInput = numberField("50");

        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, -2, 1f);
        half1.setMargins(0, 0, dp(6), 0);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1f);
        half2.setMargins(dp(6), 0, 0, 0);

        dims.addView(wrapField("Диаметр, мм", diameterInput), half1);
        dims.addView(wrapField("Толщина, мм", thicknessInput), half2);

        priceInput = numberField("");
        LinearLayout.LayoutParams priceLp = lp(-1, -2);
        priceLp.setMargins(0, dp(12), 0, 0);
        main.addView(wrapField("Цена материала, ₽/кг", priceInput), priceLp);

        errorValue = text("Введите положительные диаметр и толщину.", 13, Color.rgb(255,158,158), false);
        errorValue.setVisibility(View.GONE);
        LinearLayout.LayoutParams errLp = lp(-1, -2);
        errLp.setMargins(0, dp(8), 0, 0);
        main.addView(errorValue, errLp);

        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(dp(16), dp(10), dp(16), dp(10));
        result.setBackground(roundRect(CARD, LINE, 18));
        LinearLayout.LayoutParams resultLp = lp(-1, -2);
        resultLp.setMargins(0, dp(16), 0, 0);
        main.addView(result, resultLp);

        materialValue = addRow(result, "Материал", "");
        weightValue = addRow(result, "Вес", "—");
        costValue = addRow(result, "Стоимость", "—");
        costValue.setTextColor(ACCENT);
        costValue.setTextSize(31);
        volumeValue = addRow(result, "Объём", "—");
        densityValue = addRow(result, "Плотность", "—");

        Button clear = new Button(this);
        clear.setText("Очистить размеры");
        clear.setTextColor(TEXT);
        clear.setTextSize(16);
        clear.setAllCaps(false);
        clear.setTypeface(Typeface.DEFAULT_BOLD);
        clear.setBackground(roundRect(FIELD, LINE, 13));
        clear.setOnClickListener(v -> {
            diameterInput.setText("");
            thicknessInput.setText("");
            diameterInput.requestFocus();
        });
        LinearLayout.LayoutParams clearLp = lp(-1, dp(50));
        clearLp.setMargins(0, dp(14), 0, 0);
        main.addView(clear, clearLp);

        TextView note = text(
                "Цена запоминается отдельно для каждого материала. Плотности: сталь 7 850, алюминий 2 700, бронза 8 800, БрАЖ 7 600 кг/м³.",
                13, MUTED, false);
        LinearLayout.LayoutParams noteLp = lp(-1, -2);
        noteLp.setMargins(0, dp(14), 0, 0);
        main.addView(note, noteLp);

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { calculate(); }
            public void afterTextChanged(Editable s) {}
        };

        diameterInput.addTextChangedListener(watcher);
        thicknessInput.addTextChangedListener(watcher);
        priceInput.addTextChangedListener(watcher);

        selectMaterial(material, false);
        calculate();

        setContentView(scroll);
    }

    private void addGrid(GridLayout grid, Button button, int row, int col) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                GridLayout.spec(row),
                GridLayout.spec(col, 1f)
        );
        p.width = 0;
        p.height = dp(52);
        p.setMargins(
                col == 0 ? 0 : dp(5),
                row == 0 ? 0 : dp(5),
                col == 0 ? dp(5) : 0,
                row == 0 ? dp(5) : 0
        );
        grid.addView(button, p);
    }

    private Button materialButton(String label, String key) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setOnClickListener(v -> selectMaterial(key, true));
        return b;
    }

    private void selectMaterial(String key, boolean save) {
        material = key;

        switch (key) {
            case "aluminum":
                density = 2700;
                break;
            case "bronze":
                density = 8800;
                break;
            case "brazh":
                density = 7600;
                break;
            default:
                material = "steel";
                density = 7850;
                break;
        }

        if (save) {
            prefs.edit().putString("material", material).apply();
        }

        setButtonState(steelBtn, material.equals("steel"));
        setButtonState(aluminumBtn, material.equals("aluminum"));
        setButtonState(bronzeBtn, material.equals("bronze"));
        setButtonState(brazhBtn, material.equals("brazh"));

        String saved = prefs.getString("price_" + material, null);
        if (saved == null) {
            if (material.equals("steel")) saved = "100";
            else if (material.equals("aluminum")) saved = "250";
            else if (material.equals("bronze")) saved = "800";
            else saved = "700";
        }

        if (priceInput != null) {
            priceInput.setText(saved);
        }

        if (materialValue != null) materialValue.setText(materialName());
        if (densityValue != null) densityValue.setText(format(density, 0) + " кг/м³");

        calculate();
    }

    private void setButtonState(Button b, boolean active) {
        b.setTextColor(active ? Color.rgb(23,25,28) : TEXT);
        b.setBackground(roundRect(active ? ACCENT : FIELD, active ? ACCENT : LINE, 13));
    }

    private String materialName() {
        if (material.equals("aluminum")) return "Алюминий";
        if (material.equals("bronze")) return "Бронза";
        if (material.equals("brazh")) return "БрАЖ";
        return "Сталь";
    }

    private void calculate() {
        if (diameterInput == null || thicknessInput == null || priceInput == null) {
            return;
        }

        double d = parse(diameterInput.getText().toString());
        double h = parse(thicknessInput.getText().toString());
        double price = parse(priceInput.getText().toString());

        if (!Double.isNaN(price) && price >= 0) {
            prefs.edit().putString("price_" + material, priceInput.getText().toString()).apply();
        }

        if (materialValue != null) materialValue.setText(materialName());
        if (densityValue != null) densityValue.setText(format(density, 0) + " кг/м³");

        if (Double.isNaN(d) || Double.isNaN(h) || d <= 0 || h <= 0) {
            if (errorValue != null) errorValue.setVisibility(View.VISIBLE);
            if (weightValue != null) weightValue.setText("—");
            if (costValue != null) costValue.setText("—");
            if (volumeValue != null) volumeValue.setText("—");
            return;
        }

        if (errorValue != null) errorValue.setVisibility(View.GONE);

        double dm = d / 1000.0;
        double hm = h / 1000.0;
        double volume = Math.PI * dm * dm / 4.0 * hm;
        double mass = volume * density;

        if (weightValue != null) weightValue.setText(format(mass, 2) + " кг");
        if (volumeValue != null) volumeValue.setText(format(volume * 1_000_000.0, 3) + " л");
        if (costValue != null) {
            costValue.setText(
                    (!Double.isNaN(price) && price >= 0)
                            ? format(mass * price, 0) + " ₽"
                            : "—"
            );
        }
    }

    private double parse(String s) {
        try {
            String x = s.trim().replace(',', '.');
            if (x.isEmpty()) return Double.NaN;
            return Double.parseDouble(x);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private String format(double value, int maxDecimals) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        f.setMaximumFractionDigits(maxDecimals);
        f.setMinimumFractionDigits(0);
        return f.format(value);
    }

    private LinearLayout wrapField(String label, EditText input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(roundRect(FIELD, LINE, 14));

        TextView l = text(label, 13, MUTED, false);
        box.addView(l, lp(-1, -2));

        LinearLayout.LayoutParams inputLp = lp(-1, dp(42));
        inputLp.setMargins(0, dp(2), 0, 0);
        box.addView(input, inputLp);

        return box;
    }

    private EditText numberField(String initial) {
        EditText e = new EditText(this);
        e.setText(initial);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(25);
        e.setTypeface(Typeface.DEFAULT_BOLD);
        e.setSingleLine(true);
        e.setSelectAllOnFocus(true);
        e.setPadding(0, 0, 0, 0);
        e.setBackgroundColor(Color.TRANSPARENT);
        e.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        return e;
    }

    private TextView addRow(LinearLayout parent, String key, String initial) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        TextView k = text(key, 14, MUTED, false);
        TextView v = text(initial, 20, TEXT, true);
        v.setGravity(Gravity.END);

        row.addView(k, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1.4f));

        parent.addView(row, lp(-1, -2));
        return v;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
