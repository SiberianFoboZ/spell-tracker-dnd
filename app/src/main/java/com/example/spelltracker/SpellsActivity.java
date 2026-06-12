package com.example.spelltracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран со списком заклинаний.
 *
 * <p>Сверху списка — два ряда чипов (горизонтальный скролл):
 * <ol>
 *   <li>Мульти-выбор D&D-классов ({@link ClassFilter}). Несколько
 *       выбранных классов = объединение списков заклинаний.</li>
 *   <li>Одиночный выбор режима/уровня: «Все», «Заг.», 1..9, «Подг.»
 *       (только подготовленные). Заменяет прежний DrawerLayout с
 *       NavigationView, который был «обрезан» — фильтр не был виден
 *       до свайпа от левого края.</li>
 * </ol>
 * Под ними — строка поиска и список заклинаний.
 */
public class SpellsActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL = "extra_initial";

    private static final int MODE_PREPARED = 0;
    private static final int MODE_ALL = 1;
    private static final int MODE_LEVEL = 2;

    // Теги для level-чипов (чтобы по тегу находить нужный при инициализации
    // из интента и при смене состояния из слушателя).
    private static final String TAG_ALL = "all";
    private static final String TAG_PREPARED = "prepared";
    private static final String TAG_LEVEL_PREFIX = "level_";

    private SpellRepository repo;
    private ClassFilter classFilter;
    private MaterialToolbar toolbar;
    private ChipGroup classChips;
    private ChipGroup levelChips;
    private TextInputEditText etSearch;
    private ProgressBar progress;
    private TextView emptyView;
    private View content;
    private final ArrayList<String> selectedClassIds = new ArrayList<>();

    private int currentMode = MODE_ALL;
    private int currentLevel = 0;
    private String searchQuery = "";

    /**
     * Подавляет обработчик клика по level-чипу на время первоначальной
     * расстановки галочек из {@link #applyInitialFilter}. Без этого
     * первый {@code setChecked(true)} дёрнул бы слушатель и дважды
     * вызвал бы {@link #showCurrentList()}.
     */
    private boolean suppressLevelCallback = true;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spells);

        // Edge-to-edge: контент рисуется под системными барами.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        repo = new SpellRepository(this);
        classFilter = ClassFilter.get(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Верхний отступ под статус-бар добавляем к тулбару.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        // Нижний отступ под системную навигацию.
        View fragmentContainer = findViewById(R.id.fragment_container);
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom);
            return windowInsets;
        });

        // Стрелка «наверх» в тулбаре возвращает на главный экран.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        classChips = findViewById(R.id.class_chips);
        setupClassChips();

        levelChips = findViewById(R.id.level_chips);
        setupLevelChips();

        etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s == null ? "" : s.toString();
                if (q.equals(searchQuery)) return;
                searchQuery = q;
                applySearchToCurrent();
            }
        });

        progress = findViewById(R.id.progress);
        emptyView = findViewById(R.id.empty);
        content = findViewById(R.id.content);

        // Применяем стартовое состояние фильтра (если задан в интенте)
        // и только потом разрешаем обработчик level-чипов реагировать
        // на пользовательские клики.
        String initial = getIntent().getStringExtra(EXTRA_INITIAL);
        applyInitialFilter(initial);
        suppressLevelCallback = false;

        updateTitle();
        initialize();
    }

    /** Применяет текущий searchQuery к видимому фрагменту (если он — SpellListFragment). */
    private void applySearchToCurrent() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof SpellListFragment) {
            ((SpellListFragment) f).setSearchQuery(searchQuery);
        }
    }

    /** Создаёт мульти-выборный ряд чипов по D&D-классам. */
    private void setupClassChips() {
        List<String> ids = classFilter.getClassIds();
        if (ids.isEmpty()) {
            // JSON не загрузился — добавим заглушку, чтобы пользователь видел причину
            Chip stub = new Chip(this);
            stub.setText("Классы не загружены");
            stub.setEnabled(false);
            classChips.addView(stub);
            return;
        }
        for (String classId : ids) {
            Chip chip = new Chip(this);
            chip.setText(classFilter.getClassName(classId));
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(classId);
            classChips.addView(chip);
        }
        classChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedClassIds.clear();
            for (int id : checkedIds) {
                Chip chip = group.findViewById(id);
                if (chip != null && chip.getTag() instanceof String) {
                    selectedClassIds.add((String) chip.getTag());
                }
            }
            showCurrentList();
        });
    }

    /**
     * Создаёт ряд чипов для фильтра по уровню/режиму:
     * «Все», «Заг.», 1..9, «Подг.». Одиночный выбор, всегда выбран
     * хотя бы один (selectionRequired=true в разметке).
     */
    private void setupLevelChips() {
        addLevelChip(TAG_ALL, getString(R.string.filter_all));
        addLevelChip(TAG_LEVEL_PREFIX + "0", getString(R.string.filter_cantrips));
        for (int i = 1; i <= 9; i++) {
            addLevelChip(TAG_LEVEL_PREFIX + i, String.valueOf(i));
        }
        addLevelChip(TAG_PREPARED, getString(R.string.filter_prepared));

        levelChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressLevelCallback) return;
            if (checkedIds.isEmpty()) return; // selectionRequired=true, но на всякий случай
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) return;
            Object tag = chip.getTag();
            if (!(tag instanceof String)) return;
            applyFilterFromTag((String) tag);
            updateTitle();
            showCurrentList();
        });
    }

    private void addLevelChip(String tag, String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setId(View.generateViewId());
        chip.setTag(tag);
        // Чуть мельче дефолтного, чтобы 12 чипов влезли на узких экранах.
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        levelChips.addView(chip);
    }

    /** Применяет стартовое состояние фильтра из интента (если задан). */
    private void applyInitialFilter(String initial) {
        String targetTag = TAG_ALL;
        if ("prepared".equals(initial)) {
            currentMode = MODE_PREPARED;
            targetTag = TAG_PREPARED;
        } else if ("all".equals(initial)) {
            currentMode = MODE_ALL;
            targetTag = TAG_ALL;
        } else if ("cantrips".equals(initial)) {
            currentMode = MODE_LEVEL;
            currentLevel = 0;
            targetTag = TAG_LEVEL_PREFIX + "0";
        } else if (initial != null && initial.startsWith("level_")) {
            try {
                int n = Integer.parseInt(initial.substring("level_".length()));
                if (n >= 0 && n <= 9) {
                    currentMode = MODE_LEVEL;
                    currentLevel = n;
                    targetTag = TAG_LEVEL_PREFIX + n;
                } else {
                    currentMode = MODE_ALL;
                    targetTag = TAG_ALL;
                }
            } catch (NumberFormatException ex) {
                currentMode = MODE_ALL;
                targetTag = TAG_ALL;
            }
        } else {
            currentMode = MODE_ALL;
            targetTag = TAG_ALL;
        }
        View v = levelChips.findViewWithTag(targetTag);
        if (v instanceof Chip) {
            ((Chip) v).setChecked(true);
        }
    }

    /** Обновляет currentMode/currentLevel по выбранному чипу. */
    private void applyFilterFromTag(String tag) {
        if (TAG_ALL.equals(tag)) {
            currentMode = MODE_ALL;
        } else if (TAG_PREPARED.equals(tag)) {
            currentMode = MODE_PREPARED;
        } else if (tag != null && tag.startsWith(TAG_LEVEL_PREFIX)) {
            try {
                currentMode = MODE_LEVEL;
                currentLevel = Integer.parseInt(tag.substring(TAG_LEVEL_PREFIX.length()));
            } catch (NumberFormatException ex) {
                currentMode = MODE_ALL;
            }
        }
    }

    private void updateTitle() {
        String title;
        if (currentMode == MODE_PREPARED) title = "Подготовлено";
        else if (currentMode == MODE_ALL) title = "Все заклинания";
        else if (currentLevel == 0) title = "Заговоры";
        else title = currentLevel + " ур.";
        toolbar.setTitle(title);
    }

    private void initialize() {
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        repo.loadOrImport(count -> {
            if (count == null || count == 0) {
                progress.setVisibility(View.GONE);
                emptyView.setText("Не удалось загрузить заклинания");
                emptyView.setVisibility(View.VISIBLE);
                return;
            }
            progress.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            showCurrentList();
        });
    }

    private void showCurrentList() {
        ArrayList<String> argClasses = selectedClassIds.isEmpty()
                ? null
                : new ArrayList<>(selectedClassIds);

        Fragment fragment;
        if (currentMode == MODE_PREPARED) {
            fragment = SpellListFragment.newInstance(
                    SpellListFragment.LEVEL_PREPARED, argClasses, searchQuery);
        } else if (currentMode == MODE_ALL) {
            fragment = SpellListFragment.newInstance(
                    SpellListFragment.LEVEL_ALL, argClasses, searchQuery);
        } else {
            fragment = SpellListFragment.newInstance(currentLevel, argClasses, searchQuery);
        }

        FragmentManager fm = getSupportFragmentManager();
        // Игнорируем IllegalStateException при быстром переключении
        try {
            fm.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        } catch (IllegalStateException ignored) {
            // Фрагмент-менеджер уже сохранил состояние — пропускаем.
        }
    }
}
