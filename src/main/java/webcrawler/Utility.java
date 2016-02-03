package webcrawler;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Utility {

	static String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

	public static Document stringToXmlDocument(String xmlString) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String encodeURI(String uri) {
		String result;
		try {
			result = URLEncoder.encode(uri, "UTF-8");

		} catch (UnsupportedEncodingException e) {
			result = uri;
		}

		return result;
	}

	public static String toMonthName(int monthId) {
		return months[monthId - 1];
	}

	public static int toMonthId(String monthName) {

		for (int i = 0; i < months.length; i++) {
			if (months[i].equals(monthName))
				return ++i;
		}
		return -1;
	}
}
