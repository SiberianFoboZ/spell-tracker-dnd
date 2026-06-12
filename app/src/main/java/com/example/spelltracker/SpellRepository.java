package com.example.spelltracker;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единая точка доступа к данным заклинаний.
 *
 * <p>Логика: при первом обращении таблица пуста — данные импортируются из
 * {@code assets/spells.csv} (с транзакцией, чтобы ~480 вставок были быстрыми).
 * Все дальнейшие запросы идут через {@link SpellDao}.</p>
 *
 * <p>Все методы асинхронные, колбэки вызываются на UI-потоке.</p>
 */
public class SpellRepository {

    /** Простой колбэк. */
    public interface Callback<T> {
        void onResult(T result);
    }

    private final SpellDao dao;
    private final SpellDatabase db;
    private final Context appContext;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public SpellRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = SpellDatabase.get(appContext);
        this.dao = db.spellDao();
    }

    /**
     * Инициализирует БД: если таблица пуста — импортирует из CSV.
     * Колбэк получает общее число записей после инициализации.
     */
    public void loadOrImport(Callback<Integer> onReady) {
        io.execute(() -> {
            try {
                if (dao.count() == 0) {
                    importFromCsv();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            final int c = dao.count();
            main.post(() -> {
                if (onReady != null) onReady.onResult(c);
            });
        });
    }

    public void getAll(Callback<List<Spell>> cb) {
        io.execute(() -> {
            final List<Spell> result = dao.getAll();
            main.post(() -> {
                if (cb != null) cb.onResult(result);
            });
        });
    }

    public void getByLevel(int level, Callback<List<Spell>> cb) {
        io.execute(() -> {
            final List<Spell> result = dao.getByLevel(level);
            main.post(() -> {
                if (cb != null) cb.onResult(result);
            });
        });
    }

    public void getById(long id, Callback<Spell> cb) {
        io.execute(() -> {
            final Spell s = dao.getById(id);
            main.post(() -> {
                if (cb != null) cb.onResult(s);
            });
        });
    }

    /** Синхронный импорт CSV в БД (вызывается в фоновом потоке). */
    private void importFromCsv() throws IOException {
        List<Spell> all = SpellParser.loadFromAssets(appContext);
        db.runInTransaction(() -> {
            for (Spell s : all) {
                dao.insert(s);
            }
        });
    }
}
