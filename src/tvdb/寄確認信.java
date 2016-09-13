package tvdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

public class 寄確認信 {

	public static void main(String[] args) throws IOException {
		
		Map<String, List<InternetAddress>> unitEmails = new HashMap<>();
		
		Utils.obtain單位email(unitEmails);
		Utils.COMPRESSED_DIR.mkdir();
		
		File allUnitFolder = new File(Utils.TVDB_DIR, "單位");
		
		for(File unitFolder: allUnitFolder.listFiles()) {
			String unitName = unitFolder.getName();
			List<InternetAddress> recipients = unitEmails.get(unitName);
			if(recipients == null) {
				System.out.println("No email found for unit: "+unitName);
//				continue;
				break;
			}
			File zipFile = new File(Utils.COMPRESSED_DIR, unitName+".zip");
			if(!zipFile.exists()) {
				Utils.compress(unitFolder, zipFile);
			}
			
			try {
				doSendMessage(unitName, recipients, zipFile);
//				System.out.println(unitName+":"+unitEmails.get(unitName));
			} catch (MessagingException | UnsupportedEncodingException mex) {
				System.out.println(unitName + ": send failed, exception: " + mex);
			}
		}

	}

	private static void doSendMessage(String unitName, List<InternetAddress> recipients, File zipFile)
			throws MessagingException, IOException {

		Properties confProps = new Properties();
		try (Reader pr = new InputStreamReader(
				寄確認信.class.getResourceAsStream("寄確認信.properties"), "UTF-8")) {
			confProps.load(pr);
		}
		
		final String subject = confProps.getProperty("subject");
		final String content = confProps.getProperty("content");

		Properties props = new Properties();
		props.put("mail.smtp.host", "zimbra.ntin.edu.tw");
//		props.put("mail.from", "ntin-info-team <info-team@mail.ntin.edu.tw>");
//		System.setProperty("mail.mime.encodefilename", "true");
		Session session = Session.getInstance(props, null);

		MimeMessage msg = new MimeMessage(session);
//				msg.setFrom();
		msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw",
				"臺南護專-資訊組", "big5"));
		msg.setRecipients(RecipientType.TO, recipients.toArray(new InternetAddress[0]));
		msg.setSubject(String.format(subject, unitName), "big5");
		msg.setSentDate(new Date());
		msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");
		
		// create the message part
		MimeBodyPart messageBodyPart = new MimeBodyPart();

		// fill message
		messageBodyPart.setText(content, "big5");

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		// Part two is attachment
		messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(zipFile);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(MimeUtility.encodeText(zipFile.getName(), "big5", "B"));
		multipart.addBodyPart(messageBodyPart);

		// Put parts in message
		msg.setContent(multipart);

		// Send the message
		Transport.send(msg);
	}

}
