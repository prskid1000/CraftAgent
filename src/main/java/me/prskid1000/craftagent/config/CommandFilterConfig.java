package me.prskid1000.craftagent.config;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for filtering commands by type and category.
 * Used to disable certain commands from appearing in LLM prompts and web UI.
 */
public class CommandFilterConfig {
    private Set<String> disabledTypes = new HashSet<>();
    private Set<String> disabledCategories = new HashSet<>();
    
    public CommandFilterConfig() {
        // Default: all enabled
    }
    
    public Set<String> getDisabledTypes() {
        return new HashSet<>(disabledTypes);
    }
    
    public Set<String> getDisabledCategories() {
        return new HashSet<>(disabledCategories);
    }
    
    public void setDisabledTypes(Set<String> disabledTypes) {
        this.disabledTypes = disabledTypes != null ? new HashSet<>(disabledTypes) : new HashSet<>();
    }
    
    public void setDisabledCategories(Set<String> disabledCategories) {
        this.disabledCategories = disabledCategories != null ? new HashSet<>(disabledCategories) : new HashSet<>();
    }
    
    public boolean isTypeDisabled(String type) {
        return disabledTypes.contains(type);
    }
    
    public boolean isCategoryDisabled(String category) {
        return disabledCategories.contains(category);
    }
    
    public void toggleType(String type) {
        if (disabledTypes.contains(type)) {
            disabledTypes.remove(type);
        } else {
            disabledTypes.add(type);
        }
    }
    
    public void toggleCategory(String category) {
        if (disabledCategories.contains(category)) {
            disabledCategories.remove(category);
        } else {
            disabledCategories.add(category);
        }
    }
    
    public boolean isTypeEnabled(String type) {
        return !isTypeDisabled(type);
    }
    
    public boolean isCategoryEnabled(String category) {
        return !isCategoryDisabled(category);
    }
}



