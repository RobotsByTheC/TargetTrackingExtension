/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.usfirst.frc2084.vision.properties;

import edu.wpi.first.smartdashboard.properties.GenericProperty;
import edu.wpi.first.smartdashboard.properties.PropertyHolder;
import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author ben
 */
public class RangeProperty extends GenericProperty<Range> {

    private final Range maxRange;

    public RangeProperty(PropertyHolder holder, String name, Range range) {
        this(holder, name, range, range);
    }

    public RangeProperty(PropertyHolder holder, String name, Range range, Range defaultValue) {
        super(Range.class, holder, name, defaultValue);
        this.maxRange = range;
        slider = new RangeSlider(maxRange.getMin(), maxRange.getMax());
        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {

                Range r = new Range(slider.getValue(), slider.getUpperValue());
                System.out.println("stateChanged: " + r);
                setValue(r);
            }
        });
    }

    private final RangeSlider slider;

    private final RangeEditor editor = new RangeEditor();

    private class RangeEditor extends AbstractCellEditor implements TableCellEditor {

        @Override
        public Object getCellEditorValue() {
            return getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return slider;
        }

    };

    @Override
    public TableCellRenderer getRenderer() {
        return null;
    }

    @Override
    public TableCellEditor getEditor(Component c) {
        return editor;
    }

    @Override
    protected Range transformValue(Object value) {
        if (value instanceof String) {
            String strVal = (String) value;
            String[] vals = strVal.split(",");
            Range r = new Range(Integer.parseInt(vals[0]), Integer.parseInt(vals[1]));
            System.out.println("transformValue: " + strVal);
            return r;
        } else {
            return super.transformValue(value);
        }
    }

    @Override
    protected void valueChanged() {
        System.out.println("valueChanged: " + getValue().getMin() + "," + getValue().getMax());
        slider.setValue(getValue().getMin());
        slider.setUpperValue(getValue().getMax());
    }

}
