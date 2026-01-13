package edu.univ.erp.util;

public class ComboBoxItem<T> {
    private final T id;
    private final String label;
    
    public ComboBoxItem(T id, String label) {
        this.id = id;
        this.label = label;
    }
    
    public T getId() { return id; }
    public String getLabel() { return label; }
    
    @Override
    public String toString() { return label; }
}