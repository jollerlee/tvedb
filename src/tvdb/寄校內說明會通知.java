package tvdb;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.tuple.Pair;

public class 寄校內說明會通知 {

	private static final String 舉辦地點 = "圖資五樓電腦教室二";
	private static final String 舉辦日期 = "9/10(四)2:30pm";

	public static void main(String[] args) throws IOException {
		Map<String, List<InternetAddress>> unitEmails = new HashMap<>();

		Utils.obtain單位email(unitEmails);

		Map<InternetAddress, List<String>> emails = unitEmails.entrySet().stream()
				.flatMap(e -> e.getValue().stream().map(email -> Pair.of(email, e.getKey())))
				.collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())));
		emails.entrySet().stream().forEach((entry) -> {
			doSendMessage(entry.getKey(), entry.getValue());
		});
		// doSendMessage(new InternetAddress("jollerlee@mail.ntin.edu.tw"),
		// Arrays.asList("資訊組", "圖書館"));
		return;
	}

	private static void doSendMessage(InternetAddress email, List<String> units) {
		Properties props = new Properties();
		props.put("mail.smtp.host", "mail.ntin.edu.tw");
		Session session = Session.getInstance(props, null);

		try {
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw", "臺南護專-資訊組", "big5"));
			msg.setRecipients(RecipientType.TO, new InternetAddress[] { email });
			msg.setSubject("[會議通知]技專資料庫-校內說明會", "big5");
			msg.setSentDate(new Date());
			msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");

			// create the message part
			MimeBodyPart messageBodyPart = new MimeBodyPart();

			// fill message
			messageBodyPart.setText("您好，\n\n" + "技專資料庫填報作業已經開始，\n" + 
					"資訊組將於 "+舉辦日期+" 於"+舉辦地點+"舉辦校內說明會，" + 
					"請您務必撥冗出席。\n" + 
					"依據資訊組的記錄，您負責的單位是" + units + "，\n" + 
					"如有錯誤，請與資訊組聯絡更正，謝謝!\n\n" + "--\n" + "臺南護專/資訊組 李仁豪 (分機 232)\n",
					"big5");

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
