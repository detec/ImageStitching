
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @see https://stackoverflow.com/questions/2658663
 */
public class StitchUtil extends JPanel {

    private static final long serialVersionUID = 9073290927998138736L;

    private static final int DEFAULT_SIZE_VALUE = 1;
    private static final String FONT_NAME = "Open Sans";
    private static final int COLOR_VALUE = 0xBA131A;

    private BufferedImage image;

    public StitchUtil() {
        try {

            File leadImageFile = new File("");

            image = ImageIO.read(leadImageFile);
            image = stitch(image);
            ImageIO.write(image, "jpeg", new File(""));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    /**
     * Main method that contains implementation of lead image/generated logo
     * stitch algorithm.
     *
     * @param originalImage
     *            - already transformed from Dari {@link BufferedImage} instance
     * @return - stitched {@link BufferedImage}, ready for Dari manipulation
     */
    public static BufferedImage stitch(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        BufferedImage fontCalculationImage = new BufferedImage(originalWidth, originalHeight,
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D fontCalculationGraphics = fontCalculationImage.createGraphics();

        String caption = "CAPTION";
        Optional<SimpleEntry<Integer, Integer>> sizeLengthPairOptional = Optional
                .ofNullable(getRequiredStringLengthFontSizePair(fontCalculationGraphics, caption, originalWidth));

        int calculatedFontSize = sizeLengthPairOptional.map(SimpleEntry::getValue).orElse(DEFAULT_SIZE_VALUE);

        Font font = new Font(FONT_NAME, Font.PLAIN, calculatedFontSize);

        fontCalculationGraphics.setFont(font);
        int calculatedCaptionHeight = Optional.ofNullable(fontCalculationGraphics.getFontMetrics())
                .map(FontMetrics::getAscent).orElse(DEFAULT_SIZE_VALUE);
        fontCalculationGraphics.dispose();

        /**
         * Background height should be 20px from the top and 20px from the
         * bottom of the logo and logo height should be 40px. Thus text caption
         * takes 50% of total height of logo banner.
         */
        int logoBannerHeight = calculatedCaptionHeight * 2;
        int newLeadImageHeight = logoBannerHeight + originalHeight;

        // Using 5, this is for regular photographic JPEGs, TYPE_3BYTE_BGR
        BufferedImage stitchedLeadImage = new BufferedImage(originalWidth, newLeadImageHeight,
                BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D stitchedG2d = stitchedLeadImage.createGraphics();

        stitchedG2d.drawImage(originalImage, 0, logoBannerHeight, originalWidth, originalHeight, null);

        Color backgroundColor = new Color(COLOR_VALUE);
        stitchedG2d.setColor(backgroundColor);
        stitchedG2d.fillRect(0, 0, originalWidth, logoBannerHeight);

        stitchedG2d.setFont(font);
        stitchedG2d.setColor(Color.WHITE);

        // caption position starts with the second third part of image width.
        int x = sizeLengthPairOptional.map(SimpleEntry::getKey).orElse(DEFAULT_SIZE_VALUE);

        /**
         * We add some background above, 50% of calculated caption height. Some
         * tuning is made because of Font Metrics possible inconsistencies. This
         * is why 1.25 is used, not 1.5.
         *
         * String 'y' position is baseline so it actually is string height
         * itself + background space above.
         */
        int y = (int) Math.round(calculatedCaptionHeight * 1.25);

        // The baseline of the first character is at position (x, y)
        stitchedG2d.drawString(caption, x, y);

        // release resources
        stitchedG2d.dispose();
        return stitchedLeadImage;
    }

    private static SimpleEntry<Integer, Integer> getRequiredStringLengthFontSizePair(Graphics2D stitchedG2d,
            String caption, int originalWidth) {
        if (originalWidth <= 0) {
            return null;
        }

        // generating map with generated sizes and calculated string length
        Map<Integer, Integer> lengthSizeMap = IntStream.range(1, 500).boxed()
                .collect(Collectors.toMap(i -> getCaptionWidthByFontSize(stitchedG2d, caption, i), Function.identity(),
                        (i1, i2) -> i1, LinkedHashMap::new));

        int[] lengthArray = lengthSizeMap.keySet().stream().mapToInt(i -> i).toArray();

        // finding closest position in a sorted array of lengths, caption should
        // take circa 1/3 of original image width
        int nearestPosition = Math.abs(Arrays.binarySearch(lengthArray, Math.round(originalWidth / 3)));
        int stringLength = lengthArray[nearestPosition];
        return new SimpleEntry<>(stringLength, lengthSizeMap.get(stringLength));
    }

    private static int getCaptionWidthByFontSize(Graphics2D stitchedG2d, String caption, int fontSize) {
        Font font = new Font(FONT_NAME, Font.PLAIN, fontSize);
        stitchedG2d.setFont(font);
        return stitchedG2d.getFontMetrics().stringWidth(caption);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

    private static void create() {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new StitchUtil());
        f.pack();
        f.setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                create();
            }
        });
    }
}
