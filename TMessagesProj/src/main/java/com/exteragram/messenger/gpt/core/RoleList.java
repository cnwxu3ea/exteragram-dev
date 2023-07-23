package com.exteragram.messenger.gpt.core;

import static com.exteragram.messenger.gpt.core.Config.getRoles;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Keep
public class RoleList extends ArrayList<Role> {

    public void fill() {
        ArrayList<Role> roles = getRoles();
        if (roles != null) {
            clear();
            addAll(roles);
        }
    }

    public RoleList() {
        super();
        fill();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return stream().anyMatch(r -> r.equals(o))
                || Arrays.stream(Suggestions.values())
                .map(Suggestions::getRole)
                .anyMatch(suggestedRole -> suggestedRole.equals(o));
    }

    public Role getSelected() {
        Role suggestedRole = Arrays.stream(Suggestions.values())
                .map(Suggestions::getRole)
                .filter(Role::isSelected)
                .findFirst()
                .orElse(null);

        return stream()
                .filter(Role::isSelected)
                .findFirst()
                .orElse(suggestedRole);
    }

    @Override
    public boolean add(Role role) {
        if (contains(role)) {
            edit(role);
            return false;
        }
        boolean added = super.add(role);
        if (added) {
            save();
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = super.remove(o);
        if (removed) {
            save();
        }
        return removed;
    }

    public void edit(Role newRole) {
        fill();
        stream()
                .filter(role -> role.equals(newRole))
                .findFirst()
                .ifPresent(role -> role.setPrompt(newRole.getPrompt()));
        save();
    }

    public void edit(Role oldRole, Role newRole) {
        fill();
        stream()
                .filter(role -> role.equals(oldRole))
                .findFirst()
                .ifPresent(role -> Collections.replaceAll(this, role, newRole));
        save();
    }

    private void save() {
        Collections.sort(this);
        Config.saveRoles(new ArrayList<>(this));
    }
}
