package org.rr.jeborker.gui.renderer;

import java.awt.Component;
import java.util.logging.Level;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;

import org.rr.common.swing.StarRater;
import org.rr.commons.log.LoggerFactory;
import org.rr.commons.utils.CommonUtils;

public class DefaultPropertyRenderer extends DefaultTableCellRenderer {
	
	private static final long serialVersionUID = -9177633701463286482L;
	

	public DefaultPropertyRenderer() {
	}

	
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    	Component tableCellRendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
    	RendererUtils.setColor(tableCellRendererComponent, isSelected);
    	
    	return tableCellRendererComponent;
    }	

}