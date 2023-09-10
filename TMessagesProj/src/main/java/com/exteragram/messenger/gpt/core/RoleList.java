/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

package com.exteragram.messenger.gpt.core;

import static com.exteragram.messenger.gpt.core.Config.getRoles;

import android.os.Build;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return stream().anyMatch(r -> r.equals(o))
                    || Arrays.stream(Suggestions.values())
                    .map(Suggestions::getRole)
                    .anyMatch(suggestedRole -> suggestedRole.equals(o));
        } else {
            for (Role role : this) {
                if (role.equals(o)) {
                    return true;
                }
            }

            for (Suggestions suggestion : Suggestions.values()) {
                Role suggestedRole = suggestion.getRole();
                if (suggestedRole.equals(o)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Role getSelected() {
        Role suggestedRole = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            suggestedRole = Arrays.stream(Suggestions.values())
                    .map(Suggestions::getRole)
                    .filter(Role::isSelected)
                    .findFirst()
                    .orElse(null);

            return stream()
                    .filter(Role::isSelected)
                    .findFirst()
                    .orElse(suggestedRole);
        } else {
            for (Suggestions suggestion : Suggestions.values()) {
                Role role = suggestion.getRole();
                if (role.isSelected()) {
                    suggestedRole = role;
                    break;
                }
            }

            for (Role role : this) {
                if (role.isSelected()) {
                    return role;
                }
            }
        }
        return suggestedRole;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stream()
                    .filter(role -> role.equals(newRole))
                    .findFirst()
                    .ifPresent(role -> role.setPrompt(newRole.getPrompt()));
        } else {
            for (Role role : this) {
                if (role.equals(newRole)) {
                    role.setPrompt(newRole.getPrompt());
                    break;
                }
            }
        }
        save();
    }

    public void edit(Role oldRole, Role newRole) {
        fill();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stream()
                    .filter(role -> role.equals(oldRole))
                    .findFirst()
                    .ifPresent(role -> Collections.replaceAll(this, role, newRole));
        } else {
            for (int i = 0; i < size(); i++) {
                if (get(i).equals(oldRole)) {
                    set(i, newRole);
                    break;
                }
            }
        }
        save();
    }

    private void save() {
        Collections.sort(this);
        Config.saveRoles(new ArrayList<>(this));
    }
}
