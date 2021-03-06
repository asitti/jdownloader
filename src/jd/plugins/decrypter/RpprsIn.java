//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rappers.in" }, urls = { "https?://(?:www\\.)?rappers\\.in/(.*?\\-beat\\-\\d+\\.html|[A-Za-z0-9_\\-]+\\-tracks\\.html|.*?artist\\.php\\?id=\\d+\\&bdlid=\\d+|.*?beatdownload\\.php\\?bid=\\d+|(?!news\\-|videos|topvideos|randomvideos|swfobject|register|login|gsearch)[A-Za-z0-9_\\-]{3,})" })
public class RpprsIn extends PluginForDecrypt {
    public RpprsIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?rappers\\.in/(news\\-|videos|topvideos|randomvideos|swfobject|register|login|gsearch|fpss).*?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        } else if (parameter.matches(".+(\\-beat\\-\\d+\\.html|artist\\.php).*?")) {
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String finallink = null;
            final boolean use_new_way = true;
            final String id = new Regex(parameter, "beat\\-(\\d+)\\.html").getMatch(0);
            if (use_new_way) {
                // br.getPage("https://www.rappers.in/beatdownload.php?bid=" + id);
                // finallink = br.getRedirectLocation();
                final String[] tracks_b64 = br.getRegex("file\\s*:\\s*\".*?(aHR0[^<>\"]+)\"").getColumn(0);
                for (final String b64 : tracks_b64) {
                    finallink = Encoding.Base64Decode(b64);
                    decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                }
                return decryptedLinks;
            } else {
                br.getPage("https://www.rappers.in/playbeat-" + id + "-1808.xml?" + new Random().nextInt(10) + "s=undefined");
                finallink = br.getRegex("<filename>(http.*?)</filename>").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // Errorhandling for invalid links
            if (finallink.contains("beats//")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        } else {
            if (parameter.matches(".+/track\\-\\d+")) {
                br.getPage("https://www.rappers.in/playtrack-" + new Regex(parameter, "(\\d+)$").getMatch(0) + "-1808.xml?" + new Random().nextInt(100) + "&s=undefined");
            } else {
                String access;
                String accessid;
                if (parameter.matches(".+/[A-Za-z0-9_\\-]+\\-tracks\\.html")) {
                    access = "tracks";
                }
                br.getPage(parameter);
                final Regex accessinfo = br.getRegex("makeRIP\\(\"([A-Za-z_]+)\\-(\\d+)\"\\)");
                access = accessinfo.getMatch(0);
                if (!br.containsHTML("\"rip/vote1\\.png\"")) {
                    logger.info("Link invalid/offline: " + parameter);
                    return decryptedLinks;
                }
                accessid = accessinfo.getMatch(1);
                if (access == null || accessid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://www.rappers.in/" + access + "-" + accessid + "-1808.xml?" + new Random().nextInt(100) + "&s=undefined");
            }
            if (br.containsHTML("<playlist>[\t\n\r ]+</playlist>")) {
                logger.info("Link offline (empty tracklist): " + parameter);
                return decryptedLinks;
            }
            final String[][] allSongs = br.getRegex("<filename>(http://[^<>\"]*?)</filename>[\t\n\r ]+<title>([^<>\"]*?)</title>").getMatches();
            if (allSongs == null || allSongs.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String[] songInfo : allSongs) {
                final DownloadLink dl = createDownloadlink("directhttp://" + songInfo[0]);
                dl.setFinalFileName(Encoding.htmlDecode(songInfo[1].trim()) + ".mp3");
                decryptedLinks.add(dl);
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName("All songs of: " + new Regex(parameter, "rappers\\.in/([A-Za-z0-9_\\-]+)(\\-tracks\\.html)?").getMatch(0));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}