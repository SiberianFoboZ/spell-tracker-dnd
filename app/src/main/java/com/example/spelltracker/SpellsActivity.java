package com.example.spelltracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран со списком заклинаний.
 *
 * <p>Боковая панель ({@link NavigationView}) содержит:
 * «Подготовлено», «Все заклинания», группа «По уровню» с Заговорами и 1-9 ур.</p>
 *
 * <p>Сверху списка — горизонтальный {@link ChipGroup} с мульти-выбором
 * D&D-классов ({@link ClassFilter}). Несколько выбранных классов = объединение
 * списков заклинаний.</p>
 */
public class SpellsActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL = "extra_initial";

    private static final int MODE_PREPARED = 0;
    private static final int MODE_ALL = 1;
    private static final int MODE_LEVEL = 2;

    private SpellRepository repo;
    private ClassFilter classFilter;
    private DrawerLayout drawer;
    private MaterialToolbar toolbar;
    private ChipGroup classChips;
    private TextInputEditText etSearch;
    private ProgressBar progress;
    private TextView emptyView;
    private View content;
    private final ArrayList<String> selectedClassIds = new ArrayList<>();

    private int currentMode = MODE_ALL;
    private int currentLevel = 0;
    private String searchQuery = "";

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spells);

        // Edge-to-edge: контейнер фрагмента не уезжает за нижний nav bar.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        repo = new SpellRepository(this);
        classFilter = ClassFilter.get(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Нижний отступ под системную навигацию.
        View fragmentContainer = findViewById(R.id.fragment_container);
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom);
            return windowInsets;
        });

        drawer = findViewById(R.id.drawer);
        NavigationView nav = findViewById(R.id.nav);
        nav.setNavigationItemSelectedListener(this::onNavItemSelected);

        // Стрелка «наверх» в тулбаре возвращает на главный экран.
        // Боковая панель при этом остаётся доступной свайпом от левого края.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        classChips = findViewById(R.id.class_chips);
        setupClassChips();

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

        // Стартовый пункт меню из интента (если задан)
        String initial = getIntent().getStringExtra(EXTRA_INITIAL);
        applyInitialMenu(initial, nav);

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

    private void applyInitialMenu(String initial, NavigationView nav) {
        if ("prepared".equals(initial)) {
            currentMode = MODE_PREPARED;
            nav.setCheckedItem(R.id.nav_prepared);
        } else if ("all".equals(initial)) {
            currentMode = MODE_ALL;
            nav.setCheckedItem(R.id.nav_all);
        } else if ("cantrips".equals(initial)) {
            currentMode = MODE_LEVEL; currentLevel = 0;
            nav.setCheckedItem(R.id.nav_cantrips);
        } else if (initial != null && initial.startsWith("level_")) {
            try {
                currentMode = MODE_LEVEL;
                currentLevel = Integer.parseInt(initial.substring(6));
                nav.setCheckedItem(getLevelMenuId(currentLevel));
            } catch (NumberFormatException ignored) {}
        } else {
            nav.setCheckedItem(R.id.nav_all);
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_prepared) {
            currentMode = MODE_PREPARED;
        } else if (id == R.id.nav_all) {
            currentMode = MODE_ALL;
        } else if (id == R.id.nav_cantrips) {
            currentMode = MODE_LEVEL; currentLevel = 0;
        } else if (id == R.id.nav_lvl1) { currentMode = MODE_LEVEL; currentLevel = 1; }
        else if (id == R.id.nav_lvl2) { currentMode = MODE_LEVEL; currentLevel = 2; }
        else if (id == R.id.nav_lvl3) { currentMode = MODE_LEVEL; currentLevel = 3; }
        else if (id == R.id.nav_lvl4) { currentMode = MODE_LEVEL; currentLevel = 4; }
        else if (id == R.id.nav_lvl5) { currentMode = MODE_LEVEL; currentLevel = 5; }
        else if (id == R.id.nav_lvl6) { currentMode = MODE_LEVEL; currentLevel = 6; }
        else if (id == R.id.nav_lvl7) { currentMode = MODE_LEVEL; currentLevel = 7; }
        else if (id == R.id.nav_lvl8) { currentMode = MODE_LEVEL; currentLevel = 8; }
        else if (id == R.id.nav_lvl9) { currentMode = MODE_LEVEL; currentLevel = 9; }
        else return false;

        updateTitle();
        showCurrentList();
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private int getLevelMenuId(int level) {
        switch (level) {
            case 0:  return R.id.nav_cantrips;
            case 1:  return R.id.nav_lvl1;
            case 2:  return R.id.nav_lvl2;
            case 3:  return R.id.nav_lvl3;
            case 4:  return R.id.nav_lvl4;
            case 5:  return R.id.nav_lvl5;
            case 6:  return R.id.nav_lvl6;
            case 7:  return R.id.nav_lvl7;
            case 8:  return R.id.nav_lvl8;
            case 9:  return R.id.nav_lvl9;
            default: return R.id.nav_all;
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
