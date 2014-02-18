package org.usfirst.frc2084.vision.properties;

import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.PropertyHolder;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author Ben Wolsieffer
 */
public class SliderProperty extends IntegerProperty {

    private final JSlider slider;

    public SliderProperty(PropertyHolder element, String name, Range range, int defaultValue) {
        super(element, name, defaultValue);
        slider = new JSlider(range.getMin(), range.getMax());
        slider.setUI(new CircleSliderUI(slider));
    }

    public SliderProperty(PropertyHolder element, String name, Range range) {
        this(element, name, range, 0);
    }

    private final SliderEditor editor = new SliderEditor();

    private class SliderEditor extends AbstractCellEditor implements TableCellEditor {

        @Override
        public Object getCellEditorValue() {
            return slider.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Integer) {
                slider.setValue((Integer) value);
            }
            return slider;
        }
    }

    @Override
    public TableCellEditor getEditor(Component c) {
        return editor;
    }

}
