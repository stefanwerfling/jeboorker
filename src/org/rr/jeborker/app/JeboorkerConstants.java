package org.rr.jeborker.app;


public class JeboorkerConstants {
	
	public static interface SupportedMime {
		public String getName();
		public String getMime();
	}
	
	
	public static enum SUPPORTED_MIMES implements SupportedMime {
		MIME_EPUB {

			@Override
			public String getName() {
				return "epub";
			}

			@Override
			public String getMime() {
				return "application/epub+zip".intern();
			}
			
			@Override
			public String toString() {
				return getMime();
			}
		},
		MIME_PDF {

			@Override
			public String getName() {
				return "pdf";
			}

			@Override
			public String getMime() {
				return "application/pdf".intern();
			}
			
			@Override
			public String toString() {
				return getMime();
			}			
		},
		MIME_CBZ {

			@Override
			public String getName() {
				return "cbz";
			}

			@Override
			public String getMime() {
				return "application/x-cbz".intern();
			}
			
			@Override
			public String toString() {
				return getMime();
			}			
			
		},
		MIME_CBR {

			@Override
			public String getName() {
				return "cbr";
			}

			@Override
			public String getMime() {
				return "application/x-cbr".intern();
			}
			
			@Override
			public String toString() {
				return getMime();
			}			
			
		},
		MIME_HTML {

			@Override
			public String getName() {
				return "html";
			}

			@Override
			public String getMime() {
				return "text/html".intern();
			}
			
			@Override
			public String toString() {
				return getMime();
			}			
			
		}				
	};
	
}