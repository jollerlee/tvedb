package tvdb;

import java.io.File;
import java.io.IOException;
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

public class MailSender {

	public static void main(String[] args) throws IOException {
		Properties props = new Properties();
		props.put("mail.smtp.host", "mail.ntin.edu.tw");
//		props.put("mail.from", "ntin-info-team <info-team@mail.ntin.edu.tw>");
//		System.setProperty("mail.mime.encodefilename", "true");
		Session session = Session.getInstance(props, null);
		
		Map<String, List<InternetAddress>> unitEmails = new HashMap<String, List<InternetAddress>>();
		
		Utils.obtain���email(unitEmails);
		Utils.COMPRESSED_DIR.mkdir();
		
		File allUnitFolder = new File(Utils.TVDB_DIR, "���");
		
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
				MimeMessage msg = new MimeMessage(session);
//				msg.setFrom();
				msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw",
						"�O�n�@�M-��T��", "big5"));
				msg.setRecipients(RecipientType.TO, recipients.toArray(new InternetAddress[0]));
				msg.setSubject("�ޱM��Ʈw�����ƤU��-"+unitName, "big5");
				msg.setSentDate(new Date());
				msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");
				
				// create the message part
				MimeBodyPart messageBodyPart = new MimeBodyPart();

				// fill message
				messageBodyPart.setText(
						"�U��P���A\n\n" +
						"�ޱM��Ʈw�Ĥ@���q����@�~�w�g�����A\n" +
						"�Y��_�i�J�ĤG���q�A�D�n�u�@���T�{�����ƪ����T�ʡC\n" +
						"����U�P�����Ҹ�ơA��T�դw�N�U���Ҷ������ƤU���A\n" +
						"�b�����O�H�e���U���C\n" +
						"���󬰤@���Y�ɡA�Ѷ}�᤺���Y�z�l�ؿ��A��N�q�γB�z�覡���z�p�U�G\n\n" +
						"    (�@)��U��ơG�ж����ֹ��ƥ��T�ʡC\n" +
						"    (�G)�L��ƪ�U�G���������U�A�нT�{�O�_�T��L��ơC\n" +
						"    (�T)�ˮֺøq�G�䤺�ɮץN��t�λ{��������U��Ʀ��i�ä��B�F������T�i�Ѷ�����@�T�{��U��ƮɰѦҤ��ΡC\n" +
						"    (�|)�`�q�ި��G�нT�{�������U�`�q���T�C\n\n" +
						"�Y��������~�ο�|�����ΡA�ж����q����T�ն}��ק��U�v���A\n" +
						"�����ۦ�ץ�������A�q����T�աC\n\n" +
						"--\n" +
						"�O�n�@�M/��T�� ������ (���� 232)\n", 
						"big5");

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
			} catch (MessagingException | UnsupportedEncodingException mex) {
				System.out.println(unitName + ": send failed, exception: " + mex);
			}
		}

	}

}
