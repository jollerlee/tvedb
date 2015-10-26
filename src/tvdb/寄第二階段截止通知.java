package tvdb;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.tuple.Pair;

public class 寄第二階段截止通知 {

	public static void main(String[] args) throws IOException {
		
		Map<String, List<InternetAddress>> unitEmails = new HashMap<>();

		Utils.obtain單位email(unitEmails);

		Map<InternetAddress, List<String>> emails = unitEmails.entrySet().stream()
				.flatMap(e -> e.getValue().stream().map(email -> Pair.of(email, e.getKey())))
				.collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())));

		emails.entrySet().stream().forEach((entry) -> {
			try {
				doSendMessage(entry.getKey(), entry.getValue());
			} catch (Exception e1) {
				System.err.println("Error sending message to "+entry.getKey()+": "+e1.getMessage());
				e1.printStackTrace();
			}
		});

//		try {
//			doSendMessage(new InternetAddress("jollerlee@mail.ntin.edu.tw"), Arrays.asList("資訊組", "圖書館"));
//		} catch (AddressException e) {
//			e.printStackTrace();
//		}
		return;
	}

	private static void doSendMessage(InternetAddress email, List<String> units) throws IOException {
		Properties confProps = new Properties();
		try (Reader pr = new InputStreamReader(
				寄第二階段截止通知.class.getResourceAsStream("第二階段截止通知.properties"), "UTF-8")) {
			confProps.load(pr);
		}
		final String due = confProps.getProperty("due");
		final String subject = confProps.getProperty("subject");
		final String content = confProps.getProperty("content");

		Properties props = new Properties();
		props.put("mail.smtp.host", "mail.ntin.edu.tw");
		Session session = Session.getInstance(props, null);

		try {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw", "臺南護專-資訊組", "big5"));
			msg.setRecipients(RecipientType.TO, new InternetAddress[] { email });
			msg.setSubject(subject, "big5");
			msg.setSentDate(new Date());
			msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");

			// create the message part
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			// fill message
			messageBodyPart.setText(String.format(content, due), "big5");

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Put parts in message
			msg.setContent(multipart);

			// Send the message
			Transport.send(msg);
		} catch (MessagingException | UnsupportedEncodingException mex) {
			System.out.println(email.getAddress() + ": send failed, exception: " + mex);
		}

	}
}
