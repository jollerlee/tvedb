package tvdb;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;

public class 寄信通知 {

    public static void main(String[] args) throws IOException {
        Reflections reflections = new Reflections(寄信通知.class.getPackage().getName(), new ResourcesScanner());

        /*
         * Find all resources coming in email-xxx.properties form. When
         * searching, the package part is excluding for matching. But the result
         * resource list comes with names including the package part.
         */
        String[] allPropertiesFiles = reflections.getResources(Pattern.compile("email-.*\\.properties")).toArray(new String[0]);

        Pattern pat = Pattern.compile(".*/email-(.*)\\.properties");

        for (int i = 0; i < allPropertiesFiles.length; i++) {
            Matcher m = pat.matcher(allPropertiesFiles[i]);
            if (m.matches())
                System.out.println("(" + i + ") " + m.group(1));
        }

        /*
         * Let the use choose the email template to use.
         */
        int choice;
        while (true) {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line = br.readLine();
            try {
                choice = Integer.parseInt(line);
                if (choice >= 0 && choice < allPropertiesFiles.length) {
                    break;
                }
                System.err.println("Illegal choice.");
            } catch (NumberFormatException e) {
                System.err.println("Please enter a number.");
            }
        }

        Map<String, List<InternetAddress>> unitEmails = new HashMap<>();

        Utils.obtain單位email(unitEmails);
        
        final String emailTemplate = allPropertiesFiles[choice];

        Map<InternetAddress, List<String>> emails = unitEmails.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(email -> Pair.of(email, e.getKey())))
                .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, toList())));
        
        emails.entrySet().stream().forEach((entry) -> {
            doSendMessage(entry.getKey(), entry.getValue(), emailTemplate);
        });

        // try {
        // doSendMessage(new InternetAddress("jollerlee@mail.ntin.edu.tw"),
        // Arrays.asList("資訊組", "圖書館"),
        // emailTemplate);
        // } catch (Exception e1) {
        // e1.printStackTrace();
        // }
        return;
    }

    private static void doSendMessage(InternetAddress email, List<String> units, String templateName) {
        Properties confProps = new Properties();
        try (Reader pr = new InputStreamReader(寄信通知.class.getClassLoader().getResourceAsStream(templateName),
                "UTF-8")) {
            confProps.load(pr);
        } catch (IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Map<String, Object> formatValues = new HashMap<>();

        final String subject = confProps.getProperty("subject");
        final String content = confProps.getProperty("content");
        formatValues.put("place", confProps.getProperty("place"));
        formatValues.put("time", confProps.getProperty("time"));
        formatValues.put("units", units);

        Properties props = new Properties();
        props.put("mail.smtp.host", "zimbra.ntin.edu.tw");
        Session session = Session.getInstance(props, null);

        try {
            StrSubstitutor sub = new StrSubstitutor(formatValues, "%(", ")");
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("info-team@mail.ntin.edu.tw", "臺南護專-資訊組", "utf-8"));
            msg.setRecipients(RecipientType.TO, new InternetAddress[] { email });
            msg.setSubject(sub.replace(subject), "utf-8");
            msg.setSentDate(new Date());
            msg.addHeader("Return-Receipt-To", "info-team@mail.ntin.edu.tw");

            // create the message part
            MimeBodyPart messageBodyPart = new MimeBodyPart();

            // fill message
            messageBodyPart.setText(sub.replace(content), "utf-8");

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
