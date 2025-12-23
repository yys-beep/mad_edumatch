package com.example.mad_edumatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private RelativeLayout btnChangeLanguage;
    private Button btnLogout;
    private TextView tvCurrentLang;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Connect to the XML layout you created
        View view = inflater.inflate(R.layout.user_fragment_settings, container, false);

        // 2. Link the Java variables to the XML IDs
        btnChangeLanguage = view.findViewById(R.id.btn_change_language);
        btnLogout = view.findViewById(R.id.btn_logout_settings);
        tvCurrentLang = view.findViewById(R.id.tv_current_lang_name);

        // 3. Setup Language Change Button
        btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());

        // 4. Setup Logout Button
        btnLogout.setOnClickListener(v -> {
            // Placeholder for Logout
            Toast.makeText(getActivity(), "Logging out...", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void showLanguageDialog() {
        // The options the user will see in the popup
        String[] languages = {"English", "Bahasa Melayu"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.language_option))
                .setSingleChoiceItems(languages, -1, (dialog, which) -> {
                    if (which == 0) {
                        setLocale("en");
                    } else if (which == 1) {
                        setLocale("ms");
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void setLocale(String langCode) {
        // Update the app's language configuration
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        requireActivity().getResources().updateConfiguration(config,
                requireActivity().getResources().getDisplayMetrics());

        // Save selection in SharedPreferences (so it stays even if app closes)
        SharedPreferences prefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putString("My_Lang", langCode).apply();

        // Restart the Activity to see the new language immediately
        Intent intent = requireActivity().getIntent();
        requireActivity().finish();
        startActivity(intent);
    }
}
