package org.rr.jeborker.converter;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.rr.commons.mufs.IResourceHandler;
import org.rr.commons.utils.compression.CompressedDataEntry;
import org.rr.commons.utils.compression.FileEntryFilter;
import org.rr.commons.utils.compression.truezip.TrueZipUtils;
import org.rr.jeborker.app.JeboorkerConstants;
import org.rr.jeborker.app.JeboorkerConstants.SUPPORTED_MIMES;

public class CbzToCbzConverter extends AArchiveToArchiveConverter {

	public CbzToCbzConverter(IResourceHandler cbzResource) {
		super(cbzResource);
	}
	
	@Override
	protected void addToArchive(IResourceHandler targetCbzResource, CompressedDataEntry sourceFile, byte[] imageBytes) {
		TrueZipUtils.add(targetCbzResource, sourceFile.getName(), new ByteArrayInputStream(imageBytes));
	}

	@Override
	protected void addToArchive(IResourceHandler targetCbzResource, CompressedDataEntry sourceFile, int i, byte[] imageBytes) {
		TrueZipUtils.add(targetCbzResource, injectCounterToFileName(sourceFile.getName(), i), new ByteArrayInputStream(imageBytes));
	}
	
	@Override
	protected String getTargetArchiveExtension() {
		return SUPPORTED_MIMES.MIME_CBZ.getName();
	}

	@Override
	protected List<CompressedDataEntry> extractArchive(IResourceHandler cbzResource) {
		return TrueZipUtils.extract(cbzResource, (FileEntryFilter) null);
	}
	
	@Override
	public SUPPORTED_MIMES getConversionSourceType() {
		return JeboorkerConstants.SUPPORTED_MIMES.MIME_CBZ;
	}

	@Override
	public SUPPORTED_MIMES getConversionTargetType() {
		return JeboorkerConstants.SUPPORTED_MIMES.MIME_CBZ;
	}	

}
