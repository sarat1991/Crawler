package webcrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class TransmitData {

	static public String getResponse(String requestString) throws IOException {

		URL requestUrl = new URL(requestString);
		HttpURLConnection httpConnection = null;
		StringBuilder responseBuffer = new StringBuilder();
		InputStreamReader inputStream = null;
	
		
		httpConnection = (HttpURLConnection) requestUrl.openConnection();		
		
		if (httpConnection != null)
			httpConnection.setReadTimeout(6 * 1000);/* Timeout after 6 seconds */
		if (httpConnection != null && httpConnection.getInputStream() != null) {
			inputStream = new InputStreamReader(httpConnection.getInputStream(), Charset.defaultCharset());
			BufferedReader bufferedReader = new BufferedReader(inputStream);
			if (bufferedReader != null) {
				int cp;
				while ((cp = bufferedReader.read()) != -1) {
					responseBuffer.append((char) cp);
				}
				bufferedReader.close();
			}
		}
		
		if(inputStream != null)
			inputStream.close();

		return responseBuffer.toString();

	}

}
