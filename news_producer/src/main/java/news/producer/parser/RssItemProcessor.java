package news.producer.parser;

import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.module.DCModule;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.rome.feed.synd.SyndCategory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RssItemProcessor {
  public static Map<String, Object> processRssItem(SyndEntry entry) throws Exception {
    Map<String, Object> article = new HashMap<>();
    article.put("title", entry.getTitle());
    article.put("link", entry.getLink());
    article.put("guid", convertToId(entry.getUri()));
    article.put("description", entry.getDescription() != null ? entry.getDescription().getValue() : null);
    Module dcModule = entry.getModule(DCModule.URI);
    if (dcModule instanceof DCModule) {
      DCModule creatorModule = (DCModule) dcModule;
      article.put("creator", creatorModule.getCreator());
    }
    article.put("pubDate", entry.getPublishedDate());
    List<SyndCategory> categories = entry.getCategories();
    if (categories != null) {
      article.put("categories", categories.stream()
          .map(SyndCategory::getName)
          .toArray(String[]::new));
    }

    Module mediaModule = entry.getModule(MediaEntryModule.URI);
    if (mediaModule instanceof MediaEntryModule) {
      MediaEntryModule mediaEntry = (MediaEntryModule) mediaModule;
      MediaContent[] mediaContents = mediaEntry.getMediaContents();
      if (mediaContents != null && mediaContents.length > 0) {
        MediaContent content = mediaContents[0];
        Map<String, Object> media = new HashMap<>();
        media.put("url", content.getReference() != null ? content.getReference().toString() : null);
        media.put("width", content.getWidth());
        media.put("height", content.getHeight());
        article.put("media", media);
      }
    }

    return article;
  }

  /**
   * Converts a uri to a unique ID using SHA-256 hashing.
   *
   * @param uri the uri to convert
   * @return the unique ID as a hexadecimal string
   */

  public static String convertToId(String uri) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(uri.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error generating hash", e);
    }
  }

}
