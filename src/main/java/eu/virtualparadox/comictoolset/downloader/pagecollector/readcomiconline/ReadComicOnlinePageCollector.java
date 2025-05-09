package eu.virtualparadox.comictoolset.downloader.pagecollector.readcomiconline;

import eu.virtualparadox.comictoolset.downloader.ImageDownloader;
import eu.virtualparadox.comictoolset.downloader.ImageFormatDetector;
import eu.virtualparadox.comictoolset.downloader.issuecollector.Issue;
import eu.virtualparadox.comictoolset.downloader.pagecollector.Page;
import eu.virtualparadox.comictoolset.downloader.pagecollector.PageCollector;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * PageCollector implementation for the ReadComicOnline website.
 * <p>
 * This collector loads the issue's full-read page using Selenium in headless Chrome,
 * scrolls through the page to trigger lazy-loading of comic images,
 * and downloads the image data with detected format.
 * </p>
 */
public class ReadComicOnlinePageCollector implements PageCollector {

    private static final String ALL_PAGE_READ_TYPE = "&readType=1";
    private static final int MAX_NO_PROGRESS_TRIES = 5;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    private final Logger logger = LoggerFactory.getLogger(ReadComicOnlinePageCollector.class);

    private final ImageDownloader imageDownloader;
    private final ImageFormatDetector imageFormatDetector;

    /**
     * Constructs a new ReadComicOnlinePageCollector with internal image downloader and format detector.
     */
    public ReadComicOnlinePageCollector() {
        imageDownloader = new ImageDownloader();
        imageFormatDetector = new ImageFormatDetector();
    }

    /**
     * Collects all pages of a comic issue by simulating browser behavior and downloading all images.
     *
     * @param issue the comic issue to process
     * @return list of {@link Page} objects representing each page image
     * @throws IOException if image download fails
     */
    @Override
    public List<Page> collectPages(final Issue issue) throws IOException {
        final String url = issue.getUrl() + ALL_PAGE_READ_TYPE;
        final Set<String> imageUrls = emulateScrollAndCollectImages(url);
        return downloadImages(imageUrls);
    }

    /**
     * Downloads images from the provided image URLs and wraps them into {@link Page} objects.
     *
     * @param imageUrls the URLs to fetch
     * @return a list of page objects with content and format
     * @throws IOException if reading any image fails
     */
    private List<Page> downloadImages(final Set<String> imageUrls) throws IOException {
        final List<Page> images = new ArrayList<>();
        logger.info("Downloading images...");
        int id = 0;
        for (String url : imageUrls) {
            logger.debug("{} / {}", id, imageUrls.size());
            final byte[] imageData = imageDownloader.readImageBytes(url);
            final String format = imageFormatDetector.detectFormatFromHeader(imageData);
            final Page image = new Page(id, url, imageData, format);
            images.add(image);
            id++;
        }
        logger.info("Done.");
        return images;
    }

    /**
     * Emulates scrolling to the bottom of the ReadComicOnline page to trigger lazy-loading of all images.
     *
     * @param url the full URL to the comic issue's "read all pages" view
     * @return a set of image URLs found after scrolling
     */
    private Set<String> emulateScrollAndCollectImages(final String url) {
        WebDriverManager.chromedriver().setup();

        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        final WebDriver driver = new ChromeDriver(options);

        try {
            driver.get(url);

            final JavascriptExecutor js = (JavascriptExecutor) driver;
            final WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);
            final Set<String> allImageSrcs = new LinkedHashSet<>();
            int noProgressTries = 0;

            logger.info("Performing lazy load and scrolling. Please wait...");
            while (noProgressTries < MAX_NO_PROGRESS_TRIES) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                boolean newImagesLoaded = waitForNewImages(wait, allImageSrcs);
                if (newImagesLoaded) {
                    noProgressTries = 0;
                } else {
                    logger.info("Next try...");
                    noProgressTries++;
                }
            }
            logger.info("Scrolling complete. Total images: {}", allImageSrcs.size());
            return allImageSrcs;

        } finally {
            driver.quit();
        }
    }

    /**
     * Waits for newly loaded images to appear in the DOM and updates the set of collected image sources.
     *
     * @param wait           the WebDriverWait instance used to poll for changes
     * @param allImageSrcs   the current known set of image URLs
     * @return true if new images were found and added, false if no progress
     */
    private boolean waitForNewImages(final WebDriverWait wait,
                                     final Set<String> allImageSrcs) {
        try {
            return wait.until(d -> {
                final List<WebElement> imgs = d.findElements(By.tagName("img"));
                final Set<String> current = new LinkedHashSet<>();
                for (WebElement img : imgs) {
                    final String src = img.getAttribute("src");
                    if (src != null && src.contains("blogspot")) {
                        current.add(src);
                    }
                }

                if (current.size() > allImageSrcs.size()) {
                    int newCount = current.size() - allImageSrcs.size();
                    logger.debug("New images loaded: {} [{} total]", newCount, current.size());
                    allImageSrcs.addAll(current);
                    return true;
                }

                return false;
            });
        } catch (TimeoutException e) {
            return false;
        }
    }
}
