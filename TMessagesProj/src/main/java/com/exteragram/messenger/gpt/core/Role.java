package com.exteragram.messenger.gpt.core;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class Role implements Comparable<Role>, Serializable {
    private String name;
    private String prompt;
    private boolean isSuggestion;

    public Role(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;
    }

    @Override
    public int compareTo(Role otherPerson) {
        return this.name.compareTo(otherPerson.getName());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Role otherRole = (Role) obj;

        return name.equals(otherRole.name);
    }

    public String getName() {
        if (name == null) {
            return "";
        }
        return name;
    }

    public String getPrompt() {
        if (prompt == null) {
            return "";
        }
        return prompt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Role setSuggestion(boolean isSuggestion) {
        this.isSuggestion = isSuggestion;
        return this;
    }

    public boolean isSuggestion() {
        return isSuggestion;
    }

    public boolean isSelected() {
        return Objects.equals(Config.getSelectedRole(), name);
    }
}
