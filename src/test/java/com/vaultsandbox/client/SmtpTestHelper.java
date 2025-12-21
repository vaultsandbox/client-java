package com.vaultsandbox.client;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Properties;

/**
 * Helper class for sending test emails via SMTP. Used in integration tests to send real emails to
 * VaultSandbox inboxes.
 */
public class SmtpTestHelper {
  private final String host;
  private final int port;
  private final Session session;

  public SmtpTestHelper() {
    this(TestConfig.getSmtpHost(), TestConfig.getSmtpPort());
  }

  public SmtpTestHelper(String host, int port) {
    this.host = host;
    this.port = port;

    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", String.valueOf(port));
    props.put("mail.smtp.auth", "false");
    props.put("mail.smtp.starttls.enable", "false");
    props.put("mail.smtp.connectiontimeout", "10000");
    props.put("mail.smtp.timeout", "10000");

    this.session = Session.getInstance(props);
  }

  /**
   * Send a plain text email.
   *
   * @param to recipient email address
   * @param from sender email address
   * @param subject email subject
   * @param body email body text
   */
  public void sendEmail(String to, String from, String subject, String body)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);
    message.setText(body);

    Transport.send(message);
  }

  /**
   * Send an HTML email with both text and HTML parts.
   *
   * @param to recipient email address
   * @param from sender email address
   * @param subject email subject
   * @param text plain text body
   * @param html HTML body
   */
  public void sendHtmlEmail(String to, String from, String subject, String text, String html)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);

    // Create multipart/alternative
    MimeMultipart multipart = new MimeMultipart("alternative");

    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText(text, "utf-8");
    multipart.addBodyPart(textPart);

    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(html, "text/html; charset=utf-8");
    multipart.addBodyPart(htmlPart);

    message.setContent(multipart);

    Transport.send(message);
  }

  /**
   * Send an email with a verification code (common test scenario).
   *
   * @param to recipient email address
   * @param code the verification code
   */
  public void sendVerificationCode(String to, String code) throws MessagingException {
    String subject = "Your verification code is " + code;
    String body = "Your verification code is: " + code + "\n\nThis code expires in 10 minutes.";
    sendEmail(to, "noreply@vaultsandbox.com", subject, body);
  }

  /**
   * Send a welcome email (common test scenario).
   *
   * @param to recipient email address
   * @param username the user's name
   */
  public void sendWelcomeEmail(String to, String username) throws MessagingException {
    String subject = "Welcome to VaultSandbox, " + username + "!";
    String text = "Hello " + username + ",\n\nWelcome to VaultSandbox!\n\nBest regards,\nThe Team";
    String html =
        "<html><body><h1>Hello "
            + username
            + "!</h1><p>Welcome to VaultSandbox!</p><p>Best regards,<br>The Team</p></body></html>";
    sendHtmlEmail(to, "welcome@vaultsandbox.com", subject, text, html);
  }

  /**
   * Send multiple test emails to an inbox.
   *
   * @param to recipient email address
   * @param count number of emails to send
   */
  public void sendMultipleEmails(String to, int count) throws MessagingException {
    for (int i = 1; i <= count; i++) {
      String subject = "Test email #" + i;
      String body = "This is test email number " + i + " of " + count;
      sendEmail(to, "test" + i + "@vaultsandbox.com", subject, body);
    }
  }

  /**
   * Send an email with a single attachment.
   *
   * @param to recipient email address
   * @param from sender email address
   * @param subject email subject
   * @param body email body text
   * @param filename attachment filename
   * @param content attachment content
   * @param contentType attachment MIME type
   */
  public void sendEmailWithAttachment(
      String to,
      String from,
      String subject,
      String body,
      String filename,
      byte[] content,
      String contentType)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);

    // Create multipart/mixed
    MimeMultipart multipart = new MimeMultipart("mixed");

    // Text part
    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText(body, "utf-8");
    multipart.addBodyPart(textPart);

    // Attachment part using DataHandler for proper binary handling
    MimeBodyPart attachmentPart = new MimeBodyPart();
    attachmentPart.setDataHandler(
        new jakarta.activation.DataHandler(
            new jakarta.activation.DataSource() {
              @Override
              public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(content);
              }

              @Override
              public java.io.OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
              }

              @Override
              public String getContentType() {
                return contentType;
              }

              @Override
              public String getName() {
                return filename;
              }
            }));
    attachmentPart.setFileName(filename);
    multipart.addBodyPart(attachmentPart);

    message.setContent(multipart);

    Transport.send(message);
  }

  /**
   * Send an email with multiple attachments.
   *
   * @param to recipient email address
   * @param from sender email address
   * @param subject email subject
   * @param body email body text
   * @param attachments list of attachments, each containing filename, content, and contentType
   */
  public void sendEmailWithAttachments(
      String to,
      String from,
      String subject,
      String body,
      java.util.List<AttachmentInfo> attachments)
      throws MessagingException {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);

    // Create multipart/mixed
    MimeMultipart multipart = new MimeMultipart("mixed");

    // Text part
    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText(body, "utf-8");
    multipart.addBodyPart(textPart);

    // Attachment parts
    for (AttachmentInfo attachment : attachments) {
      MimeBodyPart attachmentPart = new MimeBodyPart();
      attachmentPart.setDataHandler(
          new jakarta.activation.DataHandler(
              new jakarta.activation.DataSource() {
                @Override
                public java.io.InputStream getInputStream() {
                  return new java.io.ByteArrayInputStream(attachment.content);
                }

                @Override
                public java.io.OutputStream getOutputStream() {
                  throw new UnsupportedOperationException();
                }

                @Override
                public String getContentType() {
                  return attachment.contentType;
                }

                @Override
                public String getName() {
                  return attachment.filename;
                }
              }));
      attachmentPart.setFileName(attachment.filename);
      multipart.addBodyPart(attachmentPart);
    }

    message.setContent(multipart);

    Transport.send(message);
  }

  /**
   * Send an HTML email with links embedded.
   *
   * @param to recipient email address
   * @param from sender email address
   * @param subject email subject
   * @param text plain text body
   * @param html HTML body containing links
   */
  public void sendHtmlEmailWithLinks(
      String to, String from, String subject, String text, String html) throws MessagingException {
    sendHtmlEmail(to, from, subject, text, html);
  }

  /** Simple container for attachment information. */
  public static class AttachmentInfo {
    public final String filename;
    public final byte[] content;
    public final String contentType;

    public AttachmentInfo(String filename, byte[] content, String contentType) {
      this.filename = filename;
      this.content = content;
      this.contentType = contentType;
    }
  }

  /** Check if SMTP is available for testing. */
  public static boolean isAvailable() {
    try {
      SmtpTestHelper helper = new SmtpTestHelper();
      Properties props = new Properties();
      props.put("mail.smtp.host", helper.host);
      props.put("mail.smtp.port", String.valueOf(helper.port));
      props.put("mail.smtp.connectiontimeout", "3000");
      props.put("mail.smtp.timeout", "3000");

      Session testSession = Session.getInstance(props);
      Transport transport = testSession.getTransport("smtp");
      transport.connect();
      transport.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
