package org.usfirst.frc2084.vision.properties;

import edu.wpi.first.smartdashboard.properties.GenericProperty;
import edu.wpi.first.smartdashboard.properties.PropertyHolder;
import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
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
        slider.setRange(defaultValue);
    }

    private final RangeSlider slider;

    private final RangeEditor editor = new RangeEditor();

    private class RangeEditor extends AbstractCellEditor implements TableCellEditor {

        @Override
        public Object getCellEditorValue() {
            return slider.getRange();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof Range) {
                slider.setRange((Range) value);
            }
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
            return new Range(Integer.parseInt(vals[0]), Integer.parseInt(vals[1]));
        } else {
            return super.transformValue(value);
        }
    }

}
