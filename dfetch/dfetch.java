import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

class dfetch {
	private static final boolean VERBOSE = true;

	public static void main(String[] args) {
		if(args.length < 2) {
			usage();
		}

		try {
			String outputFilename = args[0];

			boolean isTargetGzipped = likelyGzipped(outputFilename);

			List<URL> urls = new LinkedList<URL>();

			boolean isSourceGzipped = true;
			for(int i=1;i<args.length;i++) {
				String urlPath = args[i];
				urls.add(new URL(urlPath));

				// if any are not gzipped, then we assume none need decompression
				if(false == likelyGzipped(urlPath))
					isSourceGzipped = false;
			}

			boolean shouldGunzip = shouldGunzip(isSourceGzipped, isTargetGzipped);

			fetch(outputFilename, urls, shouldGunzip);

		} catch (MalformedURLException muex) {
			muex.printStackTrace();
			System.exit(1);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(2);
		}
	}

	private static String fetch(String outputFilename, List<URL> urls, boolean shouldGunzip) 
		throws IOException {

		List<InputStream> streams = new LinkedList<InputStream>();
		InputStream in = null;
		OutputStream fout = null;
		try {
			for(URL u: urls) {
				URLConnection ucon = u.openConnection();
				streams.add(ucon.getInputStream());
			}
			in = new SequenceInputStream(Collections.enumeration(streams));
			if(shouldGunzip) {
				in = new GZIPInputStream(in);
			}
			if(outputFilename.equals("-")) {
				fout = System.out;
			} else {
				fout = new FileOutputStream(outputFilename);
			}

			long contentRead = 0;

			byte data[] = new byte[32768];
			int count;
			final int tickEvery = 1024 * 1024 ; // output '.' every 1 Mb
			long nextTick = tickEvery;
			while ((count = in.read(data, 0, 32768)) != -1) {
				fout.write(data, 0, count);
				contentRead += count;
				if(VERBOSE) {
					if(contentRead > nextTick) {
						//System.out.println("" + contentRead/(1024 * 1024) + "Mb");
						System.err.print(".");
						System.err.flush();
						nextTick += tickEvery;
					}
				}
			}

			if(VERBOSE) {
				System.err.print("\ndone " + outputFilename + " ");
				System.err.println(formatSize(contentRead));
			}
		} finally {
			if (in != null)
				in.close();
			if (fout != null)
				fout.close();
		}


		return null;
	}

	private static String formatSize(long bytes) {
		if(bytes < 1024) 
			return "" + bytes + "b";
		else if(bytes < (1024 * 1024))
			return "" + bytes / 1024 + "kb";
		else if(bytes < (1024 * 1024 * 1024))
			return "" + bytes / (1024 * 1024) + "mb";
		else
			return "" + bytes / (1024 * 1024 * 1024) + "gb";
	}

	/**
	 * If the source name is gzipped, and the target name is NOT, then we should decompress on the fly
	 */
	private static boolean shouldGunzip(boolean isSourceGzipped, boolean isTargetGzipped) {
		return(true == isSourceGzipped && false == isTargetGzipped);
	}

	private static boolean likelyGzipped(String path) {
		return(path.toLowerCase().contains(".gz"));
	}
	
	private static boolean likelyGzipped(URL url) {
		return(likelyGzipped(url.getFile()));
	}

	private static void usage() {
		System.out.println("Usage: " + dfetch.class.getName() + " <outputFilename|'-'> <md5hash|'-'|''> <url1> .. <urlN>");
		System.exit(0);
	}
	
}
