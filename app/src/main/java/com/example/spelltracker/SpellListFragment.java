package com.example.spelltracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Фрагмент со списком заклинаний. Режим задаётся аргументами:
 * <ul>
 *   <li>{@code level} — 0..9: уровень заклинаний; -1 ({@link #LEVEL_PREPARED})
 *       — все подготовленные; -2 ({@link #LEVEL_ALL}) — весь список.</li>
 *   <li>{@code classIds} — список ID D&D-классов для фильтрации
 *       (multi-select, см. {@link ClassFilter}). {@code null} или пустой —
 *       фильтр отключён.</li>
 * </ul>
 *
 * <p>Данные читаются из Room-базы через {@link SpellRepository} асинхронно.
 * Список перезагружается в {@link #onResume()}.</p>
 */
public class SpellListFragment extends Fragment {

    private static final String ARG_LEVEL = "level";
    private static final String ARG_CLASS_IDS = "class_ids";
    private static final String ARG_QUERY = "query";

    public static final int LEVEL_PREPARED = -1;
    public static final int LEVEL_ALL = -2;

    private int level = 0;
    private ArrayList<String> classIds = null;
    private String searchQuery = "";

    private final List<Spell> loaded = new ArrayList<>();
    private final List<Spell> display = new ArrayList<>();

    private SpellStorage storage;
    private SpellAdapter adapter;
    private SpellRepository repo;
    private ClassFilter classFilter;
    private TextView empty;

    public static SpellListFragment newInstance(int level, @Nullable ArrayList<String> classIds,
                                                @Nullable String query) {
        SpellListFragment f = new SpellListFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_LEVEL, level);
        b.putStringArrayList(ARG_CLASS_IDS, classIds);
        b.putString(ARG_QUERY, query == null ? "" : query);
        f.setArguments(b);
        return f;
    }

    /** Обратная совместимость: без поискового запроса. */
    public static SpellListFragment newInstance(int level, @Nullable ArrayList<String> classIds) {
        return newInstance(level, classIds, "");
    }

    /**
     * Обновляет подстроку поиска и пересчитывает фильтр без пересоздания фрагмента.
     * Пустая строка / {@code null} отключает фильтр.
     */
    public void setSearchQuery(@Nullable String query) {
        String q = query == null ? "" : query.trim();
        if (q.equalsIgnoreCase(searchQuery)) return;
        searchQuery = q.toLowerCase(java.util.Locale.ROOT);
        applyFilters();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            level = args.getInt(ARG_LEVEL, 0);
            classIds = args.getStringArrayList(ARG_CLASS_IDS);
            String q = args.getString(ARG_QUERY, "");
            searchQuery = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spell_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storage = new SpellStorage(requireContext());
        repo = new SpellRepository(requireContext());
        classFilter = ClassFilter.get(requireContext());

        RecyclerView rv = view.findViewById(R.id.list);
        empty = view.findViewById(R.id.empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SpellAdapter(display, storage, new SpellAdapter.Listener() {
            @Override
            public void onPreparedChanged(Spell spell, boolean prepared) {
                storage.setPrepared(spell, prepared);
                if (level == LEVEL_PREPARED) {
                    applyFilters();
                } else {
                    if (adapter != null) adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onOpen(Spell spell) {
                startActivity(SpellDetailActivity.newIntent(requireContext(), spell.id));
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repo == null) repo = new SpellRepository(requireContext());
        if (classFilter == null) classFilter = ClassFilter.get(requireContext());
        reload();
    }

    private void reload() {
        if (level == LEVEL_PREPARED || level == LEVEL_ALL) {
            repo.getAll(spells -> {
                if (!isAdded()) return;
                loaded.clear();
                loaded.addAll(spells);
                applyFilters();
            });
        } else {
            final int wantLevel = level;
            repo.getByLevel(wantLevel, spells -> {
                if (!isAdded()) return;
                loaded.clear();
                loaded.addAll(spells);
                applyFilters();
            });
        }
    }

    private void applyFilters() {
        display.clear();
        for (Spell s : loaded) {
            if (level == LEVEL_PREPARED && !storage.isPrepared(s)) continue;
            if (!classFilter.matches(s, classIds)) continue;
            if (!searchQuery.isEmpty()) {
                String name = s.name == null ? "" : s.name.toLowerCase(java.util.Locale.ROOT);
                if (!name.contains(searchQuery)) continue;
            }
            display.add(s);
        }
        if (level == LEVEL_PREPARED || level == LEVEL_ALL) {
            Collections.sort(display, (a, b) -> {
                if (a.level != b.level) return Integer.compare(a.level, b.level);
                return a.name.compareToIgnoreCase(b.name);
            });
        } else {
            Collections.sort(display, (a, b) -> a.name.compareToIgnoreCase(b.name));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmpty();
    }

    private void updateEmpty() {
        if (empty == null) return;
        if (display.isEmpty()) {
            String message;
            if (level == LEVEL_PREPARED) {
                message = "Нет подготовленных заклинаний.\nОткройте уровни и отметьте нужные.";
            } else if (!searchQuery.isEmpty()) {
                message = "Ничего не найдено по запросу «" + searchQuery + "».";
            } else if (classIds != null && !classIds.isEmpty()) {
                message = "Нет заклинаний выбранных классов.";
            } else {
                message = "Нет заклинаний этого уровня";
            }
            empty.setText(message);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }
}
