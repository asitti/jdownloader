package jd.controlling.linkcrawler;

import java.util.List;

import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.utils.StringUtils;

public abstract class LinkCrawlerDeepInspector {
    /**
     * https://www.iana.org/assignments/media-types/media-types.xhtml
     *
     * @param urlConnection
     * @return
     */
    public boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (urlConnection.getResponseCode() == 200 || urlConnection.getResponseCode() == 206) {
            final boolean hasContentType = StringUtils.isNotEmpty(urlConnection.getHeaderField(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE));
            final long sizeDownloadableContent = 2 * 1024 * 1024l;
            if (urlConnection.isContentDisposition()) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "application/force-download")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "application/octet-stream")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "audio/")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "video/")) {
                return true;
            } else if (hasContentType && StringUtils.contains(urlConnection.getContentType(), "image/")) {
                return true;
            } else if (urlConnection.getLongContentLength() > sizeDownloadableContent && (!hasContentType || !isTextContent(urlConnection))) {
                return true;
            } else if (urlConnection.getLongContentLength() > sizeDownloadableContent && StringUtils.contains(urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_ACCEPT_RANGES), "bytes")) {
                return true;
            }
        }
        return false;
    }

    public boolean isTextContent(final URLConnectionAdapter urlConnection) {
        final String contentType = urlConnection.getContentType();
        return StringUtils.containsIgnoreCase(contentType, "text/") || StringUtils.containsIgnoreCase(contentType, "application/json") || StringUtils.containsIgnoreCase(contentType, "application/xml");
    }

    public abstract List<CrawledLink> deepInspect(LinkCrawler lc, final LinkCrawlerGeneration generation, Browser br, URLConnectionAdapter urlConnection, final CrawledLink link) throws Exception;
}
