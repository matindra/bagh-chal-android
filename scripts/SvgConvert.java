import com.android.ide.common.vectordrawable.Svg2Vector;
import com.android.ide.common.vectordrawable.VdPreview;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/**
 * Converts design/*.svg to res/drawable VectorDrawables using Android Studio's own
 * Svg2Vector, then renders the legacy (pre-API 26) launcher PNGs from the converted vectors.
 *
 * Usage: java -cp <sdk-common + deps> SvgConvert.java <designDir> <resDir> <pngOutDir>
 */
public class SvgConvert {

    /** design/ file name -> drawable resource name. */
    private static final Map<String, String> NAMES = new TreeMap<>(Map.ofEntries(
            Map.entry("board.svg", "board_wood"),
            Map.entry("board_stone.svg", "board_stone"),
            Map.entry("board_cloth.svg", "board_cloth"),
            Map.entry("tiger_piece.svg", "ic_tiger"),
            Map.entry("goat_piece.svg", "ic_goat"),
            Map.entry("ic_launcher_background.svg", "ic_launcher_background"),
            Map.entry("ic_launcher_foreground.svg", "ic_launcher_foreground"),
            Map.entry("result_bg_tiger.svg", "result_bg_tiger"),
            Map.entry("result_bg_goat.svg", "result_bg_goat"),
            Map.entry("lock_badge.svg", "lock_badge"),
            Map.entry("prayer_flags.svg", "deco_prayer_flags"),
            Map.entry("mountains.svg", "deco_mountains"),
            Map.entry("undo.svg", "ic_undo"),
            Map.entry("hint.svg", "ic_hint"),
            Map.entry("menu.svg", "ic_menu"),
            Map.entry("help.svg", "ic_help"),
            Map.entry("stats.svg", "ic_stats"),
            Map.entry("settings.svg", "ic_settings"),
            Map.entry("home.svg", "ic_home"),
            Map.entry("mode_vs_ai.svg", "ic_mode_vs_ai"),
            Map.entry("mode_vs_player.svg", "ic_mode_vs_player"),
            Map.entry("mode_online.svg", "ic_mode_online"),
            Map.entry("back.svg", "ic_back"),
            Map.entry("share.svg", "ic_share"),
            Map.entry("copy.svg", "ic_copy"),
            Map.entry("add.svg", "ic_add"),
            Map.entry("join.svg", "ic_join"),
            Map.entry("search.svg", "ic_search"),
            Map.entry("person.svg", "ic_person"),
            Map.entry("privacy.svg", "ic_privacy")
    ));

    /** mipmap density -> legacy launcher icon size in px (48dp). */
    private static final Map<String, Integer> DENSITIES = new TreeMap<>(Map.of(
            "mdpi", 48, "hdpi", 72, "xhdpi", 96, "xxhdpi", 144, "xxxhdpi", 192
    ));

    public static void main(String[] args) throws Exception {
        Path designDir = Path.of(args[0]);
        Path drawableDir = Path.of(args[1], "drawable");
        Path pngOutDir = Path.of(args[2]);
        Files.createDirectories(drawableDir);

        File[] svgs = designDir.toFile().listFiles((d, n) -> n.endsWith(".svg"));
        if (svgs == null) throw new IllegalStateException("No SVGs in " + designDir);
        for (File svg : svgs) {
            String name = NAMES.get(svg.getName());
            if (name == null) throw new IllegalStateException("No resource name mapped for " + svg.getName());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String error = Svg2Vector.parseSvgToXml(svg.toPath(), out);
            if (error != null && !error.isBlank()) {
                throw new IllegalStateException(svg.getName() + ": " + error);
            }
            String xml = "<!-- Generated from design/" + svg.getName()
                    + " by scripts/convert-svgs.sh. Do not edit by hand. -->\n"
                    + out.toString(StandardCharsets.UTF_8);
            Files.writeString(drawableDir.resolve(name + ".xml"), xml);
            System.out.println(svg.getName() + " -> drawable/" + name + ".xml");
        }

        String background = Files.readString(drawableDir.resolve("ic_launcher_background.xml"));
        String foreground = Files.readString(drawableDir.resolve("ic_launcher_foreground.xml"));
        for (Map.Entry<String, Integer> e : DENSITIES.entrySet()) {
            int size = e.getValue();
            Path dir = pngOutDir.resolve("mipmap-" + e.getKey());
            Files.createDirectories(dir);
            BufferedImage full = renderAdaptive(background, foreground, size);
            ImageIO.write(mask(full, size, false), "png", dir.resolve("ic_launcher.png").toFile());
            ImageIO.write(mask(full, size, true), "png", dir.resolve("ic_launcher_round.png").toFile());
        }
        System.out.println("Legacy launcher PNGs -> " + pngOutDir);
    }

    /** Renders the 108dp adaptive layers so that the visible 72dp centre maps onto [size] px. */
    private static BufferedImage renderAdaptive(String background, String foreground, int size) {
        int full = Math.round(size * 108f / 72f);
        BufferedImage canvas = new BufferedImage(full, full, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        for (String layer : new String[]{background, foreground}) {
            StringBuilder err = new StringBuilder();
            BufferedImage img = VdPreview.getPreviewFromVectorXml(
                    VdPreview.TargetSize.createFromMaxDimension(full), layer, err);
            if (img == null || err.length() > 0) throw new IllegalStateException(err.toString());
            g.drawImage(img, 0, 0, null);
        }
        g.dispose();
        int offset = (full - size) / 2;
        return canvas.getSubimage(offset, offset, size, size);
    }

    private static BufferedImage mask(BufferedImage src, int size, boolean round) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float inset = size * 2f / 48f;
        float s = size - 2 * inset;
        g.fill(round
                ? new Ellipse2D.Float(inset, inset, s, s)
                : new RoundRectangle2D.Float(inset, inset, s, s, s * 0.25f, s * 0.25f));
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
