package com.example.spelltracker;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Загружает списки заклинаний для D&D-классов из индивидуальных JSON-файлов
 * в {@code assets/} (по одному файлу на класс; имена берутся из
 * {@link Classes}). Используется для фильтрации общего списка заклинаний
 * по выбранным классам в SpellsActivity.
 *
 * <p>Singleton — JSON читается один раз, при первом обращении. Если файл
 * отсутствует или повреждён, класс просто пропускается (matches() вернёт
 * false для его заклинаний).</p>
 *
 * <p>Сравнение имён — без учёта регистра и крайних пробелов. Заголовки
 * секций вида «1-й круг:», «Заговоры:» игнорируются.</p>
 */
public class ClassFilter {

    private static volatile ClassFilter INSTANCE;

    private final List<String> classIds = new ArrayList<>();
    private final Map<String, String> classNames = new HashMap<>();
    private final Map<String, Set<String>> classSpells = new HashMap<>();

    public static ClassFilter get(Context context) {
        if (INSTANCE == null) {
            synchronized (ClassFilter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ClassFilter(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private ClassFilter(Context context) {
        for (Classes.Info info : Classes.ALL) {
            try {
                String json = readAsset(context, info.assetFile);
                JSONObject root = new JSONObject(json);
                JSONArray sp = root.optJSONArray("spells");
                Set<String> set = new HashSet<>();
                if (sp != null) {
                    for (int j = 0; j < sp.length(); j++) {
                        String s = sp.getString(j).toLowerCase(Locale.ROOT).trim();
                        if (s.isEmpty() || s.endsWith(":")) continue;
                        set.add(s);
                    }
                }
                classIds.add(info.id);
                classNames.put(info.id, info.name);
                classSpells.put(info.id, set);
            } catch (Exception e) {
                // Файл отсутствует или повреждён — класс будет без списка заклинаний.
                e.printStackTrace();
            }
        }
    }

    /** @return упорядоченный список ID классов (порядок из {@link Classes#ALL}). */
    public List<String> getClassIds() {
        return Collections.unmodifiableList(classIds);
    }

    public String getClassName(String id) {
        String n = classNames.get(id);
        return n == null ? id : n;
    }

    /**
     * @param spell       заклинание из БД
     * @param selectedIds выбранные классы; {@code null} или пустой — фильтр отключён
     * @return {@code true}, если фильтр отключён или заклинание входит
     *         хотя бы в один из выбранных классов
     */
    public boolean matches(Spell spell, List<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) return true;
        if (classIds.isEmpty()) return true;
        String name = spell.name == null ? "" : spell.name.toLowerCase(Locale.ROOT).trim();
        for (String id : selectedIds) {
            Set<String> set = classSpells.get(id);
            if (set != null && set.contains(name)) return true;
        }
        return false;
    }

    private static String readAsset(Context ctx, String name) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = ctx.getAssets().open(name);
             BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
