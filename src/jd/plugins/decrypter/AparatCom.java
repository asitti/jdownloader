//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "aparat.com" }, urls = { "https?://(?:www\\.)?aparat.com/v/([A-Za-z0-9]+)" })
public class AparatCom extends PluginForDecrypt {
    public AparatCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String itemID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        //
        String[][] videoMatches = br.getRegex("contentUrl\":\"(.+?)\"").getMatches();
        for (String[] videoMatch : videoMatches) {
            String videoURL = Encoding.htmlDecode(videoMatch[0]).replaceAll("\\\\/", "/");
            decryptedLinks.add(createDownloadlink(videoURL));
        }
        //
        String[][] jsonMatches = br.getRegex("<script[\\s\\t\\r]+type=\"application/ld\\+json\">[\\s\\t\\r]*(.*?)[\\s\\t\\r]*</script>").getMatches();
        for (String[] jsonMatch : jsonMatches) {
            final LinkedHashMap<String, Object> jsonEntries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(jsonMatch[0]);
            String videoName = jsonEntries.get("name").toString();
            final String videoURL = jsonEntries.get("contentUrl").toString();
            if (videoName != null && videoURL != null) {
                if (Encoding.isHtmlEntityCoded(videoName)) {
                    videoName = Encoding.htmlDecode(videoName);
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + videoURL);
                dl.setForcedFileName(videoName + ".mp4");
                decryptedLinks.add(dl);
            }
        }
        //
        if (decryptedLinks.isEmpty()) {
            /* 2020-11-09: Fallback to HLS */
            decryptedLinks.add(this.createDownloadlink("https://www.aparat.com/video/hls/manifest/visittype/site/videohash/" + itemID + "/f/" + itemID + ".m3u8"));
        }
        if (!title.isEmpty()) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(title));
            filePackage.setComment(title);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }
}