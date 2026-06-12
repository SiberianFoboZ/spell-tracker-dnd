package com.example.spelltracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Экран подробностей заклинания. Получает только {@code long} id в Intent,
 * читает полные данные из Room через {@link SpellRepository}.
 */
public class SpellDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SPELL_ID = "spell_id";

    private long spellId;

    public static Intent newIntent(Context context, long spellId) {
        Intent i = new Intent(context, SpellDetailActivity.class);
        i.putExtra(EXTRA_SPELL_ID, spellId);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spell_detail);

        // Edge-to-edge: контент не уезжает за нижний nav bar.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Нижний отступ под системную навигацию.
        View scrollView = findViewById(R.id.scroll_view);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom);
            return windowInsets;
        });

        spellId = getIntent().getLongExtra(EXTRA_SPELL_ID, -1L);
        if (spellId < 0) {
            finish();
            return;
        }

        new SpellRepository(this).getById(spellId, this::render);
    }

    private void render(Spell spell) {
        if (spell == null) {
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(spell.name);
        }

        ((TextView) findViewById(R.id.tv_school))
                .setText(spell.school + " · " + spell.levelLabel());
        ((TextView) findViewById(R.id.tv_cast)).setText(nullToEmpty(spell.castingTime));
        ((TextView) findViewById(R.id.tv_range)).setText(nullToEmpty(spell.range));
        ((TextView) findViewById(R.id.tv_components)).setText(nullToEmpty(spell.components));
        ((TextView) findViewById(R.id.tv_duration)).setText(nullToEmpty(spell.duration));
        ((TextView) findViewById(R.id.tv_description)).setText(nullToEmpty(spell.description));

        TextView tvHigher = findViewById(R.id.tv_higher);
        TextView tvHigherHeader = findViewById(R.id.tv_higher_header);
        if (spell.higherLevel == null || spell.higherLevel.trim().isEmpty()) {
            tvHigherHeader.setVisibility(TextView.GONE);
            tvHigher.setVisibility(TextView.GONE);
        } else {
            tvHigher.setText(spell.higherLevel);
            tvHigherHeader.setVisibility(TextView.VISIBLE);
            tvHigher.setVisibility(TextView.VISIBLE);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
