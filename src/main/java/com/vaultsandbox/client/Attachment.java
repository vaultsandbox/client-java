package com.vaultsandbox.client;

import com.vaultsandbox.client.model.AttachmentData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Represents an email attachment.
 *
 * <p>Attachments are accessed via {@link Email#getAttachments()} and provide methods to retrieve
 * the attachment content and metadata.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * for (Attachment attachment : email.getAttachments()) {
 *     System.out.println("File: " + attachment.getFilename());
 *     System.out.println("Type: " + attachment.getContentType());
 *     System.out.println("Size: " + attachment.getSize() + " bytes");
 *
 *     // Save to disk
 *     attachment.saveTo(Path.of("/tmp/" + attachment.getFilename()));
 *
 *     // Or get raw bytes
 *     byte[] content = attachment.getContent();
 * }
 * }</pre>
 *
 * @see Email#getAttachments()
 */
public class Attachment {
  private final String filename;
  private final String contentType;
  private final int size;
  private final byte[] content;

  Attachment(AttachmentData data) {
    this.filename = data.getFilename();
    this.contentType = data.getContentType();
    this.size = data.getSize();
    String b64 = data.getContentB64();
    this.content = b64 != null ? Base64.getDecoder().decode(b64) : null;
  }

  /**
   * Returns the filename of the attachment.
   *
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Returns the MIME content type of the attachment.
   *
   * @return the content type (e.g., "application/pdf", "image/png")
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the size of the attachment in bytes.
   *
   * @return the size in bytes
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns a copy of the attachment content.
   *
   * <p>Returns a defensive copy of the content bytes to prevent modification. May return {@code
   * null} if the attachment content was not fetched (e.g., when listing emails without full
   * content).
   *
   * @return the attachment content as a byte array, or {@code null} if not available
   */
  public byte[] getContent() {
    return content != null ? Arrays.copyOf(content, content.length) : null;
  }

  /**
   * Saves the attachment content to a file.
   *
   * @param path the file path to save to
   * @throws IOException if an I/O error occurs writing the file
   * @throws IllegalStateException if the attachment content is not available
   */
  public void saveTo(Path path) throws IOException {
    if (content == null) {
      throw new IllegalStateException("Attachment content is not available");
    }
    Files.write(path, content);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Attachment that = (Attachment) o;
    return size == that.size
        && Objects.equals(filename, that.filename)
        && Objects.equals(contentType, that.contentType)
        && Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(filename, contentType, size);
    result = 31 * result + Arrays.hashCode(content);
    return result;
  }
}
