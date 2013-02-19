package org.rr.jeborker.metadata;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.jempbox.xmp.XMPUtils;
import org.rr.commons.log.LoggerFactory;
import org.rr.commons.mufs.IResourceHandler;
import org.rr.commons.mufs.ResourceHandlerFactory;
import org.rr.commons.utils.CommonUtils;
import org.rr.pm.image.ImageUtils;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfStream;

abstract class PDFCommonDocument {

	public static final int ITEXT = 0;
	
	private static final double MIN_IMAGE_COVER_WIDTH = 1.4d;
	
	private static final double MAX_IMAGE_COVER_WIDTH = 1.7d;
	
	private IResourceHandler pdfFile;
	
	/**
	 * Store sthe PDF key/value info. If it's null, the Info in the
	 * pdf file didn't get touched.
	 */
	protected Map<String, String> moreInfo;
	
	protected byte[] xmpMetadata;
	
	public static PDFCommonDocument getInstance(int type, IResourceHandler pdfFile) {
		PDFCommonDocument result = null;
		switch(type) {
		case ITEXT:
			result = new PDFCommonDocument.ItextPDFDocument(pdfFile);
			break;
		}
		result.setResourceHandler(pdfFile);
		return result;
	}
	
	/**
	 * Set the pdf file containing the pdf data used for the {@link PDFCommonDocument} instance.
	 * @param pdfFile The pdf file to be used.
	 */
	protected void setResourceHandler(IResourceHandler pdfFile)  {
		this.pdfFile = pdfFile;
	}
	
	/**
	 * get the pdf file for this {@link PDFCommonDocument} instance.
	 * @return The desired {@link PDFCommonDocument}.
	 */
	public IResourceHandler getResourceHandler() {
		return this.pdfFile;
	}
	
	/**
	 * Read the xmp metadata as byte array.
	 * @return The desired xmp bytes.
	 */
	public abstract byte[] getXMPMetadata() throws IOException;
	
	/**
	 * Get a map with values contained in the pdf info block.
	 * @return The desired info values.
	 */
	public abstract Map<String, String> getInfo() throws IOException;
	
	/**
	 * Set the map values for the pdf. 
	 * @param info The metadata key/values to be written to the pdf.
	 */
	public void setInfo(Map<String, String> info) {
		this.moreInfo = info;
	}
	
	public void setXMPMetadata(byte[] xmpMetadata) {
		this.xmpMetadata = xmpMetadata;
	}
	
	public abstract byte[] fetchCoverFromPDFContent() throws IOException;
	
	/**
	 * Write the previously set metadata to the file.
	 * @throws IOException
	 */
	public abstract void write() throws IOException;
	
	/**
	 * Dispose this {@link PDFCommonDocument} instance. It could no longer be used
	 * after invoking dispose.
	 */
	public abstract void dispose();
	
	/**
	 * Renders a page of the given pdf data. 
	 * @param pdfdata The pdf bytes.
	 * @param pageNumber The page number to be rendered starting with 1.
	 * @return The rendered pdf data or <code>null</code> if the pdf could not be rendered.
	 * @throws IOException
	 */
	byte[] renderPage(byte[] pdfdata, int pageNumber) throws Exception {
		com.jmupdf.pdf.PdfDocument doc = null;
		com.jmupdf.page.PagePixels pp = null;
		com.jmupdf.page.Page page = null;
		try {
			doc = new com.jmupdf.pdf.PdfDocument(pdfdata);

			page = doc.getPage(pageNumber);

			pp = new com.jmupdf.page.PagePixels(page);
			pp.setRotation(com.jmupdf.page.Page.PAGE_ROTATE_NONE);
			pp.drawPage(null, pp.getX0(), pp.getY0(), pp.getX1(), pp.getY1());
			BufferedImage image = pp.getImage();
			byte[] imageBytes = ImageUtils.getImageBytes(image, "image/jpeg");
			return imageBytes;
		} finally {
			if (pp != null)
				pp.dispose();
			if (doc != null)
				doc.dispose();
		}
	}	
	
	
	private static class ItextPDFDocument extends PDFCommonDocument {

		private PdfReader pdfReader;
		
		byte[] pdfdata = null;
		
		ItextPDFDocument(IResourceHandler pdfFile) {
			try {
				pdfdata = pdfFile.getContent();
				
				//use byte[] instead of InputStream because itext read the stream
				//into a ByteArrayOutputStream and than does a toByteArray() which 
				//does another copy of the bytes.
				this.pdfReader = new PdfReader(pdfdata);
			} catch(Throwable e) {
				throw new RuntimeException(e.getMessage() + " at '" + pdfFile.getName() + "'", e);
			} 
		}
		
		@Override
		public byte[] getXMPMetadata() throws IOException {
			if(this.xmpMetadata == null) {
				final byte[] xmpMetadataBytes = pdfReader.getMetadata();
				if(XMPUtils.isValidXMP(xmpMetadataBytes)) {
					this.xmpMetadata = xmpMetadataBytes;
				}
			}
			return this.xmpMetadata;
		}

		@Override
		public Map<String, String> getInfo() throws IOException {
			if(moreInfo == null) {
				moreInfo = pdfReader.getInfo();
				return moreInfo;
			}
			return moreInfo;
		}

		@Override
		public void write() throws IOException {
			final IResourceHandler ebookResource = getResourceHandler();
			final IResourceHandler tmpEbookResourceLoader = ResourceHandlerFactory.getUniqueResourceHandler(ebookResource, "tmp");
			PdfStamper stamper = null;
			OutputStream ebookResourceOutputStream = null;
			
			try {
				ebookResourceOutputStream = tmpEbookResourceLoader.getContentOutputStream(false);
				stamper = new PdfStamper(pdfReader, ebookResourceOutputStream);
				byte[] xmp = this.xmpMetadata != null ? this.xmpMetadata : getXMPMetadata();
				stamper.setXmpMetadata(XMPUtils.handleMissingXMPRootTag(xmp));
				Map<String, String> info = this.moreInfo != null ? this.moreInfo : getInfo();
				if(this.moreInfo != null) {
					//to delete old entries, itext need to null them.
					HashMap<String, String> oldInfo = pdfReader.getInfo();
					HashMap<String, String> newInfo = new HashMap<String, String>(oldInfo.size() + this.moreInfo.size());
					for (Iterator<String> it = oldInfo.keySet().iterator(); it.hasNext();) {
						newInfo.put(it.next(), null); 
					}
					newInfo.putAll(info);
					
					stamper.setMoreInfo(newInfo);
				}
			} catch(Exception e) {
				throw new IOException(e);
			} finally {
				if (stamper != null) {
					try {
						stamper.close();
					} catch (DocumentException e) {
						LoggerFactory.logWarning(this, "Could not close pdf stamper for " + ebookResource, e);
					} catch (IOException e) {
						LoggerFactory.logWarning(this, "Could not close pdf stamper for " + ebookResource, e);
					}
				}
				if (ebookResourceOutputStream != null) {
					try {
						ebookResourceOutputStream.flush();
					} catch (IOException e) {
					}
					IOUtils.closeQuietly(ebookResourceOutputStream);
				}
				if(tmpEbookResourceLoader.size() > 0) {
					//new temp pdf looks good. Move the new temp one over the old one. 
					tmpEbookResourceLoader.moveTo(ebookResource, true);
				} else {
					tmpEbookResourceLoader.delete();
				}
			}			
		}

		@Override
		public void dispose() {
			if(this.pdfReader != null) {
				this.pdfReader.close();
			}
		}
		
		public byte[] fetchCoverFromPDFContent() throws IOException {
			try {
				return renderPage(pdfdata, 1);
			} catch (Exception e) {
				LoggerFactory.log(Level.WARNING, this, "could not render PDF " + getResourceHandler());
			}
			
			int xrefSize = pdfReader.getXrefSize();
			for (int i = 0; i < xrefSize; i++) { //process the first ten xrefs.
				PdfObject pdfobj = pdfReader.getPdfObject(i);
				if(pdfobj != null) {
					if (pdfobj.isStream()) {
						PdfStream stream = (PdfStream) pdfobj;
						
						PdfObject pdfsubtype = stream.get(PdfName.SUBTYPE);
						if (pdfsubtype == null || !pdfsubtype.toString().equals(PdfName.IMAGE.toString())) {
							continue;
						}
		
						// now you have a PDF stream object with an image
						byte[] img = PdfReader.getStreamBytesRaw((PRStream) stream);
						if(img.length > 1000) {
							int width = 0;
							int height = 0;
							try {
								width = Integer.parseInt(stream.get(PdfName.WIDTH).toString());
								height = Integer.parseInt(stream.get(PdfName.HEIGHT).toString());
								
								if(width <= 0 || height <= 0) {
									continue;
								}
								
								PdfObject bitspercomponent = stream.get(PdfName.BITSPERCOMPONENT);
								if(bitspercomponent!=null) {
									Number bitspercomponentNum = CommonUtils.toNumber(bitspercomponent.toString());
									if(bitspercomponentNum!=null && bitspercomponentNum.intValue()==1) {
										//no b/w images
										continue;
									}
								}							
							} catch(Exception e) {}
							
							double aspectRatio = ((double)height) / ((double)width);
							if(width > 150 && aspectRatio > MIN_IMAGE_COVER_WIDTH && aspectRatio < MAX_IMAGE_COVER_WIDTH) {
								return img;
							}
						}
					}
				}
			}
			return null;
		}
		
	}	
	
}
