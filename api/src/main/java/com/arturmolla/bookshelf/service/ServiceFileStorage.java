package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.entity.EntityBookCover;
import com.arturmolla.bookshelf.model.entity.EntityBookPdf;
import com.arturmolla.bookshelf.model.entity.EntityUserProfilePic;
import com.arturmolla.bookshelf.model.entity.EntityUserWallpaper;
import com.arturmolla.bookshelf.repository.RepositoryBookCover;
import com.arturmolla.bookshelf.repository.RepositoryBookPdf;
import com.arturmolla.bookshelf.repository.RepositoryUserProfilePic;
import com.arturmolla.bookshelf.repository.RepositoryUserWallpaper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceFileStorage {

    /**
     * Files larger than this threshold will be compressed before storage.
     */
    private static final long COMPRESS_THRESHOLD_BYTES = 2 * 1024 * 1024; // 2 MB

    /**
     * Maximum dimension (width or height) after downscaling.
     */
    private static final int MAX_DIMENSION = 1200;

    /**
     * JPEG quality for compressed output (0.0 – 1.0).
     */
    private static final float JPEG_QUALITY = 0.85f;

    private final RepositoryBookCover repositoryBookCover;
    private final RepositoryBookPdf repositoryBookPdf;
    private final RepositoryUserProfilePic repositoryUserProfilePic;
    private final RepositoryUserWallpaper repositoryUserWallpaper;

    /**
     * Saves (or replaces) the cover image for the given book in the database.
     * Images larger than {@value #COMPRESS_THRESHOLD_BYTES} bytes are automatically
     * scaled down and re-encoded as JPEG before storage.
     *
     * @param file   the uploaded multipart file
     * @param bookId the id of the book this cover belongs to
     * @throws IllegalArgumentException if the file cannot be read or processed
     */
    @Transactional
    public void saveFile(MultipartFile file, Long bookId) {
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();

            if (bytes.length > COMPRESS_THRESHOLD_BYTES) {
                log.info("Cover for bookId={} is {} bytes — compressing", bookId, bytes.length);
                bytes = compressImage(bytes);
                contentType = "image/jpeg";
                log.info("Cover for bookId={} compressed to {} bytes", bookId, bytes.length);
            }

            EntityBookCover cover = repositoryBookCover.findByBookId(bookId)
                    .orElseGet(() -> EntityBookCover.builder().bookId(bookId).build());

            cover.setData(bytes);
            cover.setContentType(contentType);
            cover.setFileName(file.getOriginalFilename());
            cover.setFileSize((long) bytes.length);
            cover.setUploadedAt(LocalDateTime.now());

            repositoryBookCover.save(cover);
            log.info("Cover image saved to DB for bookId={}, final size={} bytes", bookId, bytes.length);
        } catch (IOException e) {
            log.error("Failed to process uploaded file: {}", e.getMessage());
            throw new IllegalArgumentException("Could not process uploaded file", e);
        }
    }

    /**
     * Retrieves the cover image bytes for the given book from the database.
     *
     * @param bookId the book id
     * @return the raw bytes, or {@code null} if no cover is stored
     */
    public byte[] loadFile(Long bookId) {
        return repositoryBookCover.findByBookId(bookId)
                .map(EntityBookCover::getData)
                .orElse(null);
    }

    /**
     * Saves (or replaces) the PDF for the given book in the database.
     *
     * @param file   the uploaded multipart PDF file
     * @param bookId the id of the book this PDF belongs to
     */
    @Transactional
    public void savePdf(MultipartFile file, Long bookId) {
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";

            EntityBookPdf pdf = repositoryBookPdf.findByBookId(bookId)
                    .orElseGet(() -> EntityBookPdf.builder().bookId(bookId).build());

            pdf.setData(bytes);
            pdf.setContentType(contentType);
            pdf.setFileName(file.getOriginalFilename());
            pdf.setFileSize((long) bytes.length);
            pdf.setUploadedAt(java.time.LocalDateTime.now());

            repositoryBookPdf.save(pdf);
            log.info("PDF saved to DB for bookId={}, size={} bytes", bookId, bytes.length);
        } catch (IOException e) {
            log.error("Failed to process uploaded PDF: {}", e.getMessage());
            throw new IllegalArgumentException("Could not process uploaded PDF file", e);
        }
    }

    /**
     * Retrieves the PDF bytes for the given book from the database.
     *
     * @param bookId the book id
     * @return the raw bytes, or {@code null} if no PDF is stored
     */
    public byte[] loadPdf(Long bookId) {
        return repositoryBookPdf.findByBookId(bookId)
                .map(EntityBookPdf::getData)
                .orElse(null);
    }

    /**
     * Returns whether a PDF has been uploaded for the given book.
     *
     * @param bookId the book id
     * @return true if a PDF exists
     */
    public boolean hasPdf(Long bookId) {
        return repositoryBookPdf.existsByBookId(bookId);
    }

    // -------------------------------------------------------------------------
    // User profile picture
    // -------------------------------------------------------------------------

    /**
     * Saves (or replaces) the profile picture for the given user in the database.
     * Images larger than {@value #COMPRESS_THRESHOLD_BYTES} bytes are automatically
     * scaled down and re-encoded as JPEG before storage.
     */
    @Transactional
    public void saveProfilePic(MultipartFile file, Long userId) {
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();

            if (bytes.length > COMPRESS_THRESHOLD_BYTES) {
                log.info("Profile pic for userId={} is {} bytes — compressing", userId, bytes.length);
                bytes = compressImage(bytes);
                contentType = "image/jpeg";
                log.info("Profile pic for userId={} compressed to {} bytes", userId, bytes.length);
            }

            EntityUserProfilePic pic = repositoryUserProfilePic.findByUserId(userId)
                    .orElseGet(() -> EntityUserProfilePic.builder().userId(userId).build());

            pic.setData(bytes);
            pic.setContentType(contentType);
            pic.setFileName(file.getOriginalFilename());
            pic.setFileSize((long) bytes.length);
            pic.setUploadedAt(LocalDateTime.now());

            repositoryUserProfilePic.save(pic);
            log.info("Profile pic saved to DB for userId={}, final size={} bytes", userId, bytes.length);
        } catch (IOException e) {
            log.error("Failed to process uploaded profile pic: {}", e.getMessage());
            throw new IllegalArgumentException("Could not process uploaded profile picture", e);
        }
    }

    /**
     * Retrieves the raw profile picture bytes for the given user.
     *
     * @param userId the user id
     * @return the raw bytes, or {@code null} if no picture is stored
     */
    public byte[] loadProfilePic(Long userId) {
        return repositoryUserProfilePic.findByUserId(userId)
                .map(EntityUserProfilePic::getData)
                .orElse(null);
    }

    /**
     * Returns the content type of the stored profile picture, defaulting to {@code image/jpeg}.
     */
    public String getProfilePicContentType(Long userId) {
        return repositoryUserProfilePic.findByUserId(userId)
                .map(p -> p.getContentType() != null ? p.getContentType() : "image/jpeg")
                .orElse("image/jpeg");
    }

    /**
     * Returns whether a profile picture has been uploaded for the given user.
     */
    public boolean hasProfilePic(Long userId) {
        return repositoryUserProfilePic.existsByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // User wallpaper
    // -------------------------------------------------------------------------

    /**
     * Saves (or replaces) the wallpaper for the given user in the database.
     * Images larger than {@value #COMPRESS_THRESHOLD_BYTES} bytes are automatically
     * scaled down and re-encoded as JPEG before storage.
     */
    @Transactional
    public void saveWallpaper(MultipartFile file, Long userId) {
        try {
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType();

            if (bytes.length > COMPRESS_THRESHOLD_BYTES) {
                log.info("Wallpaper for userId={} is {} bytes — compressing", userId, bytes.length);
                bytes = compressImage(bytes);
                contentType = "image/jpeg";
                log.info("Wallpaper for userId={} compressed to {} bytes", userId, bytes.length);
            }

            EntityUserWallpaper wallpaper = repositoryUserWallpaper.findByUserId(userId)
                    .orElseGet(() -> EntityUserWallpaper.builder().userId(userId).build());

            wallpaper.setData(bytes);
            wallpaper.setContentType(contentType);
            wallpaper.setFileName(file.getOriginalFilename());
            wallpaper.setFileSize((long) bytes.length);
            wallpaper.setUploadedAt(LocalDateTime.now());

            repositoryUserWallpaper.save(wallpaper);
            log.info("Wallpaper saved to DB for userId={}, final size={} bytes", userId, bytes.length);
        } catch (IOException e) {
            log.error("Failed to process uploaded wallpaper: {}", e.getMessage());
            throw new IllegalArgumentException("Could not process uploaded wallpaper", e);
        }
    }

    /**
     * Retrieves the raw wallpaper bytes for the given user.
     *
     * @param userId the user id
     * @return the raw bytes, or {@code null} if no wallpaper is stored
     */
    public byte[] loadWallpaper(Long userId) {
        return repositoryUserWallpaper.findByUserId(userId)
                .map(EntityUserWallpaper::getData)
                .orElse(null);
    }

    /**
     * Returns the content type of the stored wallpaper, defaulting to {@code image/jpeg}.
     */
    public String getWallpaperContentType(Long userId) {
        return repositoryUserWallpaper.findByUserId(userId)
                .map(w -> w.getContentType() != null ? w.getContentType() : "image/jpeg")
                .orElse("image/jpeg");
    }

    /**
     * Returns whether a wallpaper has been uploaded for the given user.
     */
    public boolean hasWallpaper(Long userId) {
        return repositoryUserWallpaper.existsByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Scales the image so its longest side is at most {@value #MAX_DIMENSION} px,
     * then re-encodes it as JPEG at {@value #JPEG_QUALITY} quality.
     * <p>
     * Package-visible so that other services (e.g. {@code ServiceHomePost}) can
     * reuse this logic without duplicating code.
     */
    byte[] compressImageBytes(byte[] originalBytes) throws IOException {
        return compressImage(originalBytes);
    }

    private byte[] compressImage(byte[] originalBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (original == null) {
            throw new IllegalArgumentException("Uploaded file is not a readable image");
        }

        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        // Calculate target dimensions preserving aspect ratio
        int targetWidth;
        int targetHeight;
        if (origWidth >= origHeight) {
            targetWidth = Math.min(origWidth, MAX_DIMENSION);
            targetHeight = (int) Math.round((double) origHeight / origWidth * targetWidth);
        } else {
            targetHeight = Math.min(origHeight, MAX_DIMENSION);
            targetWidth = (int) Math.round((double) origWidth / origHeight * targetHeight);
        }

        // Draw scaled image
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        // Encode as JPEG with controlled quality
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter found");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(scaled, null, null), param);
        } finally {
            writer.dispose();
        }

        return out.toByteArray();
    }
}
