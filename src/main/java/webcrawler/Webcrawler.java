package webcrawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Webcrawler {

	static Document doc;
	private final static Logger LOGGER = Logger.getLogger(Webcrawler.class.getName());

	// args[0] --> year
	public static void main(String[] args) {

		
		try {
			LOGGER.addHandler(new FileHandler("Logging.txt"));
			LOGGER.setUseParentHandlers(false);
		} catch (SecurityException securityExcep) {

			securityExcep.printStackTrace();
		} catch (IOException ioExcep) {

			ioExcep.printStackTrace();
		}

		String year = null;

		Properties prop = new Properties();
		InputStream input;
		try {
			input = Thread.currentThread().getContextClassLoader().getResourceAsStream("app.properties");
			prop.load(input);

			year = (args.length == 1) ? args[0] : prop.getProperty("year");

		} catch (FileNotFoundException fileNotFoundExcep) {

			fileNotFoundExcep.printStackTrace();
		} catch (IOException ioExcep) {

			ioExcep.printStackTrace();
		}

		File yearZip = new File(year + ".zip");

		if (!yearZip.exists()) {
			try {
				yearZip.createNewFile();

			} catch (IOException e) {

				LOGGER.log(Level.SEVERE, "Cannot create a file : " + year + ".zip.", e);
			}
		}

		/* Creating Buddy File */

		File buddyFile = new File("BuddyFile.xml");

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		Document buddyDoc = null;
		Element filesNode = null;
		Element monthRootTag = null;
		int monthCounter = 0;
		int messageCounterStartIndex = 0;
		boolean resumeDownload = false;

		try {
			dBuilder = dbFactory.newDocumentBuilder();

			if (buddyFile.exists()) {
				buddyDoc = dBuilder.parse(buddyFile);

				buddyDoc.normalize();
				filesNode = buddyDoc.getDocumentElement();

				if (filesNode.getChildNodes().getLength() > 0) {
					String monthName = filesNode.getLastChild().getAttributes().getNamedItem("name").getNodeValue();
					monthCounter = Utility.toMonthId(monthName);
					messageCounterStartIndex = filesNode.getLastChild().getChildNodes().getLength() - 1;
					monthRootTag = (Element) filesNode.getLastChild();
					resumeDownload = true;
				}

			} else {
				try {
					createBuddyDTD();
					buddyFile.createNewFile();
					buddyDoc = dBuilder.newDocument();
					filesNode = buddyDoc.createElement("Files");
					buddyDoc.appendChild(filesNode);
					monthCounter = 1;

				} catch (IOException ioExcep) {

					LOGGER.log(Level.SEVERE, "Cannot create the temporary files", ioExcep);
				}
			}

			// root element

		} catch (ParserConfigurationException e1) {

			LOGGER.log(Level.SEVERE, "Internal error", e1);
		} catch (SAXException e1) {

			LOGGER.log(Level.SEVERE, "BuddyFile.xml is modified. Delete all the files and restart the download", e1);
		} catch (IOException e1) {

			LOGGER.log(Level.SEVERE, "Cannot read the BuddyFile. Check if the BuddyFile is present or not", e1);
		}

		ZipOutputStream zos = null;
		BufferedWriter bufferedWriter = null;
		try {

			zos = new ZipOutputStream(new FileOutputStream(yearZip));
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));

			String baseUrl = "http://mail-archives.apache.org/mod_mbox/maven-users/";

			for (; monthCounter <= 12; monthCounter++) { // change it to 12

				StringBuilder monthUrl = urlForMonth(baseUrl, monthCounter, year);
				String monthUrl_base = monthUrl.toString();

				monthUrl.append("thread?0");

				String monthResponse = null;

				monthResponse = TransmitData.getResponse(monthUrl.toString());

				Document monthMessageXml = Utility.stringToXmlDocument(monthResponse);

				NodeList messages = monthMessageXml.getElementsByTagName("message");

				/* Month tag in buddyFile */
				if (!resumeDownload) {
					String monthName = Utility.toMonthName(monthCounter);
					monthRootTag = buddyDoc.createElement("month");
					monthRootTag.setAttribute("name", monthName);
					filesNode.appendChild(monthRootTag);
				}

				bufferedWriter = messageParsingAndDownload(messageCounterStartIndex, monthUrl_base, messages, monthCounter, zos, bufferedWriter,
						buddyDoc, monthRootTag, filesNode);
				messageCounterStartIndex = 0;
				resumeDownload = false;
				LOGGER.log(Level.INFO, "Mails are downloading. Things are in good shape");
			}
		} catch (IOException ie) {
			LOGGER.log(Level.SEVERE, "Network issue", ie);
		} finally {
			try {

				TransformerFactory transformerFactory = TransformerFactory.newInstance();

				Transformer transformer = transformerFactory.newTransformer();

				transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "BuddyFile.dtd");

				DOMSource source = new DOMSource(buddyDoc);

				StreamResult result = new StreamResult(buddyFile);

				transformer.transform(source, result);
				if (null != bufferedWriter)
					bufferedWriter.close();
			} catch (IOException e) {

				LOGGER.log(Level.SEVERE, "Internal error", e);
			} catch (TransformerConfigurationException e) {

				LOGGER.log(Level.SEVERE, "Internal error", e);
			} catch (TransformerException e) {

				LOGGER.log(Level.SEVERE, "Internal error", e);
			}
		}

	}

	private static StringBuilder urlForMonth(String baseURL, int monthID, String year) {

		StringBuilder homeURL = new StringBuilder(baseURL);

		String monthString = (monthID <= 9) ? "0" + monthID : "" + monthID;
		homeURL.append(year + monthString);
		homeURL.append(".mbox/ajax/");

		return homeURL;

	}

	private static String[] getmessageContent(String mailResponse) {

		String[] messageContent = new String[4];

		Document mail = Utility.stringToXmlDocument(mailResponse);

		NodeList mailChildren = mail.getFirstChild().getChildNodes();

		messageContent[0] = mailChildren.item(1).getTextContent(); // fromContent
		messageContent[1] = mailChildren.item(3).getTextContent(); // subject
		messageContent[2] = mailChildren.item(5).getTextContent(); // dateTime
		messageContent[3] = mailChildren.item(7).getTextContent(); // contents

		return messageContent;

	}

	private static BufferedWriter downloadFile(String[] messageContents, ZipOutputStream zipOutStream, String zipEntryName,
			BufferedWriter bufferedWriter) {

		try {

			
			zipOutStream.putNextEntry(new ZipEntry(zipEntryName));

			bufferedWriter.write("From:");
			bufferedWriter.newLine();
			bufferedWriter.write("    " + HtmlManipulator.replaceHtmlEntities(messageContents[0]));
			bufferedWriter.newLine();

			bufferedWriter.write("Subject:");
			bufferedWriter.newLine();
			bufferedWriter.write("    " + messageContents[1]);
			bufferedWriter.newLine();

			bufferedWriter.write("Date And Time:");
			bufferedWriter.newLine();
			bufferedWriter.write("    " + messageContents[2]);
			bufferedWriter.newLine();

			bufferedWriter.write("Contents:");
			bufferedWriter.newLine();
			bufferedWriter.write("    " + HtmlManipulator.replaceHtmlEntities(messageContents[3]));
			bufferedWriter.newLine();

			bufferedWriter.flush();
			zipOutStream.closeEntry();

		} catch (IOException ioExcep) {
			
			ioExcep.printStackTrace();
		}

		return bufferedWriter;
	}

	private static void writeToBuddyFile(String fileName, Document buddyFile, Element rootTag) {

		
		Element fName = buddyFile.createElement("fname");
		fName.appendChild(buddyFile.createTextNode(fileName));
		rootTag.appendChild(fName);

	}

	private static void createBuddyDTD() throws IOException {

		File buddyDTD = new File("BuddyFile.dtd");
		if (!buddyDTD.exists()) {

			buddyDTD.createNewFile();

		}

		FileOutputStream fop = null;

		fop = new FileOutputStream(buddyDTD);

		// if file doesnt exists, then create it

		String content = "<!ELEMENT Files (month*)>\r<!ELEMENT month (fname*)>\r<!ATTLIST month name CDATA \"null\">\r<!ELEMENT fname (#PCDATA)>";

		// get the content in bytes
		byte[] contentInBytes = content.getBytes();

		fop.write(contentInBytes);

		fop.flush();
		fop.close();

	}

	private static BufferedWriter messageParsingAndDownload(int messageCounterStartIndex, String monthUrl_base,
			NodeList messages, int monthCounter, ZipOutputStream zipOutStream, BufferedWriter bufferedWriter, Document buddyDoc,
			Element monthRootTag, Element filesNode) throws IOException {

		int prevFilecount = 1;
		String prevFileName = null;
		for (int messageCounter = messageCounterStartIndex; messageCounter < messages.getLength(); messageCounter++) { // Change
			// to
			StringBuilder tempString = new StringBuilder(monthUrl_base);
			String id = messages.item(messageCounter).getAttributes().getNamedItem("id").toString().replace("id=", "")
					.replace("\"", "");
			String encodedURI = Utility.encodeURI(id);
			String mailRequest = tempString.append(encodedURI).toString();

			String mailResponse = TransmitData.getResponse(mailRequest);

			String[] messageContent = getmessageContent(mailResponse);

			String zipEntryName = "" + monthCounter + "/" + messageContent[2] + ".txt";
			if (zipEntryName.equals(prevFileName)) {
				zipEntryName = "" + monthCounter + "/" + messageContent[2] + "(" + prevFilecount + ")" + ".txt";
				prevFilecount++;
			} else {
				prevFileName = zipEntryName;
				prevFilecount = 1;
			}

			bufferedWriter = downloadFile(messageContent, zipOutStream, zipEntryName, bufferedWriter);
			writeToBuddyFile(zipEntryName, buddyDoc, monthRootTag);

			filesNode.appendChild(monthRootTag);

		}

		return bufferedWriter;

	}

}
