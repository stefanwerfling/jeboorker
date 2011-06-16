package org.rr.jeborker.gui.renderer;

import static org.rr.jeborker.JEBorkerConstants.MIME_EPUB;
import static org.rr.jeborker.JEBorkerConstants.MIME_PDF;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.rr.commons.log.LoggerFactory;
import org.rr.commons.mufs.IResourceHandler;
import org.rr.commons.mufs.ResourceHandlerFactory;
import org.rr.commons.utils.HTMLEntityConverter;
import org.rr.commons.utils.ReflectionFailureException;
import org.rr.commons.utils.ReflectionUtils;
import org.rr.commons.utils.StringUtils;
import org.rr.commons.utils.UtilConstants;
import org.rr.jeborker.db.item.EbookPropertyItem;
import org.rr.jeborker.gui.JEBorkerMainController;
import org.rr.pm.image.IImageProvider;
import org.rr.pm.image.ImageProviderFactory;
import org.rr.pm.image.ImageUtils;

import com.itextpdf.text.Font;

public class EbookTableCellRenderer extends JPanel implements TableCellRenderer, Serializable  {
	
	private static final long serialVersionUID = 1L;

	JLabel imageLabel;
	
	JLabel firstLineLabel;
	
	JLabel secondLineLabel;
	
	JLabel thirdLineLabel;
	
	JLabel firstLineRightLabel;
	
	private Dimension thumbnailDimension;
	
	private String duplicateDetection = "";
	
	/**
	 * A flag that tells where must be something to do with the labels.  
	 */
	private boolean labelSetupComplete = false;

	public EbookTableCellRenderer() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		imageLabel = new JLabel("");
		imageLabel.setOpaque(false);
		imageLabel.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_imageLabel = new GridBagConstraints();
		gbc_imageLabel.insets = new Insets(0, 0, 0, 0);
		gbc_imageLabel.gridheight = 5;
		gbc_imageLabel.gridx = 0;
		gbc_imageLabel.gridy = 1;
		add(imageLabel, gbc_imageLabel);
		
		firstLineLabel = new JLabel("");
		firstLineLabel.setOpaque(false);
		firstLineLabel.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_firstLineLabel = new GridBagConstraints();
		gbc_firstLineLabel.insets = new Insets(0, 0, 0, 0);
		gbc_firstLineLabel.anchor = GridBagConstraints.WEST;
		gbc_firstLineLabel.gridx = 1;
		gbc_firstLineLabel.gridy = 1;
		add(firstLineLabel, gbc_firstLineLabel);
		
		firstLineRightLabel = new JLabel("");
		firstLineRightLabel.setOpaque(false);
		firstLineRightLabel.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.insets = new Insets(0, 0, 0, 0);
		gbc_label.gridx = 2;
		gbc_label.gridy = 1;
		add(firstLineRightLabel, gbc_label);		
		
		secondLineLabel = new JLabel("");
		secondLineLabel.setOpaque(false);
		secondLineLabel.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_secondLineLabel = new GridBagConstraints();
		gbc_secondLineLabel.insets = new Insets(0, 0, 0, 0);
		gbc_secondLineLabel.gridwidth = 3;
		gbc_secondLineLabel.anchor = GridBagConstraints.WEST;
		gbc_secondLineLabel.gridx = 1;
		gbc_secondLineLabel.gridy = 2;
		add(secondLineLabel, gbc_secondLineLabel);
		
		thirdLineLabel = new JLabel("");
		thirdLineLabel.setOpaque(false);
		thirdLineLabel.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_thirdLineLabel = new GridBagConstraints();
		gbc_thirdLineLabel.insets = new Insets(0, 0, 0, 0);
		gbc_thirdLineLabel.gridwidth = 3;
		gbc_thirdLineLabel.anchor = GridBagConstraints.WEST;
		gbc_thirdLineLabel.gridx = 1;
		gbc_thirdLineLabel.gridy = 3;
		add(thirdLineLabel, gbc_thirdLineLabel);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, final int column) {
		//can happens that one row is rendered several times. Test here if the same row has already been rendered. 
		//no need to do it twice.
		final String duplicateDetection = String.valueOf(value) + isSelected + row;
		if(this.duplicateDetection.equals(duplicateDetection)) {
			return this;
		}
		
		final EbookPropertyItem item = (EbookPropertyItem) value;
		switch(column) {
			case 0:
				if(isSelected) {
					this.setBackground(UIManager.getColor("Tree.selectionBackground"));	
					this.setForeground(UIManager.getColor("Tree.selectionForeground"));
				} else {
					this.setBackground(UIManager.getColor("Tree.textBackground"));
					this.setForeground(UIManager.getColor("Tree.textForeground"));
				}
				
				imageLabel.setIcon(this.getImageIconCover(table, item));
				this.completeLabelSetup(table);
				
				//title
				firstLineLabel.setText(this.getTitle(item));
				
				//gray light file format
				firstLineRightLabel.setText(getDataFormat(item));
				
				secondLineLabel.setText(getAuthorAndOrderValues(item));
				
				if(item!=null && item.getDescription() != null) {
					//attach html for a multiline label but previously strip all possible html from the description. 
					String stripedDescription = cleanString(item != null ? item.getDescription() : "");
					thirdLineLabel.setText("<html>" + stripedDescription + "</html>");
				} else {
					thirdLineLabel.setText("");
				}
				break;
			case 1:
			case 2:
			case 3:
			default:
				break;	
		}
		
		this.duplicateDetection = duplicateDetection;
		return this;
	}

	/**
	 * Gets the thumbnail image to be displayed in the renderer.
	 * @param table The JTable instance.
	 * @param item The item to be rendered.
	 * @return The thumbnail image to be displayed in the renderer.
	 */
	private ImageIcon getImageIconCover(final JTable table, final EbookPropertyItem item) {
		if(item!=null && item.getCoverThumbnail()!=null && item.getCoverThumbnail().length > 0) {
			try {
				final byte[] coverData = item.getCoverThumbnail();
				if(coverData!=null) {
					final IResourceHandler virtualImageResourceLoader = ResourceHandlerFactory.getVirtualResourceLoader("TableCellRendererImageData", coverData);
					final IImageProvider imageProvider = ImageProviderFactory.getImageProvider(virtualImageResourceLoader);
					final BufferedImage image = imageProvider.getImage();
					if(image!=null) {	
						BufferedImage scaleToMatch = ImageUtils.scaleToMatch(imageProvider.getImage(), getThumbnailDimension(table), false);
						ImageIcon imageIcon = new ImageIcon(scaleToMatch);
						return imageIcon;
					} else {
						return null;
					}
				}
			} catch (Exception e) {
				LoggerFactory.logInfo(this, "Could not render thumbnail", e);
			} 
		}
		return null;
	}
	
	/**
	 * Gets the dimension for the thumbnail in the view.
	 * @param table The tabel which shows the thumbnail.
	 * @return The dimension for the thumbnail.
	 */
	private Dimension getThumbnailDimension(final JTable table) {
		if(thumbnailDimension==null) {
			thumbnailDimension = new Dimension((int) (table.getRowHeight()*0.7), table.getRowHeight());	
		}
		return thumbnailDimension;
	}
	
	private String getAuthorAndOrderValues(EbookPropertyItem item) {
		if(item == null) {
			return "";
		}
		
		final StringBuilder result = new StringBuilder();
		final List<Field> selectedFields = JEBorkerMainController.getController().getSortColumnComponentController().getSelectedFields();
		
		for (Field field : selectedFields) {
			//do not add the folowing ones.
			if(field.getName().equalsIgnoreCase("author")) {
				continue;
			} else if(field.getName().equalsIgnoreCase("title")) {
				continue;
			} else if(field.getName().equalsIgnoreCase("description")) {
				continue;
			}
			
			try {
				final Object fieldValueObject = ReflectionUtils.getFieldValue(item, field.getName());
				final String fieldValueString = StringUtils.toString(fieldValueObject);
				if(StringUtils.isNotEmpty(fieldValueString)) {
					if(StringUtils.isNotEmpty(result)) {
						result.append(", ");
					}
					
					if(fieldValueObject instanceof Date) {
						result.append(SimpleDateFormat.getDateInstance().format(fieldValueObject));
					} else {
						result.append(fieldValueString);
					}
				}
			} catch (ReflectionFailureException e) {
				LoggerFactory.logWarning(this, "No field named " + field.getName(), e);
			}
		}
		
		//prepend the author to the result string 
		if(StringUtils.isNotEmpty(item.getAuthor())) {
			if(StringUtils.isNotEmpty(result)) {
				result.insert(0, ", ");
			}
			result.insert(0, item.getAuthor());
		}
		return result.toString();
	}
	
	/**
	 * Creates the title of the document.
	 * @param item The item where the title should be created from.
	 * @return The desired book title.
	 */
	private String getTitle(EbookPropertyItem item) {
		if(item!=null && StringUtils.isEmpty(item.getTitle())) {
			//if there is no title, just use the file name but without file extension
			final String fileName = StringUtils.substringBefore(item.getFileName(), ".", false, UtilConstants.COMPARE_BINARY);
			return fileName;
		} else {
			return cleanString(item != null ? item.getTitle() : "");
		}		
	}
	
	/**
	 * Remove all html tags an decode entities.
	 * @param toClean The text to be cleaned.
	 * @return
	 */
	private String cleanString(String toClean) {
		if(toClean==null) {
			return "";
		}
		if(toClean.indexOf('&')!=-1) {
			toClean = new HTMLEntityConverter(toClean, HTMLEntityConverter.ENCODE_EIGHT_BIT_ASCII).decodeEntities();
		}
		
		if(toClean.indexOf('<')!=-1) {
			toClean = Jsoup.clean(toClean, Whitelist.none());
		}
		return toClean.trim();
	}
	
	/**
	 * Gets the string value for the file format to be displayed in the renderer.
	 * For example "pdf" if it's a pdf file.
	 * @param item The item where the string should be evaluated for.
	 * @return The detected file format string or an empty string if the format coult not
	 * be detected. Never returns <code>null</code>.
	 */
	private String getDataFormat(EbookPropertyItem item) {
		if(item!=null) {
			if(MIME_EPUB.equals(item.getMimeType())) {
				return "epub";
			} else if(MIME_PDF.equals(item.getMimeType())) {
				return "pdf";
			}
		}
		return "";
	}

	/**
	 * take shure that the labels have a constant allocation.
	 */
	private void completeLabelSetup(JTable table) {
		if(!labelSetupComplete) {
			final int oneLineHeight = 19;
			final int lastLabelHeight = table.getRowHeight() - oneLineHeight - oneLineHeight;
			
			java.awt.Font f = firstLineLabel.getFont();
			firstLineLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
			firstLineLabel.setBorder(new EmptyBorder(0,0,0,0));
			firstLineLabel.setMinimumSize(new Dimension(table.getWidth(), oneLineHeight));
			
			firstLineRightLabel.setBorder(new EmptyBorder(0,0,0,5));
			firstLineRightLabel.setForeground(Color.GRAY);
			
			secondLineLabel.setMinimumSize(new Dimension(table.getWidth(), oneLineHeight));
			secondLineLabel.setBorder(new EmptyBorder(0,0,0,0));
			
			thirdLineLabel.setMinimumSize(new Dimension(table.getWidth(), lastLabelHeight));
			
			imageLabel.setMinimumSize(new Dimension(50, table.getRowHeight()));
			imageLabel.setMaximumSize(new Dimension(50, table.getRowHeight()));
			imageLabel.setSize(new Dimension(50, table.getRowHeight()));
			imageLabel.setPreferredSize(new Dimension(50, table.getRowHeight()));
			labelSetupComplete = true;
		}		
	}
}
