package org.musicStore.util;

import org.musicStore.model.Stock;

/**
 * Maps a Stock product to a representative emoji icon based on
 * its name, category, and brand. Used to show product images in tables.
 */
public class ProductImageUtil {

    public static String getEmoji(Stock s) {
        if (s == null) return "🎵";
        String name     = s.getName()     != null ? s.getName().toLowerCase()     : "";
        String category = s.getCategory() != null ? s.getCategory().toLowerCase() : "";
        String brand    = s.getBrand()    != null ? s.getBrand().toLowerCase()    : "";

        // ── Specific product names ────────────────────────────────────────────
        if (name.contains("classical guitar") || name.contains("acoustic guitar")) return "🎸";
        if (name.contains("electric guitar") || name.contains("guitar"))           return "🎸";
        if (name.contains("bass guitar") || name.contains("bass"))                 return "🎸";
        if (name.contains("violin"))                                                return "🎻";
        if (name.contains("cello"))                                                 return "🎻";
        if (name.contains("viola"))                                                 return "🎻";
        if (name.contains("piano") || name.contains("keyboard") || name.contains("keys")) return "🎹";
        if (name.contains("drum") || name.contains("percussion") || name.contains("snare")) return "🥁";
        if (name.contains("saxophone") || name.contains("sax"))                    return "🎷";
        if (name.contains("trumpet") || name.contains("cornet"))                   return "🎺";
        if (name.contains("trombone") || name.contains("tuba"))                    return "🎺";
        if (name.contains("flute") || name.contains("piccolo"))                    return "🪈";
        if (name.contains("clarinet") || name.contains("oboe") || name.contains("bassoon")) return "🎵";
        if (name.contains("harmonica"))                                             return "🎵";
        if (name.contains("harp"))                                                  return "🎵";
        if (name.contains("ukulele") || name.contains("mandolin") || name.contains("banjo")) return "🪕";
        if (name.contains("microphone") || name.contains("mic"))                   return "🎙️";
        if (name.contains("amplifier") || name.contains("amp") || name.contains("speaker")) return "🔊";
        if (name.contains("headphone") || name.contains("earphone"))               return "🎧";
        if (name.contains("string") || name.contains("string set") || name.contains("strings")) return "🎵";
        if (name.contains("reed") || name.contains("mouthpiece"))                  return "🎷";
        if (name.contains("bow"))                                                   return "🎻";
        if (name.contains("stick") || name.contains("mallet"))                     return "🥁";
        if (name.contains("pedal") || name.contains("sustain"))                    return "🎹";
        if (name.contains("stand") || name.contains("music stand"))                return "🎼";
        if (name.contains("tuner") || name.contains("metronome"))                  return "🎵";
        if (name.contains("capo"))                                                  return "🎸";
        if (name.contains("pick") || name.contains("plectrum"))                    return "🎸";
        if (name.contains("cable") || name.contains("cord"))                       return "🔌";
        if (name.contains("bag") || name.contains("case") || name.contains("gig bag")) return "🎒";
        if (name.contains("cleaning") || name.contains("polish"))                  return "✨";
        if (name.contains("sheet") || name.contains("book") || name.contains("score")) return "📖";
        if (name.contains("synthesizer") || name.contains("synth"))                return "🎹";
        if (name.contains("recorder"))                                              return "🪈";

        // ── Category fallback ─────────────────────────────────────────────────
        if (category.contains("string"))      return "🎸";
        if (category.contains("keyboard"))    return "🎹";
        if (category.contains("percussion") || category.contains("drum")) return "🥁";
        if (category.contains("wind") || category.contains("brass") || category.contains("woodwind")) return "🎷";
        if (category.contains("accessory"))   return "🎵";
        if (category.contains("equipment"))   return "🎼";
        if (category.contains("electronic"))  return "🔊";

        // ── Brand hints ───────────────────────────────────────────────────────
        if (brand.contains("fender") || brand.contains("gibson") || brand.contains("ibanez") ||
            brand.contains("schecter") || brand.contains("prs"))               return "🎸";
        if (brand.contains("steinway") || brand.contains("yamaha") || brand.contains("roland") ||
            brand.contains("kawai") || brand.contains("korg") || brand.contains("casio")) return "🎹";
        if (brand.contains("pearl") || brand.contains("tama") || brand.contains("ddrum") ||
            brand.contains("vic firth") || brand.contains("zildjian"))         return "🥁";
        if (brand.contains("selmer") || brand.contains("vandoren") || brand.contains("conn")) return "🎷";
        if (brand.contains("stradivari") || brand.contains("codabow") || brand.contains("stentor")) return "🎻";
        if (brand.contains("shure") || brand.contains("sennheiser") || brand.contains("audio-technica")) return "🎙️";
        if (brand.contains("marshall") || brand.contains("mesa") || brand.contains("orange")) return "🔊";

        return "🎵"; // generic music fallback
    }

    /**
     * Returns a styled background color for the image cell based on category.
     */
    public static String getCellColor(Stock s) {
        if (s == null) return "#1a1a28";
        String category = s.getCategory() != null ? s.getCategory().toLowerCase() : "";
        String name     = s.getName()     != null ? s.getName().toLowerCase()     : "";

        if (category.contains("string")   || name.contains("guitar") || name.contains("violin")) return "#1a1a0d";
        if (category.contains("keyboard") || name.contains("piano")  || name.contains("synth"))  return "#0d1a2e";
        if (category.contains("percussion")|| name.contains("drum"))                              return "#1a0d0d";
        if (category.contains("wind")     || name.contains("sax")   || name.contains("trumpet")) return "#0d1a1a";
        if (category.contains("equipment"))                                                        return "#1a1228";
        return "#12121a";
    }
}
