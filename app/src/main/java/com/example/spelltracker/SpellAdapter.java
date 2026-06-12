package com.example.spelltracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Адаптер для списка заклинаний с галочкой "Подготовлено".
 * По нажатию на строку открывается {@link SpellDetailActivity}.
 */
public class SpellAdapter extends RecyclerView.Adapter<SpellAdapter.VH> {

    public interface Listener {
        void onPreparedChanged(Spell spell, boolean prepared);
        void onOpen(Spell spell);
    }

    private final List<Spell> spells;
    private final SpellStorage storage;
    private final Listener listener;

    public SpellAdapter(List<Spell> spells, SpellStorage storage, Listener listener) {
        this.spells = spells;
        this.storage = storage;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_spell, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Spell s = spells.get(position);
        h.name.setText(s.name);
        h.subtitle.setText(s.levelLabel() + " • " + s.school);
        // Снимаем слушатель, чтобы не сработал при перезаписи
        h.checkbox.setOnCheckedChangeListener(null);
        h.checkbox.setChecked(storage.isPrepared(s));
        h.checkbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                listener.onPreparedChanged(s, isChecked));
        h.itemView.setOnClickListener(v -> listener.onOpen(s));
    }

    @Override
    public int getItemCount() { return spells.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, subtitle;
        final CheckBox checkbox;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.spell_name);
            subtitle = v.findViewById(R.id.spell_subtitle);
            checkbox = v.findViewById(R.id.spell_prepared);
        }
    }
}
