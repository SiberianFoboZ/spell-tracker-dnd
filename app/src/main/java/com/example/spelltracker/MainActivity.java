package com.example.spelltracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spelltracker.Classes.Info;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

/**
 * Главный экран: ввод уровней D&D-классов (мульти-класс) + автоматический
 * расчёт эффективного уровня заклинателя и ячеек заклинаний 1..9 уровней +
 * переход к списку заклинаний.
 *
 * <p>Класс Колдун (warlock) — отображается в полях ввода, но не участвует
 * в формуле эффективного уровня (использует pact magic).</p>
 */
public class MainActivity extends AppCompatActivity {

    private SlotAdapter adapter;
    private SpellStorage storage;
    private TextView tvEffectiveLevel;
    private TextView tvEffectiveLevelValue;
    private final Map<String, TextInputEditText> classInputs = new HashMap<>();

    private View pactMagicSection;
    private TextView tvPactMagicCount;
    private TextView tvPactMagicSubtitle;
    private MaterialButton btnPactUse;
    private MaterialButton btnPactRestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Edge-to-edge: рисуем контент под системными барами и сами
        // учитываем insets (нижние кнопки не уезжают за nav bar).
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        storage = new SpellStorage(this);

        // Тулбар убран в редизайне (заголовок теперь — TextView в activity_main.xml).

        // Нижний отступ под системную навигацию добавляем к панели кнопок.
        View bottomRow = findViewById(R.id.bottom_buttons_row);
        ViewCompat.setOnApplyWindowInsetsListener(bottomRow, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom);
            return windowInsets;
        });

        tvEffectiveLevel = findViewById(R.id.tv_effective_level);
        tvEffectiveLevelValue = findViewById(R.id.tv_effective_level_value);

        // Карточка pact magic
        pactMagicSection = findViewById(R.id.pact_magic_section);
        tvPactMagicCount = findViewById(R.id.tv_pact_magic_count);
        tvPactMagicSubtitle = findViewById(R.id.tv_pact_magic_subtitle);
        btnPactUse = findViewById(R.id.btn_pact_use);
        btnPactRestore = findViewById(R.id.btn_pact_restore);
        btnPactUse.setOnClickListener(v -> { storage.useWarlockSlot(); updatePactMagic(); refresh(); });
        btnPactRestore.setOnClickListener(v -> { storage.restoreWarlockSlot(); updatePactMagic(); refresh(); });

        // Поля ввода уровней классов ищутся динамически по id из Classes.
        for (Info info : Classes.ALL) {
            int resId = getResources().getIdentifier("et_" + info.id, "id", getPackageName());
            if (resId == 0) continue;
            TextInputEditText et = findViewById(resId);
            classInputs.put(info.id, et);
            // Поле по умолчанию показывает плейсхолдер «0» («нет уровня»).
            // При фокусе плейсхолдер стирается, при потере фокуса
            // восстанавливается, если пользователь ничего не ввёл.
            // Цифровая клавиатура включается через android:inputType="number"
            // в activity_main.xml.
            et.setText("0");
            et.setOnFocusChangeListener((v, hasFocus) -> {
                TextInputEditText editText = (TextInputEditText) v;
                String cur = editText.getText() == null ? "" : editText.getText().toString();
                if (hasFocus) {
                    if ("0".equals(cur)) {
                        editText.setText("");
                    }
                } else {
                    if (cur.trim().isEmpty()) {
                        editText.setText("0");
                    }
                }
            });
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    String text = s == null ? "" : s.toString().trim();
                    // Пустое поле — не трогаем storage (это промежуточное
                    // состояние при фокусе, до того как пользователь начал
                    // вводить число; на blur вернётся «0», который уже
                    // обработается повторным вызовом).
                    // «0» — реальное значение (плейсхолдер при открытии экрана
                    // или ввод пользователя), его нужно сохранять, иначе
                    // расчёт не сбросится к нулю.
                    if (text.isEmpty()) {
                        return;
                    }
                    int level;
                    try {
                        level = Integer.parseInt(text);
                    } catch (NumberFormatException ex) {
                        return; // ждём ввода числа
                    }
                    storage.setClassLevel(info.id, level);
                    storage.applySlotTable();
                    storage.applyWarlockSlots();
                    updateEffectiveLevel();
                    updatePactMagic();
                    refresh();
                }
            });
        }

        RecyclerView rv = findViewById(R.id.slots_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SlotAdapter(storage, new SlotAdapter.Listener() {
            @Override public void onUse(int level)     { storage.useSlot(level); refresh(); }
            @Override public void onRestore(int level) { storage.restoreSlot(level); refresh(); }
        });
        rv.setAdapter(adapter);

        MaterialButton btnSpells = findViewById(R.id.btn_open_spells);
        btnSpells.setOnClickListener(v ->
                startActivity(new Intent(this, SpellsActivity.class)));

        MaterialButton btnReset = findViewById(R.id.btn_reset_used);
        btnReset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Сбросить использование?")
                .setMessage("Все использованные ячейки заклинаний будут восстановлены.")
                .setPositiveButton("Сбросить", (d, w) -> {
                    storage.resetAllUsed();
                    updatePactMagic();
                    refresh();
                })
                .setNegativeButton("Отмена", null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Поля при открытии экрана показывают плейсхолдер «0».
        // setText("0") дёргает TextWatcher, который сохраняет «0» в storage
        // (см. afterTextChanged), тем самым сбрасывая эффективный уровень
        // заклинателя к 0. Если пользователь хочет оставить прежний уровень,
        // он вводит число заново — после первого ввода TextWatcher запишет
        // реальное значение в storage и дальше оно переживёт перезапуск.
        for (Info info : Classes.ALL) {
            TextInputEditText et = classInputs.get(info.id);
            if (et != null) {
                et.setText("0");
            }
        }
        // Пересчитываем ячейки на случай, если состояние изменилось вне этого экрана.
        storage.applySlotTable();
        storage.applyWarlockSlots();
        updateEffectiveLevel();
        updatePactMagic();
        refresh();
    }

    private void updateEffectiveLevel() {
        int eff = storage.computeCasterLevel();
        tvEffectiveLevel.setText(getString(R.string.effective_caster_level, eff));
        tvEffectiveLevelValue.setText(String.valueOf(eff));
    }

    /** Показ/скрытие и обновление карточки pact magic. */
    private void updatePactMagic() {
        if (pactMagicSection == null) return; // вьюхи не инициализированы
        int warlockLevel = storage.getClassLevel("warlock");
        if (warlockLevel <= 0) {
            pactMagicSection.setVisibility(View.GONE);
            return;
        }
        pactMagicSection.setVisibility(View.VISIBLE);
        int count = storage.getWarlockSlotCount();
        int level = storage.getWarlockSlotLevel();
        int used = storage.getWarlockSlotUsed();
        tvPactMagicCount.setText(used + " / " + count);
        tvPactMagicSubtitle.setText(getString(R.string.pact_magic_subtitle_format, count, level));
        btnPactUse.setEnabled(used < count);
        btnPactRestore.setEnabled(used > 0);
    }

    private void refresh() {
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
