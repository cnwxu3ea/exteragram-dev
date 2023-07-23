package com.exteragram.messenger.gpt.ui;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Pattern;

public class ApiKeyInputFilter implements InputFilter {

    private final Pattern pattern;

    public ApiKeyInputFilter() {
        pattern = Pattern.compile("[a-zA-Z\\d-]");
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder filteredStringBuilder = new StringBuilder();
        for (int i = start; i < end; i++) {
            char ch = source.charAt(i);
            if (pattern.matcher(String.valueOf(ch)).matches()) {
                filteredStringBuilder.append(ch);
            }
        }
        boolean allCharactersValid = (filteredStringBuilder.length() == (end - start));

        String newSource = dest.subSequence(0, dstart) + source.subSequence(start, end).toString() + dest.subSequence(dend, dest.length());

        if (!allCharactersValid || newSource.length() > 0 && (newSource.length() == 1 && !newSource.startsWith("s") || newSource.length() == 2 && !newSource.startsWith("sk") || newSource.length() == 3 && !newSource.startsWith("sk-"))) {
            return "";
        }
        return null;
    }
}
