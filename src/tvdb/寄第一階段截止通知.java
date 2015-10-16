package tvdb;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
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

public class �H�Ĥ@���q�I��q�� {

	private static final String �I��ɶ� = "10��15(�|) 17:00";

	public static void main(String[] args) throws IOException {
		Map<String, List<InternetAddress>> unitEmails = new HashMap<>();

		Utils.obtain���email(unitEmails);

		Map<InternetAddress, List<String>> emails = unitEmails.entrySet().stream()
				.flatMap(e -> e.getValue().stream().map(email -> Pair.of(email, e.getKey())))
				.collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())));
		emails.entrySet().stream().forEach((entry) -> {
			doSendMessage(entry.getKey(), entry.getValue());
		});
//		doSendMessage(new InternetAddress("jollerlee@mail.ntin.edu.tw"),
//				Arrays.asList("��T��", "�Ϯ��]"));
		return;
	}

	private static void doSendMessage(InternetAddress email, List<String> units) {
		Properties props = new Properties();
		props.put("mail.smtp.host", "mail.ntin.edu.tw");
		Session session = Session.getInstance(props, null);

		try {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw", "�O�n�@�M-��T��", "big5"));
			msg.setRecipients(RecipientType.TO, new InternetAddress[] { email });
			msg.setSubject("�ޱM��Ʈw-�Ĥ@���q����@�~�Y�N�I��", "big5");
			msg.setSentDate(new Date());
			msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");

			// create the message part
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			// fill message
			messageBodyPart.setText("�z�n�A\n\n" + 
					"�ޱM��Ʈw�Ĥ@���q����@�~�Y�N�� "+�I��ɶ�+" �I��A\n" + 
					"�ЦU��P���ⴤ�ɶ��C\n" + 
					"�p���ðݡA�лP��T���p���A����!\n\n" + 
					"--\n" + 
					"�O�n�@�M/��T�� ������ (���� 232)\n", "big5");

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
