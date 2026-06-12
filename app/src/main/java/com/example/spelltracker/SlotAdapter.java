package com.example.spelltracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

/**
 * Адаптер для строк счётчиков ячеек заклинаний 1..9 уровня.
 * Каждая строка: "Уровень N" • "использовано/всего" • [use] [restore].
 * Общее число ячеек не редактируется вручную — задаётся по таблице
 * {@link SpellStorage#applySlotTable()}.
 */
public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.VH> {

    public interface Listener {
        void onUse(int level);
        void onRestore(int level);
    }

    private final SpellStorage storage;
    private final Listener listener;

    public SlotAdapter(SpellStorage storage, Listener listener) {
        this.storage = storage;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        int level = position + 1;
        int total = storage.getSlotTotal(level);
        int used = storage.getSlotUsed(level);
        h.level.setText("Уровень " + level);
        h.count.setText(used + " / " + total);
        h.btnUse.setOnClickListener(v -> listener.onUse(level));
        h.btnRestore.setOnClickListener(v -> listener.onRestore(level));
        h.btnUse.setEnabled(used < total);
        h.btnRestore.setEnabled(used > 0);
    }

    @Override
    public int getItemCount() { return 9; }

    static class VH extends RecyclerView.ViewHolder {
        final TextView level, count;
        final MaterialButton btnUse, btnRestore;

        VH(@NonNull View v) {
            super(v);
            level = v.findViewById(R.id.slot_level);
            count = v.findViewById(R.id.slot_count);
            btnUse = v.findViewById(R.id.btn_use);
            btnRestore = v.findViewById(R.id.btn_restore);
        }
    }
}
