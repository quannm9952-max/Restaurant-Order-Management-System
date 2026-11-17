package com.restaurant.service;

import com.restaurant.exception.BusinessException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.support.MenuImages;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MenuImageService {
  private static final Logger LOG = LoggerFactory
      .getLogger(MenuImageService.class);
  private static final long MAX_BYTES = 5 * 1024 * 1024;
  private final Path directory;

  public MenuImageService(
      @Value("${menu.images.directory:../uploads/menu}") String directory) {
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  public byte[] validate(MultipartFile file) {
    if (file == null || (file.isEmpty() && (file.getOriginalFilename() == null
        || file.getOriginalFilename().isBlank())))
      return null;
    if (file.isEmpty() || file.getSize() > MAX_BYTES)
      throw new IllegalArgumentException(
          "Choose a JPG or PNG image between 1 byte and 5 MB.");
    try {
      byte[] bytes = file.getBytes();
      try (var stream = ImageIO
          .createImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext())
          throw new IllegalArgumentException(
              "Choose a valid JPG or PNG image.");
        var reader = readers.next();
        try {
          reader.setInput(stream);
          String format = reader.getFormatName().toLowerCase(Locale.ROOT);
          if (!format.equals("jpeg") && !format.equals("png"))
            throw new IllegalArgumentException(
                "Only JPG and PNG images are supported.");
          int width = reader.getWidth(0), height = reader.getHeight(0);
          if (width < 1 || height < 1 || width > 8000 || height > 8000
              || (long) width * height > 12_000_000)
            throw new IllegalArgumentException(
                "Image must be at most 8000 pixels per side and 12 megapixels.");
          reader.read(0).flush();
        } finally {
          reader.dispose();
        }
      }
      return bytes;
    } catch (IOException ex) {
      throw new IllegalArgumentException(
          "The image cannot be read. Choose another JPG or PNG file.");
    }
  }

  public void save(long id, byte[] bytes) {
    if (bytes == null)
      return;
    Path file = file(id);
    try {
      byte[] previous = Files.isRegularFile(file)
          ? Files.readAllBytes(file)
          : null;
      if (TransactionSynchronizationManager.isSynchronizationActive())
        TransactionSynchronizationManager
            .registerSynchronization(new TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED)
                  return;
                try {
                  if (previous == null)
                    Files.deleteIfExists(file);
                  else
                    write(file, previous);
                } catch (IOException ex) {
                  LOG.error("Unable to restore menu photo {} after rollback",
                      id, ex);
                }
              }
            });
      write(file, bytes);
    } catch (IOException ex) {
      LOG.error("Unable to save menu photo {}", id, ex);
      throw new BusinessException(
          "Unable to save the image. Please try again.");
    }
  }

  private void write(Path destination, byte[] bytes) throws IOException {
    Files.createDirectories(directory);
    Path temporary = Files.createTempFile(directory, "photo-", ".tmp");
    try {
      Files.write(temporary, bytes);
      try {
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private Path file(long id) {
    if (id < 1)
      throw new NotFoundException("Menu photo not found.");
    return directory.resolve(id + ".image");
  }

  public String url(long id, String name) {
    Path file = file(id);
    try {
      if (Files.isRegularFile(file))
        return "/images/menu/uploads/" + id + "?v="
            + Files.getLastModifiedTime(file).toMillis();
    } catch (IOException ex) {
      LOG.warn("Unable to inspect menu photo {}", id, ex);
    }
    return MenuImages.forItemName(name);
  }

  public Photo read(long id) {
    try {
      byte[] bytes = Files.readAllBytes(file(id));
      String type = bytes.length > 0 && bytes[0] == (byte) 0x89
          ? "image/png"
          : "image/jpeg";
      return new Photo(bytes, type);
    } catch (IOException ex) {
      throw new NotFoundException("Menu photo not found.");
    }
  }

  public record Photo(byte[] bytes, String contentType) {
  }
}
