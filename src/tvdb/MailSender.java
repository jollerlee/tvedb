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
				MimeMessage msg = new MimeMessage(session);
//				msg.setFrom();
				msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw",
						"臺南護專-資訊組", "big5"));
				msg.setRecipients(RecipientType.TO, recipients.toArray(new InternetAddress[0]));
				msg.setSubject("技專資料庫填報資料下載-"+unitName, "big5");
				msg.setSentDate(new Date());
				msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");
				
				// create the message part
				MimeBodyPart messageBodyPart = new MimeBodyPart();

				// fill message
				messageBodyPart.setText(
						"各位同仁，\n\n" +
						"技專資料庫第一階段填報作業已經結束，\n" +
						"即日起進入第二階段，主要工作為確認填報資料的正確性。\n" +
						"為協助同仁驗證資料，資訊組已將各單位所填報的資料下載，\n" +
						"在此分別寄送給各單位。\n" +
						"附件為一壓縮檔，解開後內有若干子目錄，其意義及處理方式分述如下：\n\n" +
						"    (一)表冊資料：請填表單位核對資料正確性。\n" +
						"    (二)無資料表冊：未填報之表冊，請確認是否確實無資料。\n" +
						"    (三)檢核疑義：其內檔案代表系統認為相關表冊資料有可疑之處；此項資訊可供填表單位當作確認表冊資料時參考之用。\n" +
						"    (四)總量管制表：請確認報表內的各總量正確。\n\n" +
						"若填報有錯誤或遺漏的情形，請填表單位通知資訊組開放修改表冊權限，\n" +
						"填表單位自行修正完成後再通知資訊組。\n\n" +
						"--\n" +
						"臺南護專/資訊組 李仁豪 (分機 232)\n", 
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
