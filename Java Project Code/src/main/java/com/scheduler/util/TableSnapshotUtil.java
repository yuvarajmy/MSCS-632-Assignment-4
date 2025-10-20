package com.scheduler.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TableSnapshotUtil {

    public static void saveNodeAsPNG(Node node, File file) throws IOException {
        SnapshotParameters params = new SnapshotParameters();

        WritableImage writableImage = node.snapshot(params, null);

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

        ImageIO.write(bufferedImage, "png", file);
    }
}
